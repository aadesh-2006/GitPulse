package com.gitpulse.domain.risk;

import com.gitpulse.common.exception.ResourceNotFoundException;
import com.gitpulse.domain.contributorfile.RepositoryContributorFileJpaRepository;
import com.gitpulse.domain.contributorfile.dto.FileOwnershipShareRow;
import com.gitpulse.domain.file.RepositoryFile;
import com.gitpulse.domain.file.RepositoryFileJpaRepository;
import com.gitpulse.domain.file.dto.RepositoryFileNormalizationMaximaRow;
import com.gitpulse.domain.repository.RepositoryJpaRepository;
import com.gitpulse.domain.risk.dto.RepositoryFileRiskMaterializationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Materialization service for repository file risk & stability intelligence.
 * <p>
 * Evaluates repository-wide normalization maxima, retrieves top-contributor ownership shares
 * in page-bounded batches, computes deterministic multi-dimensional scores via {@link FileRiskScoringService},
 * and performs in-place entity updates with exact bitwise change detection.
 */
@Service
public class RepositoryFileRiskMaterializationService {

    private static final Logger log = LoggerFactory.getLogger(RepositoryFileRiskMaterializationService.class);
    private static final int BATCH_SIZE = 500;

    private final RepositoryJpaRepository repositoryJpaRepository;
    private final RepositoryFileJpaRepository repositoryFileJpaRepository;
    private final RepositoryContributorFileJpaRepository repositoryContributorFileJpaRepository;
    private final FileRiskScoringService fileRiskScoringService;

    public RepositoryFileRiskMaterializationService(RepositoryJpaRepository repositoryJpaRepository,
                                                   RepositoryFileJpaRepository repositoryFileJpaRepository,
                                                   RepositoryContributorFileJpaRepository repositoryContributorFileJpaRepository,
                                                   FileRiskScoringService fileRiskScoringService) {
        this.repositoryJpaRepository = Objects.requireNonNull(repositoryJpaRepository, "repositoryJpaRepository must not be null");
        this.repositoryFileJpaRepository = Objects.requireNonNull(repositoryFileJpaRepository, "repositoryFileJpaRepository must not be null");
        this.repositoryContributorFileJpaRepository = Objects.requireNonNull(repositoryContributorFileJpaRepository, "repositoryContributorFileJpaRepository must not be null");
        this.fileRiskScoringService = Objects.requireNonNull(fileRiskScoringService, "fileRiskScoringService must not be null");
    }

    /**
     * Materializes deterministic file risk scores for all files belonging to the specified repository.
     *
     * @param repositoryId  the ID of the repository to process, must not be null
     * @param referenceTime explicit deterministic reference timestamp (e.g., analysis job creation time), must not be null
     * @return summary result of the materialization run
     */
    @Transactional
    public RepositoryFileRiskMaterializationResult materializeFileRisks(Long repositoryId, Instant referenceTime) {
        Objects.requireNonNull(repositoryId, "repositoryId must not be null");
        Objects.requireNonNull(referenceTime, "referenceTime must not be null");

        log.info("Starting file risk score materialization for repositoryId={} with referenceTime={}", repositoryId, referenceTime);

        if (!repositoryJpaRepository.existsById(repositoryId)) {
            throw new ResourceNotFoundException("Repository not found with id: " + repositoryId);
        }

        // 1. Calculate repository-level normalization maxima via database aggregation
        RepositoryFileNormalizationMaximaRow maximaRow = repositoryFileJpaRepository.findNormalizationMaximaByRepositoryId(repositoryId);
        long maxRevisions = (maximaRow != null) ? maximaRow.getMaxRevisions() : 0L;
        long maxChurn = (maximaRow != null) ? maximaRow.getMaxChurn() : 0L;

        FileRiskNormalizationContext context = new FileRiskNormalizationContext(maxRevisions, maxChurn);

        int totalProcessed = 0;
        int updatedCount = 0;
        int unchangedCount = 0;
        int pageNumber = 0;

        Page<RepositoryFile> filePage;
        do {
            Pageable pageable = PageRequest.of(pageNumber, BATCH_SIZE, Sort.by(Sort.Direction.ASC, "filePath"));
            filePage = repositoryFileJpaRepository.findByRepositoryId(repositoryId, pageable);

            if (filePage.isEmpty()) {
                break;
            }

            List<RepositoryFile> files = filePage.getContent();
            List<String> filePaths = files.stream().map(RepositoryFile::getFilePath).toList();

            // 2. Fetch ownership shares in a single page-scoped query
            Map<String, Double> ownershipMap = repositoryContributorFileJpaRepository
                    .findOwnershipSharesByRepositoryIdAndFilePaths(repositoryId, filePaths).stream()
                    .filter(row -> row.getFilePath() != null && row.getTopContributorRevisionShare() != null)
                    .collect(Collectors.toMap(
                            FileOwnershipShareRow::getFilePath,
                            FileOwnershipShareRow::getTopContributorRevisionShare,
                            (existing, replacement) -> existing
                    ));

            List<RepositoryFile> toSave = new ArrayList<>();

            // 3. Compute scores and apply change detection
            for (RepositoryFile file : files) {
                totalProcessed++;
                double topContributorShare = ownershipMap.getOrDefault(file.getFilePath(), 0.0);

                FileRiskInput input = new FileRiskInput(
                        file.getTotalRevisions(),
                        file.getTotalChurn(),
                        file.getLastModifiedAt(),
                        topContributorShare
                );

                FileRiskScore score = fileRiskScoringService.scoreFile(input, context, referenceTime);

                if (isRiskScoreChanged(file, score)) {
                    file.updateRiskScores(
                            score.baselineScore(),
                            score.revisionFrequencyScore(),
                            score.churnScore(),
                            score.recencyScore(),
                            score.ownershipConcentrationScore(),
                            score.compositeScore()
                    );
                    toSave.add(file);
                    updatedCount++;
                } else {
                    unchangedCount++;
                }
            }

            // 4. Batch persist changed entities
            if (!toSave.isEmpty()) {
                repositoryFileJpaRepository.saveAll(toSave);
            }

            pageNumber++;
        } while (filePage.hasNext());

        repositoryFileJpaRepository.flush();

        RepositoryFileRiskMaterializationResult result = new RepositoryFileRiskMaterializationResult(
                repositoryId,
                totalProcessed,
                updatedCount,
                unchangedCount
        );

        log.info("Completed file risk score materialization for repositoryId={}: totalProcessed={}, updated={}, unchanged={}",
                repositoryId, totalProcessed, updatedCount, unchangedCount);

        return result;
    }

    private boolean isRiskScoreChanged(RepositoryFile file, FileRiskScore score) {
        return Double.doubleToLongBits(file.getBaselineScore()) != Double.doubleToLongBits(score.baselineScore())
                || Double.doubleToLongBits(file.getRevisionFrequencyScore()) != Double.doubleToLongBits(score.revisionFrequencyScore())
                || Double.doubleToLongBits(file.getChurnScore()) != Double.doubleToLongBits(score.churnScore())
                || Double.doubleToLongBits(file.getRecencyScore()) != Double.doubleToLongBits(score.recencyScore())
                || Double.doubleToLongBits(file.getOwnershipConcentrationScore()) != Double.doubleToLongBits(score.ownershipConcentrationScore())
                || Double.doubleToLongBits(file.getCompositeScore()) != Double.doubleToLongBits(score.compositeScore());
    }
}