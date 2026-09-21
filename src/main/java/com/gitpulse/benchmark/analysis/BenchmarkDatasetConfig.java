package com.gitpulse.benchmark.analysis;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Deterministic configuration model for the benchmark evaluation dataset.
 */
public record BenchmarkDatasetConfig(
        Duration horizonDuration,
        int cutoffsPerRepository,
        String primaryMetric,
        int bootstrapResamples,
        long randomSeed,
        List<RepositoryBenchmarkSpec> targetRepositories
) {
    public static final Duration DEFAULT_HORIZON = Duration.ofDays(90);
    public static final int DEFAULT_CUTOFFS_PER_REPO = 3;
    public static final String DEFAULT_PRIMARY_METRIC = "precisionAt10";
    public static final int DEFAULT_BOOTSTRAP_RESAMPLES = 10_000;
    public static final long DEFAULT_RANDOM_SEED = 20260921L;

    public BenchmarkDatasetConfig {
        Objects.requireNonNull(horizonDuration, "horizonDuration must not be null");
        Objects.requireNonNull(primaryMetric, "primaryMetric must not be null");
        Objects.requireNonNull(targetRepositories, "targetRepositories must not be null");
        if (cutoffsPerRepository <= 0) {
            throw new IllegalArgumentException("cutoffsPerRepository must be strictly positive");
        }
        if (bootstrapResamples <= 0) {
            throw new IllegalArgumentException("bootstrapResamples must be strictly positive");
        }
    }

    /**
     * Creates standard default dataset configuration containing 15 diverse public open-source benchmark targets.
     */
    public static BenchmarkDatasetConfig createStandardDataset() {
        List<RepositoryBenchmarkSpec> repos = List.of(
                new RepositoryBenchmarkSpec("octocat", "Hello-World", "Small", "Starter educational repository"),
                new RepositoryBenchmarkSpec("pallets", "flask", "Medium", "Python lightweight WSGI web framework"),
                new RepositoryBenchmarkSpec("psf", "requests", "Medium", "HTTP library for Python"),
                new RepositoryBenchmarkSpec("expressjs", "express", "Medium", "Fast, unopinionated web framework for Node.js"),
                new RepositoryBenchmarkSpec("facebook", "react", "Large", "JavaScript library for building user interfaces"),
                new RepositoryBenchmarkSpec("spring-projects", "spring-boot", "Large", "Enterprise Java framework and runtime"),
                new RepositoryBenchmarkSpec("vuejs", "core", "Large", "Progressive JavaScript Framework"),
                new RepositoryBenchmarkSpec("gin-gonic", "gin", "Medium", "High-performance HTTP web framework written in Go"),
                new RepositoryBenchmarkSpec("rust-lang", "rust-clippy", "Large", "Lints to catch common mistakes in Rust code"),
                new RepositoryBenchmarkSpec("axios", "axios", "Medium", "Promise based HTTP client for the browser and Node.js"),
                new RepositoryBenchmarkSpec("torvalds", "linux", "Large", "Linux kernel source tree"),
                new RepositoryBenchmarkSpec("curl", "curl", "Large", "Command line tool and library for transferring data with URLs"),
                new RepositoryBenchmarkSpec("tiangolo", "fastapi", "Medium", "FastAPI framework, high performance, easy to learn"),
                new RepositoryBenchmarkSpec("chalk", "chalk", "Small", "Terminal string styling done right for Node.js"),
                new RepositoryBenchmarkSpec("google", "guava", "Large", "Google Core Libraries for Java")
        );

        return new BenchmarkDatasetConfig(
                DEFAULT_HORIZON,
                DEFAULT_CUTOFFS_PER_REPO,
                DEFAULT_PRIMARY_METRIC,
                DEFAULT_BOOTSTRAP_RESAMPLES,
                DEFAULT_RANDOM_SEED,
                repos
        );
    }

    public record RepositoryBenchmarkSpec(
            String owner,
            String name,
            String sizeCategory,
            String description
    ) {
        public String fullName() {
            return owner + "/" + name;
        }
    }
}
