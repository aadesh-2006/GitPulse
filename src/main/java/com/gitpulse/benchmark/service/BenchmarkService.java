package com.gitpulse.benchmark.service;

import com.gitpulse.benchmark.dto.BenchmarkEvaluationItem;
import com.gitpulse.benchmark.dto.BenchmarkFileSnapshot;
import com.gitpulse.benchmark.dto.BenchmarkFutureOutcome;
import com.gitpulse.benchmark.dto.BenchmarkRankingMetrics;
import com.gitpulse.benchmark.dto.BenchmarkResult;
import com.gitpulse.benchmark.dto.BenchmarkSignalComparison;
import com.gitpulse.benchmark.dto.FutureFileOutcomeRow;
import com.gitpulse.benchmark.dto.HistoricalAuthorFileCountRow;
import com.gitpulse.benchmark.dto.HistoricalFileSnapshotRow;
import com.gitpulse.benchmark.metric.BenchmarkMetricsCalculator;
import com.gitpulse.benchmark.repository.BenchmarkQueryRepository;
import com.gitpulse.common.exception.ResourceNotFoundException;
import com.gitpulse.domain.repository.Repository;
import com.gitpulse.domain.repository.RepositoryJpaRepository;
import com.gitpulse.domain.risk.FileRiskInput;
import com.gitpulse.domain.risk.FileRiskNormalizationContext;
import com.gitpulse.domain.risk.FileRiskScore;
import com.gitpulse.domain.risk.FileRiskScoringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

/**
 * Core execution engine for temporal repository risk benchmarking.
 * <p>
 * Strictly isolates historical evaluation windows from future outcomes to prevent data leakage.
 */
@Service
public class BenchmarkService {

    private static final Logger log = LoggerFactory.getLogger(BenchmarkService.class);

    private final RepositoryJpaRepository repositoryJpaRepository;
    private final BenchmarkQueryRepository benchmarkQueryRepository;
    private final FileRiskScoringService fileRiskScoringService;

    public BenchmarkService(
            RepositoryJpaRepository repositoryJpaRepository,
            BenchmarkQueryRepository benchmarkQueryRepository,
            FileRiskScoringService fileRiskScoringService
    ) {
        this.repositoryJpaRepository = Objects.requireNonNull(repositoryJpaRepository, "repositoryJpaRepository must not be null");
        this.benchmarkQueryRepository = Objects.requireNonNull(benchmarkQueryRepository, "benchmarkQueryRepository must not be null");
        this.fileRiskScoringService = Objects.requireNonNull(fileRiskScoringService, "fileRiskScoringService must not be null");
    }

    /**
     * Executes a temporal evaluation benchmark for the given repository.
     *
     * @param repositoryId    ID of the repository to benchmark, must not be null
     * @param cutoffTime      historical snapshot cutoff timestamp, must not be null
     * @param horizonDuration duration of the future observation window, must not be null and positive
     * @return full {@link BenchmarkResult}
     */
    @Transactional(readOnly = true)
    public BenchmarkResult evaluateRepository(
            Long repositoryId,
            Instant cutoffTime,
            Duration horizonDuration
    ) {
        Objects.requireNonNull(repositoryId, "repositoryId must not be null");
        Objects.requireNonNull(cutoffTime, "cutoffTime must not be null");
        Objects.requireNonNull(horizonDuration, "horizonDuration must not be null");
        if (horizonDuration.isNegative() || horizonDuration.isZero()) {
            throw new IllegalArgumentException("horizonDuration must be strictly positive");
        }

        long startNanos = System.nanoTime();
        Repository repository = repositoryJpaRepository.findById(repositoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Repository", "id", repositoryId));

        Instant horizonEndTime = cutoffTime.plus(horizonDuration);
        log.info("Starting temporal benchmark for repo [id={}, name={}], cutoff={}, horizonEnd={}",
                repositoryId, repository.getFullName(), cutoffTime, horizonEndTime);

        // 1. Reconstruct historical snapshot up to cutoff
        List<HistoricalFileSnapshotRow> rawSnapshots = benchmarkQueryRepository.findHistoricalFileSnapshots(repositoryId, cutoffTime);
        List<HistoricalAuthorFileCountRow> authorRows = benchmarkQueryRepository.findHistoricalAuthorFileCounts(repositoryId, cutoffTime);

        // Compute ownership share per file up to cutoff
        Map<String, Map<String, Long>> fileAuthorRevisions = new HashMap<>();
        for (HistoricalAuthorFileCountRow authorRow : authorRows) {
            fileAuthorRevisions
                    .computeIfAbsent(authorRow.getFilePath(), k -> new HashMap<>())
                    .put(authorRow.getAuthorKey(), authorRow.getRevisionCount());
        }

        long maxRevisions = 0L;
        long maxChurn = 0L;
        for (HistoricalFileSnapshotRow row : rawSnapshots) {
            if (row.getTotalRevisions() > maxRevisions) {
                maxRevisions = row.getTotalRevisions();
            }
            if (row.getTotalChurn() > maxChurn) {
                maxChurn = row.getTotalChurn();
            }
        }

        FileRiskNormalizationContext context = new FileRiskNormalizationContext(maxRevisions, maxChurn);

        List<BenchmarkFileSnapshot> snapshots = new ArrayList<>(rawSnapshots.size());
        for (HistoricalFileSnapshotRow row : rawSnapshots) {
            String filePath = row.getFilePath();
            Map<String, Long> authors = fileAuthorRevisions.getOrDefault(filePath, Collections.emptyMap());
            long topAuthorRevs = authors.values().stream().max(Long::compareTo).orElse(0L);
            long totalAuthorRevs = authors.values().stream().mapToLong(Long::longValue).sum();
            double topContributorShare = totalAuthorRevs > 0 ? ((double) topAuthorRevs / totalAuthorRevs) : 0.0;

            Instant lastModifiedAt = row.getLastModifiedAtInstant();

            FileRiskInput input = new FileRiskInput(
                    row.getTotalRevisions(),
                    row.getTotalChurn(),
                    lastModifiedAt,
                    topContributorShare
            );

            FileRiskScore score = fileRiskScoringService.scoreFile(input, context, cutoffTime);

            snapshots.add(new BenchmarkFileSnapshot(
                    filePath,
                    row.getTotalRevisions(),
                    row.getTotalChurn(),
                    lastModifiedAt,
                    topContributorShare,
                    score
            ));
        }

        // 2. Query future outcomes in window (cutoffTime, horizonEndTime]
        List<FutureFileOutcomeRow> futureRows = benchmarkQueryRepository.findFutureFileOutcomes(repositoryId, cutoffTime, horizonEndTime);
        Map<String, BenchmarkFutureOutcome> futureMap = futureRows.stream()
                .collect(Collectors.toMap(
                        FutureFileOutcomeRow::getFilePath,
                        row -> new BenchmarkFutureOutcome(
                                row.getFilePath(),
                                row.getFutureRevisionCount(),
                                row.getFutureChurn(),
                                row.getFutureDistinctContributors(),
                                row.getFutureRevisionCount() > 0
                        ),
                        (existing, replacement) -> existing
                ));

        // 3. Assemble evaluation population
        Set<String> historicalPaths = snapshots.stream().map(BenchmarkFileSnapshot::filePath).collect(Collectors.toSet());
        List<BenchmarkEvaluationItem> evaluationItems = new ArrayList<>(snapshots.size());
        int futureChangedHistoricalFiles = 0;

        for (BenchmarkFileSnapshot snapshot : snapshots) {
            BenchmarkFutureOutcome outcome = futureMap.getOrDefault(snapshot.filePath(), BenchmarkFutureOutcome.empty(snapshot.filePath()));
            if (outcome.futureChanged()) {
                futureChangedHistoricalFiles++;
            }
            evaluationItems.add(new BenchmarkEvaluationItem(snapshot, outcome));
        }

        int totalActiveFutureFiles = futureMap.size();
        int newFilesIntroducedInFuture = (int) futureMap.keySet().stream()
                .filter(path -> !historicalPaths.contains(path))
                .count();

        // 4. Compute ranking and statistical metrics across signals
        BenchmarkRankingMetrics baselineMetrics = evaluateSignal(
                "baseline",
                evaluationItems,
                item -> item.snapshot().scores().baselineScore(),
                futureChangedHistoricalFiles
        );

        BenchmarkRankingMetrics compositeMetrics = evaluateSignal(
                "composite",
                evaluationItems,
                item -> item.snapshot().scores().compositeScore(),
                futureChangedHistoricalFiles
        );

        Map<String, BenchmarkRankingMetrics> ablationMetrics = new LinkedHashMap<>();
        ablationMetrics.put("baseline", baselineMetrics);
        ablationMetrics.put("composite", compositeMetrics);
        ablationMetrics.put("revisionFrequency", evaluateSignal(
                "revisionFrequency",
                evaluationItems,
                item -> item.snapshot().scores().revisionFrequencyScore(),
                futureChangedHistoricalFiles
        ));
        ablationMetrics.put("churn", evaluateSignal(
                "churn",
                evaluationItems,
                item -> item.snapshot().scores().churnScore(),
                futureChangedHistoricalFiles
        ));
        ablationMetrics.put("recency", evaluateSignal(
                "recency",
                evaluationItems,
                item -> item.snapshot().scores().recencyScore(),
                futureChangedHistoricalFiles
        ));
        ablationMetrics.put("ownershipConcentration", evaluateSignal(
                "ownershipConcentration",
                evaluationItems,
                item -> item.snapshot().scores().ownershipConcentrationScore(),
                futureChangedHistoricalFiles
        ));

        BenchmarkSignalComparison comparisonSummary = BenchmarkSignalComparison.of(baselineMetrics, compositeMetrics);
        long executionDurationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);

        log.info("Temporal benchmark completed for repo [id={}] in {}ms. Baseline AUC={}, Composite AUC={}",
                repositoryId, executionDurationMs, baselineMetrics.rocAreaUnderCurve(), compositeMetrics.rocAreaUnderCurve());

        return new BenchmarkResult(
                repositoryId,
                repository.getFullName(),
                cutoffTime,
                horizonDuration,
                snapshots.size(),
                totalActiveFutureFiles,
                futureChangedHistoricalFiles,
                newFilesIntroducedInFuture,
                baselineMetrics,
                compositeMetrics,
                ablationMetrics,
                comparisonSummary,
                executionDurationMs
        );
    }

    private BenchmarkRankingMetrics evaluateSignal(
            String signalName,
            List<BenchmarkEvaluationItem> items,
            ToDoubleFunction<BenchmarkEvaluationItem> scoreExtractor,
            long totalPositives
    ) {
        List<BenchmarkEvaluationItem> ranked = new ArrayList<>(items);
        ranked.sort(
                Comparator.<BenchmarkEvaluationItem>comparingDouble(scoreExtractor::applyAsDouble).reversed()
                        .thenComparing(Comparator.comparingLong((BenchmarkEvaluationItem item) -> item.snapshot().historicalRevisions()).reversed())
                        .thenComparing(BenchmarkEvaluationItem::filePath)
        );

        double p5 = BenchmarkMetricsCalculator.precisionAtK(ranked, BenchmarkEvaluationItem::futureChanged, 5);
        double p10 = BenchmarkMetricsCalculator.precisionAtK(ranked, BenchmarkEvaluationItem::futureChanged, 10);
        double p20 = BenchmarkMetricsCalculator.precisionAtK(ranked, BenchmarkEvaluationItem::futureChanged, 20);

        double r5 = BenchmarkMetricsCalculator.recallAtK(ranked, BenchmarkEvaluationItem::futureChanged, totalPositives, 5);
        double r10 = BenchmarkMetricsCalculator.recallAtK(ranked, BenchmarkEvaluationItem::futureChanged, totalPositives, 10);
        double r20 = BenchmarkMetricsCalculator.recallAtK(ranked, BenchmarkEvaluationItem::futureChanged, totalPositives, 20);

        double h5 = BenchmarkMetricsCalculator.hitRateAtK(ranked, BenchmarkEvaluationItem::futureChanged, 5);
        double h10 = BenchmarkMetricsCalculator.hitRateAtK(ranked, BenchmarkEvaluationItem::futureChanged, 10);
        double h20 = BenchmarkMetricsCalculator.hitRateAtK(ranked, BenchmarkEvaluationItem::futureChanged, 20);

        double spearmanRevs = BenchmarkMetricsCalculator.spearmanCorrelation(
                items,
                scoreExtractor,
                item -> (double) item.futureRevisionCount()
        );

        double spearmanChurn = BenchmarkMetricsCalculator.spearmanCorrelation(
                items,
                scoreExtractor,
                item -> (double) item.futureChurn()
        );

        double rocAuc = BenchmarkMetricsCalculator.rocAreaUnderCurve(
                items,
                scoreExtractor,
                BenchmarkEvaluationItem::futureChanged
        );

        return new BenchmarkRankingMetrics(
                signalName,
                p5,
                p10,
                p20,
                r5,
                r10,
                r20,
                h5,
                h10,
                h20,
                spearmanRevs,
                spearmanChurn,
                rocAuc
        );
    }
}
