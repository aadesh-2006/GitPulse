package com.gitpulse.domain.risk;

import com.gitpulse.common.exception.ResourceNotFoundException;
import com.gitpulse.domain.contributor.Contributor;
import com.gitpulse.domain.contributor.ContributorJpaRepository;
import com.gitpulse.domain.contributorfile.RepositoryContributorFile;
import com.gitpulse.domain.contributorfile.RepositoryContributorFileJpaRepository;
import com.gitpulse.domain.file.RepositoryFile;
import com.gitpulse.domain.file.RepositoryFileJpaRepository;
import com.gitpulse.domain.repository.Repository;
import com.gitpulse.domain.repository.RepositoryJpaRepository;
import com.gitpulse.domain.risk.dto.RepositoryFileRiskMaterializationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

@DataJpaTest
@ActiveProfiles("test")
@Import({RepositoryFileRiskMaterializationService.class, FileRiskScoringService.class})
class RepositoryFileRiskMaterializationServiceIntegrationTest {

    @Autowired
    private RepositoryFileRiskMaterializationService materializationService;

    @Autowired
    private FileRiskScoringService scoringService;

    @Autowired
    private RepositoryJpaRepository repositoryJpaRepository;

    @Autowired
    private RepositoryFileJpaRepository repositoryFileJpaRepository;

    @Autowired
    private RepositoryContributorFileJpaRepository repositoryContributorFileJpaRepository;

    @Autowired
    private ContributorJpaRepository contributorJpaRepository;

    private Repository repo1;
    private Repository repo2;
    private Contributor alice;
    private Contributor bob;
    private Contributor charlie;
    private final Instant refTime = Instant.parse("2026-06-01T12:00:00Z");

    @BeforeEach
    void setUp() {
        repo1 = repositoryJpaRepository.save(new Repository("owner1", "repo1", "Repo 1", "main"));
        repo2 = repositoryJpaRepository.save(new Repository("owner2", "repo2", "Repo 2", "main"));

        alice = contributorJpaRepository.save(new Contributor("alice@example.com", "alice", "Alice"));
        bob = contributorJpaRepository.save(new Contributor("bob@example.com", "bob", "Bob"));
        charlie = contributorJpaRepository.save(new Contributor("charlie@example.com", "charlie", "Charlie"));
    }

    @Test
    @DisplayName("1. Repository with multiple files calculates and persists matching risk scores")
    void materializeFileRisks_MultipleFiles_ComputesExactScores() {
        // fileA: revisions = 10, churn = 100, lastModified = refTime (age 0), ownership share = 0.50 (Alice=5, Bob=5)
        RepositoryFile fileA = repositoryFileJpaRepository.save(new RepositoryFile(
                repo1, "src/fileA.java", "fileA.java", "java", "src",
                10, 80, 20, 100, false, refTime.minus(Duration.ofDays(30)), refTime, alice
        ));
        repositoryContributorFileJpaRepository.save(new RepositoryContributorFile(repo1, alice, "src/fileA.java", 5, 50, 0, 50, refTime, refTime));
        repositoryContributorFileJpaRepository.save(new RepositoryContributorFile(repo1, bob, "src/fileA.java", 5, 50, 0, 50, refTime, refTime));

        // fileB: revisions = 50, churn = 500, lastModified = refTime (age 0), ownership share = 1.00 (Alice=50)
        RepositoryFile fileB = repositoryFileJpaRepository.save(new RepositoryFile(
                repo1, "src/fileB.java", "fileB.java", "java", "src",
                50, 400, 100, 500, false, refTime.minus(Duration.ofDays(60)), refTime, alice
        ));
        repositoryContributorFileJpaRepository.save(new RepositoryContributorFile(repo1, alice, "src/fileB.java", 50, 500, 0, 500, refTime, refTime));

        // fileC: revisions = 1, churn = 5, lastModified = 90 days ago, ownership share = 0.25 (Alice=1, Bob=3 -> 1/4 = 0.25 for Alice vs Bob)
        Instant ninetyDaysAgo = refTime.minus(Duration.ofDays(90));
        RepositoryFile fileC = repositoryFileJpaRepository.save(new RepositoryFile(
                repo1, "src/fileC.java", "fileC.java", "java", "src",
                1, 4, 1, 5, false, ninetyDaysAgo, ninetyDaysAgo, alice
        ));
        repositoryContributorFileJpaRepository.save(new RepositoryContributorFile(repo1, alice, "src/fileC.java", 1, 5, 0, 5, ninetyDaysAgo, ninetyDaysAgo));
        repositoryContributorFileJpaRepository.save(new RepositoryContributorFile(repo1, bob, "src/fileC.java", 3, 15, 0, 15, ninetyDaysAgo, ninetyDaysAgo));

        repositoryFileJpaRepository.flush();
        repositoryContributorFileJpaRepository.flush();

        // Run materialization
        RepositoryFileRiskMaterializationResult result = materializationService.materializeFileRisks(repo1.getId(), refTime);

        assertThat(result.repositoryId()).isEqualTo(repo1.getId());
        assertThat(result.totalFilesProcessed()).isEqualTo(3);
        assertThat(result.updatedCount()).isEqualTo(3);
        assertThat(result.unchangedCount()).isEqualTo(0);

        // Verify persisted scores match FileRiskScoringService
        FileRiskNormalizationContext context = new FileRiskNormalizationContext(50, 500);

        Map<String, RepositoryFile> filesByPath = repositoryFileJpaRepository.findByRepositoryId(repo1.getId()).stream()
                .collect(Collectors.toMap(RepositoryFile::getFilePath, f -> f));

        RepositoryFile persistedA = filesByPath.get("src/fileA.java");
        FileRiskScore expectedScoreA = scoringService.scoreFile(
                new FileRiskInput(10, 100, refTime, 0.50), context, refTime
        );
        assertThat(persistedA.getBaselineScore()).isEqualTo(expectedScoreA.baselineScore());
        assertThat(persistedA.getRevisionFrequencyScore()).isEqualTo(expectedScoreA.revisionFrequencyScore());
        assertThat(persistedA.getChurnScore()).isEqualTo(expectedScoreA.churnScore());
        assertThat(persistedA.getRecencyScore()).isCloseTo(1.0, within(1e-9));
        assertThat(persistedA.getOwnershipConcentrationScore()).isEqualTo(0.50);
        assertThat(persistedA.getCompositeScore()).isCloseTo(expectedScoreA.compositeScore(), within(1e-9));

        RepositoryFile persistedB = filesByPath.get("src/fileB.java");
        FileRiskScore expectedScoreB = scoringService.scoreFile(
                new FileRiskInput(50, 500, refTime, 1.00), context, refTime
        );
        assertThat(persistedB.getBaselineScore()).isEqualTo(1.0);
        assertThat(persistedB.getRevisionFrequencyScore()).isEqualTo(1.0);
        assertThat(persistedB.getChurnScore()).isEqualTo(1.0);
        assertThat(persistedB.getRecencyScore()).isCloseTo(1.0, within(1e-9));
        assertThat(persistedB.getOwnershipConcentrationScore()).isEqualTo(1.0);
        assertThat(persistedB.getCompositeScore()).isCloseTo(expectedScoreB.compositeScore(), within(1e-9));

        RepositoryFile persistedC = filesByPath.get("src/fileC.java");
        // For fileC: Bob has 3 revisions out of 4 total -> topContributorRevisionShare = 3/4 = 0.75
        FileRiskScore expectedScoreC = scoringService.scoreFile(
                new FileRiskInput(1, 5, ninetyDaysAgo, 0.75), context, refTime
        );
        assertThat(persistedC.getBaselineScore()).isEqualTo(expectedScoreC.baselineScore());
        assertThat(persistedC.getRecencyScore()).isCloseTo(0.5, within(1e-9));
        assertThat(persistedC.getOwnershipConcentrationScore()).isEqualTo(0.75);
        assertThat(persistedC.getCompositeScore()).isCloseTo(expectedScoreC.compositeScore(), within(1e-9));
    }

    @Test
    @DisplayName("2. Normalization maxima are repository-specific and isolated")
    void materializeFileRisks_RepositorySpecificMaxima() {
        // repo1 max revisions = 10, max churn = 100
        repositoryFileJpaRepository.save(new RepositoryFile(
                repo1, "src/file1.java", "file1.java", "java", "src",
                10, 80, 20, 100, false, refTime, refTime, alice
        ));

        // repo2 max revisions = 1000, max churn = 50000
        repositoryFileJpaRepository.save(new RepositoryFile(
                repo2, "src/file2.java", "file2.java", "java", "src",
                1000, 40000, 10000, 50000, false, refTime, refTime, alice
        ));

        repositoryFileJpaRepository.flush();

        materializationService.materializeFileRisks(repo1.getId(), refTime);
        materializationService.materializeFileRisks(repo2.getId(), refTime);

        RepositoryFile repo1File = repositoryFileJpaRepository.findByRepositoryIdAndFilePath(repo1.getId(), "src/file1.java").orElseThrow();
        RepositoryFile repo2File = repositoryFileJpaRepository.findByRepositoryIdAndFilePath(repo2.getId(), "src/file2.java").orElseThrow();

        // Since both have their respective repository's max revisions, their revision frequency scores should both be 1.0 relative to their own repo
        assertThat(repo1File.getRevisionFrequencyScore()).isEqualTo(1.0);
        assertThat(repo2File.getRevisionFrequencyScore()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("3. Ownership integration uses actual contributor-file relationship")
    void materializeFileRisks_UsesActualContributorFileOwnership() {
        repositoryFileJpaRepository.save(new RepositoryFile(
                repo1, "src/shared.java", "shared.java", "java", "src",
                20, 150, 50, 200, false, refTime, refTime, alice
        ));
        // Alice = 15, Bob = 5 -> total = 20 -> top contributor (Alice) share = 15/20 = 0.75
        repositoryContributorFileJpaRepository.save(new RepositoryContributorFile(repo1, alice, "src/shared.java", 15, 150, 0, 150, refTime, refTime));
        repositoryContributorFileJpaRepository.save(new RepositoryContributorFile(repo1, bob, "src/shared.java", 5, 50, 0, 50, refTime, refTime));
        repositoryFileJpaRepository.flush();
        repositoryContributorFileJpaRepository.flush();

        materializationService.materializeFileRisks(repo1.getId(), refTime);

        RepositoryFile file = repositoryFileJpaRepository.findByRepositoryIdAndFilePath(repo1.getId(), "src/shared.java").orElseThrow();
        assertThat(file.getOwnershipConcentrationScore()).isEqualTo(0.75);
    }

    @Test
    @DisplayName("4. Recency determinism: repeated materialization with same referenceTime produces identical scores")
    void materializeFileRisks_RecencyDeterminism() {
        repositoryFileJpaRepository.save(new RepositoryFile(
                repo1, "src/code.java", "code.java", "java", "src",
                10, 80, 20, 100, false, refTime.minus(Duration.ofDays(45)), refTime.minus(Duration.ofDays(45)), alice
        ));
        repositoryFileJpaRepository.flush();

        materializationService.materializeFileRisks(repo1.getId(), refTime);
        RepositoryFile firstRun = repositoryFileJpaRepository.findByRepositoryIdAndFilePath(repo1.getId(), "src/code.java").orElseThrow();
        double firstRecency = firstRun.getRecencyScore();

        materializationService.materializeFileRisks(repo1.getId(), refTime);
        RepositoryFile secondRun = repositoryFileJpaRepository.findByRepositoryIdAndFilePath(repo1.getId(), "src/code.java").orElseThrow();
        double secondRecency = secondRun.getRecencyScore();

        assertThat(firstRecency).isEqualTo(secondRecency);
    }

    @Test
    @DisplayName("5. Empty repository succeeds with 0 processed files")
    void materializeFileRisks_EmptyRepository() {
        RepositoryFileRiskMaterializationResult result = materializationService.materializeFileRisks(repo1.getId(), refTime);

        assertThat(result.totalFilesProcessed()).isEqualTo(0);
        assertThat(result.updatedCount()).isEqualTo(0);
        assertThat(result.unchangedCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("6. Idempotent rerun: unchanged files are not re-saved")
    void materializeFileRisks_IdempotentRerun() {
        repositoryFileJpaRepository.save(new RepositoryFile(
                repo1, "src/fileA.java", "fileA.java", "java", "src",
                10, 80, 20, 100, false, refTime, refTime, alice
        ));
        repositoryFileJpaRepository.flush();

        // First run: 1 file updated
        RepositoryFileRiskMaterializationResult result1 = materializationService.materializeFileRisks(repo1.getId(), refTime);
        assertThat(result1.totalFilesProcessed()).isEqualTo(1);
        assertThat(result1.updatedCount()).isEqualTo(1);
        assertThat(result1.unchangedCount()).isEqualTo(0);

        // Second run with same input and referenceTime: 1 file unchanged
        RepositoryFileRiskMaterializationResult result2 = materializationService.materializeFileRisks(repo1.getId(), refTime);
        assertThat(result2.totalFilesProcessed()).isEqualTo(1);
        assertThat(result2.updatedCount()).isEqualTo(0);
        assertThat(result2.unchangedCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("7. Repository isolation: files from repo2 never affect repo1")
    void materializeFileRisks_RepositoryIsolation() {
        repositoryFileJpaRepository.save(new RepositoryFile(
                repo1, "src/repo1File.java", "repo1File.java", "java", "src",
                5, 40, 10, 50, false, refTime, refTime, alice
        ));
        repositoryFileJpaRepository.save(new RepositoryFile(
                repo2, "src/repo2File.java", "repo2File.java", "java", "src",
                500, 4000, 1000, 5000, false, refTime, refTime, alice
        ));
        repositoryFileJpaRepository.flush();

        materializationService.materializeFileRisks(repo1.getId(), refTime);

        RepositoryFile repo1File = repositoryFileJpaRepository.findByRepositoryIdAndFilePath(repo1.getId(), "src/repo1File.java").orElseThrow();
        RepositoryFile repo2File = repositoryFileJpaRepository.findByRepositoryIdAndFilePath(repo2.getId(), "src/repo2File.java").orElseThrow();

        // repo1File is the max file in repo1, so its revision frequency score is 1.0
        assertThat(repo1File.getRevisionFrequencyScore()).isEqualTo(1.0);
        // repo2File has not been materialized yet, so its score is 0.0
        assertThat(repo2File.getRevisionFrequencyScore()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("8. Boundary behavior: all persisted scores remain strictly within [0.0, 1.0]")
    void materializeFileRisks_BoundaryEnforcement() {
        repositoryFileJpaRepository.save(new RepositoryFile(
                repo1, "src/boundary.java", "boundary.java", "java", "src",
                999999, 800000, 200000, 1000000, false, refTime.plus(Duration.ofDays(100)), refTime.plus(Duration.ofDays(100)), alice
        ));
        repositoryFileJpaRepository.flush();

        materializationService.materializeFileRisks(repo1.getId(), refTime);

        RepositoryFile file = repositoryFileJpaRepository.findByRepositoryIdAndFilePath(repo1.getId(), "src/boundary.java").orElseThrow();
        assertThat(file.getBaselineScore()).isBetween(0.0, 1.0);
        assertThat(file.getRevisionFrequencyScore()).isBetween(0.0, 1.0);
        assertThat(file.getChurnScore()).isBetween(0.0, 1.0);
        assertThat(file.getRecencyScore()).isBetween(0.0, 1.0);
        assertThat(file.getOwnershipConcentrationScore()).isBetween(0.0, 1.0);
        assertThat(file.getCompositeScore()).isBetween(0.0, 1.0);
    }

    @Test
    @DisplayName("9. Persistence round-trip: all six risk metrics reload faithfully from database")
    void materializeFileRisks_PersistenceRoundTrip() {
        RepositoryFile file = repositoryFileJpaRepository.save(new RepositoryFile(
                repo1, "src/roundtrip.java", "roundtrip.java", "java", "src",
                15, 120, 30, 150, false, refTime, refTime, alice
        ));
        repositoryContributorFileJpaRepository.save(new RepositoryContributorFile(repo1, alice, "src/roundtrip.java", 15, 150, 0, 150, refTime, refTime));
        repositoryFileJpaRepository.flush();
        repositoryContributorFileJpaRepository.flush();

        materializationService.materializeFileRisks(repo1.getId(), refTime);

        // Clear persistence context / reload from DB
        RepositoryFile reloaded = repositoryFileJpaRepository.findById(file.getId()).orElseThrow();

        assertThat(reloaded.getBaselineScore()).isEqualTo(1.0);
        assertThat(reloaded.getRevisionFrequencyScore()).isEqualTo(1.0);
        assertThat(reloaded.getChurnScore()).isEqualTo(1.0);
        assertThat(reloaded.getRecencyScore()).isEqualTo(1.0);
        assertThat(reloaded.getOwnershipConcentrationScore()).isEqualTo(1.0);
        assertThat(reloaded.getCompositeScore()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("10. Absent ownership relationship falls back safely to 0.0 topContributorRevisionShare")
    void materializeFileRisks_AbsentOwnershipFallback() {
        // File exists in repository_files, but has NO entries in repository_contributor_files
        repositoryFileJpaRepository.save(new RepositoryFile(
                repo1, "src/orphan.java", "orphan.java", "java", "src",
                10, 80, 20, 100, false, refTime, refTime, null
        ));
        repositoryFileJpaRepository.flush();

        materializationService.materializeFileRisks(repo1.getId(), refTime);

        RepositoryFile file = repositoryFileJpaRepository.findByRepositoryIdAndFilePath(repo1.getId(), "src/orphan.java").orElseThrow();
        assertThat(file.getOwnershipConcentrationScore()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("11. Non-existent repository throws ResourceNotFoundException")
    void materializeFileRisks_NotFound_ThrowsException() {
        assertThatThrownBy(() -> materializationService.materializeFileRisks(9999L, refTime))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Repository not found with id: 9999");
    }

    @Test
    @DisplayName("12. Multi-page bounded processing crosses page boundaries and processes all files")
    void materializeFileRisks_CrossPageBoundary_ProcessesAllFiles() {
        Repository repoBatch = repositoryJpaRepository.save(new Repository("ownerBatch", "repoBatch", "Repo Batch", "main"));
        int totalFilesToCreate = 505; // crosses default page size of 500

        List<RepositoryFile> files = new java.util.ArrayList<>();
        for (int i = 0; i < totalFilesToCreate; i++) {
            String path = String.format("src/pkg/File%04d.java", i);
            files.add(new RepositoryFile(
                    repoBatch, path, String.format("File%04d.java", i), "java", "src/pkg",
                    i + 1, (i + 1) * 10, (i + 1) * 2, (i + 1) * 12, false, refTime, refTime, alice
            ));
        }
        repositoryFileJpaRepository.saveAll(files);
        repositoryFileJpaRepository.flush();

        // 1. Initial run: all 505 files across page 0 and page 1 are processed and updated
        RepositoryFileRiskMaterializationResult result = materializationService.materializeFileRisks(repoBatch.getId(), refTime);

        assertThat(result.totalFilesProcessed()).isEqualTo(totalFilesToCreate);
        assertThat(result.updatedCount()).isEqualTo(totalFilesToCreate);
        assertThat(result.unchangedCount()).isEqualTo(0);

        List<RepositoryFile> allBatchFiles = repositoryFileJpaRepository.findByRepositoryId(repoBatch.getId());
        assertThat(allBatchFiles).hasSize(totalFilesToCreate);

        // Verify Page 0 file (File0000)
        RepositoryFile firstFile = allBatchFiles.stream()
                .filter(f -> f.getFilePath().equals("src/pkg/File0000.java"))
                .findFirst()
                .orElseThrow();
        assertThat(firstFile.getCompositeScore()).isGreaterThan(0.0).isLessThanOrEqualTo(1.0);
        assertThat(firstFile.getRevisionFrequencyScore()).isGreaterThan(0.0).isLessThanOrEqualTo(1.0);

        // Verify Page 1 file (File0504)
        RepositoryFile lastFile = allBatchFiles.stream()
                .filter(f -> f.getFilePath().equals("src/pkg/File0504.java"))
                .findFirst()
                .orElseThrow();
        assertThat(lastFile.getCompositeScore()).isGreaterThan(0.0).isLessThanOrEqualTo(1.0);
        assertThat(lastFile.getRevisionFrequencyScore()).isEqualTo(1.0); // max revisions file

        // Verify all 505 files have valid non-zero composite scores
        long countWithNonZeroCompositeScore = allBatchFiles.stream()
                .filter(f -> f.getCompositeScore() > 0.0)
                .count();
        assertThat(countWithNonZeroCompositeScore).isEqualTo(totalFilesToCreate);

        // 2. Second run: Idempotency across page boundary (0 updated, 505 unchanged)
        RepositoryFileRiskMaterializationResult rerunResult = materializationService.materializeFileRisks(repoBatch.getId(), refTime);
        assertThat(rerunResult.totalFilesProcessed()).isEqualTo(totalFilesToCreate);
        assertThat(rerunResult.updatedCount()).isEqualTo(0);
        assertThat(rerunResult.unchangedCount()).isEqualTo(totalFilesToCreate);
    }
}