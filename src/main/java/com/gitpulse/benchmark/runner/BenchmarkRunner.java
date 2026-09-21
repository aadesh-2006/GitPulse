package com.gitpulse.benchmark.runner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gitpulse.benchmark.dto.BenchmarkRankingMetrics;
import com.gitpulse.benchmark.dto.BenchmarkResult;
import com.gitpulse.benchmark.dto.BenchmarkSignalComparison;
import com.gitpulse.benchmark.service.BenchmarkService;
import com.gitpulse.domain.commit.CommitJpaRepository;
import com.gitpulse.domain.repository.Repository;
import com.gitpulse.domain.repository.RepositoryJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Command-line and automated execution runner for repository risk benchmarks.
 * Activated via {@code gitpulse.benchmark.enabled=true} or {@code --benchmark.enabled=true}.
 */
@Component
@ConditionalOnProperty(name = "gitpulse.benchmark.enabled", havingValue = "true")
public class BenchmarkRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BenchmarkRunner.class);

    private final BenchmarkService benchmarkService;
    private final RepositoryJpaRepository repositoryJpaRepository;
    private final CommitJpaRepository commitJpaRepository;
    private final ObjectMapper objectMapper;

    @Value("${gitpulse.benchmark.repository-id:#{null}}")
    private Long configuredRepositoryId;

    @Value("${gitpulse.benchmark.repository-name:#{null}}")
    private String configuredRepositoryName;

    @Value("${gitpulse.benchmark.cutoff:#{null}}")
    private String configuredCutoff;

    @Value("${gitpulse.benchmark.horizon-days:90}")
    private long configuredHorizonDays;

    public BenchmarkRunner(
            BenchmarkService benchmarkService,
            RepositoryJpaRepository repositoryJpaRepository,
            CommitJpaRepository commitJpaRepository
    ) {
        this.benchmarkService = benchmarkService;
        this.repositoryJpaRepository = repositoryJpaRepository;
        this.commitJpaRepository = commitJpaRepository;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .enable(SerializationFeature.INDENT_OUTPUT)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("================================================================================");
        log.info("                  GITPULSE BENCHMARK EVALUATION HARNESS                        ");
        log.info("================================================================================");

        Long repoId = resolveRepositoryId(args);
        if (repoId == null) {
            log.error("Benchmark failed: No valid repository ID or repository name specified.");
            return;
        }

        Repository repo = repositoryJpaRepository.findById(repoId).orElse(null);
        if (repo == null) {
            log.error("Benchmark failed: Repository with id {} not found.", repoId);
            return;
        }

        Instant cutoffTime = resolveCutoffTime(args, repoId);
        Duration horizon = resolveHorizonDuration(args);

        log.info("Benchmarking Repository: {} (id={})", repo.getFullName(), repo.getId());
        log.info("Historical Cutoff (T_cutoff): {}", cutoffTime);
        log.info("Future Horizon Duration: {} days (Window: [{}, {}])",
                horizon.toDays(), cutoffTime, cutoffTime.plus(horizon));

        BenchmarkResult result = benchmarkService.evaluateRepository(repoId, cutoffTime, horizon);

        printFormattedReport(result);

        String jsonResult = objectMapper.writeValueAsString(result);
        log.info("\n--- BENCHMARK JSON SUMMARY ---\n{}\n--- END BENCHMARK JSON ---", jsonResult);
    }

    private Long resolveRepositoryId(ApplicationArguments args) {
        if (args.containsOption("benchmark.repo-id")) {
            return Long.parseLong(args.getOptionValues("benchmark.repo-id").get(0));
        }
        if (args.containsOption("benchmark.repo")) {
            String name = args.getOptionValues("benchmark.repo").get(0);
            return findRepositoryIdByName(name);
        }
        if (configuredRepositoryId != null) {
            return configuredRepositoryId;
        }
        if (configuredRepositoryName != null && !configuredRepositoryName.isBlank()) {
            return findRepositoryIdByName(configuredRepositoryName);
        }

        // Fallback to first available repository
        return repositoryJpaRepository.findAll().stream()
                .findFirst()
                .map(Repository::getId)
                .orElse(null);
    }

    private Long findRepositoryIdByName(String fullName) {
        return repositoryJpaRepository.findByFullNameIgnoreCase(fullName.trim())
                .map(Repository::getId)
                .orElse(null);
    }

    private Instant resolveCutoffTime(ApplicationArguments args, Long repoId) {
        String cutoffStr = null;
        if (args.containsOption("benchmark.cutoff")) {
            cutoffStr = args.getOptionValues("benchmark.cutoff").get(0);
        } else if (configuredCutoff != null && !configuredCutoff.isBlank()) {
            cutoffStr = configuredCutoff;
        }

        if (cutoffStr != null && !cutoffStr.isBlank()) {
            return Instant.parse(cutoffStr.trim());
        }

        // Default cutoff: 90 days before the latest commit
        Instant latestCommitTime = commitJpaRepository.findLatestCommittedAtByRepositoryId(repoId);
        if (latestCommitTime != null) {
            return latestCommitTime.minus(Duration.ofDays(configuredHorizonDays));
        }

        return Instant.now().minus(Duration.ofDays(configuredHorizonDays));
    }

    private Duration resolveHorizonDuration(ApplicationArguments args) {
        long days = configuredHorizonDays;
        if (args.containsOption("benchmark.horizonDays")) {
            days = Long.parseLong(args.getOptionValues("benchmark.horizonDays").get(0));
        }
        return Duration.ofDays(Math.max(1, days));
    }

    private void printFormattedReport(BenchmarkResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n================================================================================\n");
        sb.append("                            BENCHMARK RESULTS REPORT                            \n");
        sb.append("================================================================================\n");
        sb.append(String.format("Repository:                 %s (id=%d)\n", result.repositoryName(), result.repositoryId()));
        sb.append(String.format("Cutoff Time (T_cutoff):     %s\n", result.cutoffTime()));
        sb.append(String.format("Horizon Duration:           %d days\n", result.horizonDuration().toDays()));
        sb.append(String.format("Historical Files (Cutoff):  %d\n", result.totalHistoricalFiles()));
        sb.append(String.format("Future Active Files:        %d\n", result.totalActiveFutureFiles()));
        sb.append(String.format("Historical Files Changed:   %d (%.1f%%)\n",
                result.futureChangedHistoricalFiles(),
                result.totalHistoricalFiles() > 0 ? (result.futureChangedHistoricalFiles() * 100.0 / result.totalHistoricalFiles()) : 0.0));
        sb.append(String.format("New Files in Future:        %d\n", result.newFilesIntroducedInFuture()));
        sb.append(String.format("Benchmark Runtime:          %d ms\n", result.executionDurationMs()));
        sb.append("--------------------------------------------------------------------------------\n");
        sb.append(String.format(Locale.US, "%-22s | %-6s %-6s %-6s | %-6s %-6s %-6s | %-7s | %-7s | %-7s\n",
                "Signal", "P@5", "P@10", "P@20", "R@5", "R@10", "R@20", "SpearmanR", "SpearmanC", "ROC-AUC"));
        sb.append("--------------------------------------------------------------------------------\n");

        for (Map.Entry<String, BenchmarkRankingMetrics> entry : result.ablationMetrics().entrySet()) {
            BenchmarkRankingMetrics m = entry.getValue();
            sb.append(String.format(Locale.US,
                    "%-22s | %-6.3f %-6.3f %-6.3f | %-6.3f %-6.3f %-6.3f | %-9.4f | %-9.4f | %-7.4f\n",
                    m.signalName(),
                    m.precisionAt5(), m.precisionAt10(), m.precisionAt20(),
                    m.recallAt5(), m.recallAt10(), m.recallAt20(),
                    m.spearmanCorrelationRevisions(),
                    m.spearmanCorrelationChurn(),
                    m.rocAreaUnderCurve()
            ));
        }

        sb.append("--------------------------------------------------------------------------------\n");
        BenchmarkSignalComparison cmp = result.comparisonSummary();
        sb.append("HYPOTHESIS EVALUATION SUMMARY (Composite vs Baseline):\n");
        sb.append(String.format(Locale.US, "  Precision@10:    Baseline=%.3f, Composite=%.3f (Delta=%+.3f)\n",
                cmp.baselinePrecisionAt10(), cmp.compositePrecisionAt10(), cmp.precisionDeltaAt10()));
        sb.append(String.format(Locale.US, "  Recall@10:       Baseline=%.3f, Composite=%.3f (Delta=%+.3f)\n",
                cmp.baselineRecallAt10(), cmp.compositeRecallAt10(), cmp.recallDeltaAt10()));
        sb.append(String.format(Locale.US, "  Spearman (Revs): Baseline=%.4f, Composite=%.4f (Delta=%+.4f)\n",
                cmp.baselineSpearmanRevisions(), cmp.compositeSpearmanRevisions(), cmp.spearmanRevisionsDelta()));
        sb.append(String.format(Locale.US, "  ROC-AUC:         Baseline=%.4f, Composite=%.4f (Delta=%+.4f)\n",
                cmp.baselineRocAuc(), cmp.compositeRocAuc(), cmp.rocAucDelta()));
        sb.append("================================================================================\n");

        log.info("{}", sb);
    }
}
