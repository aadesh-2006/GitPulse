-- Step 10: Create repository_contributor_files table for contributor-file intelligence read model

CREATE TABLE repository_contributor_files (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    repository_id BIGINT NOT NULL,
    contributor_id BIGINT NOT NULL,
    file_path VARCHAR(1000) NOT NULL,
    total_revisions BIGINT NOT NULL DEFAULT 0,
    total_additions BIGINT NOT NULL DEFAULT 0,
    total_deletions BIGINT NOT NULL DEFAULT 0,
    total_churn BIGINT NOT NULL DEFAULT 0,
    first_contributed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_contributed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_repo_contrib_files_repo FOREIGN KEY (repository_id) REFERENCES repositories(id) ON DELETE CASCADE,
    CONSTRAINT fk_repo_contrib_files_contrib FOREIGN KEY (contributor_id) REFERENCES contributors(id) ON DELETE CASCADE,
    CONSTRAINT uq_repo_contrib_files UNIQUE (repository_id, contributor_id, file_path)
);

CREATE INDEX idx_rcf_repo_id ON repository_contributor_files(repository_id);
CREATE INDEX idx_rcf_contrib_id ON repository_contributor_files(contributor_id);
CREATE INDEX idx_rcf_repo_contrib ON repository_contributor_files(repository_id, contributor_id);
CREATE INDEX idx_rcf_repo_file ON repository_contributor_files(repository_id, file_path);
CREATE INDEX idx_rcf_repo_churn ON repository_contributor_files(repository_id, total_churn DESC);
CREATE INDEX idx_rcf_repo_contrib_churn ON repository_contributor_files(repository_id, contributor_id, total_churn DESC);
