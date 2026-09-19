-- Step 11: Add derived file risk and stability metrics to repository_files

ALTER TABLE repository_files ADD COLUMN baseline_score DOUBLE PRECISION NOT NULL DEFAULT 0.0;
ALTER TABLE repository_files ADD COLUMN revision_frequency_score DOUBLE PRECISION NOT NULL DEFAULT 0.0;
ALTER TABLE repository_files ADD COLUMN churn_score DOUBLE PRECISION NOT NULL DEFAULT 0.0;
ALTER TABLE repository_files ADD COLUMN recency_score DOUBLE PRECISION NOT NULL DEFAULT 0.0;
ALTER TABLE repository_files ADD COLUMN ownership_concentration_score DOUBLE PRECISION NOT NULL DEFAULT 0.0;
ALTER TABLE repository_files ADD COLUMN composite_score DOUBLE PRECISION NOT NULL DEFAULT 0.0;

CREATE INDEX idx_repository_files_repo_composite_score ON repository_files(repository_id, composite_score DESC);
CREATE INDEX idx_repository_files_repo_baseline_score ON repository_files(repository_id, baseline_score DESC);