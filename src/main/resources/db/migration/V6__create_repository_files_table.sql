-- Step 8: Create repository_files table for materialized file activity and code churn read model

CREATE TABLE repository_files (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    repository_id BIGINT NOT NULL,
    file_path VARCHAR(1000) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    extension VARCHAR(50),
    directory_path VARCHAR(1000),
    total_revisions INTEGER NOT NULL DEFAULT 0,
    total_additions INTEGER NOT NULL DEFAULT 0,
    total_deletions INTEGER NOT NULL DEFAULT 0,
    total_churn INTEGER NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    first_modified_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_modified_at TIMESTAMP WITH TIME ZONE NOT NULL,
    primary_contributor_id BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_repo_files_repository FOREIGN KEY (repository_id) REFERENCES repositories(id) ON DELETE CASCADE,
    CONSTRAINT fk_repo_files_primary_contributor FOREIGN KEY (primary_contributor_id) REFERENCES contributors(id) ON DELETE SET NULL,
    CONSTRAINT uq_repo_files_repo_path UNIQUE (repository_id, file_path)
);

CREATE INDEX idx_repo_files_repo_id ON repository_files(repository_id);
CREATE INDEX idx_repo_files_repo_churn ON repository_files(repository_id, total_churn DESC);
CREATE INDEX idx_repo_files_repo_revisions ON repository_files(repository_id, total_revisions DESC);
CREATE INDEX idx_repo_files_repo_last_modified ON repository_files(repository_id, last_modified_at DESC);
CREATE INDEX idx_repo_files_repo_extension ON repository_files(repository_id, extension);
CREATE INDEX idx_repo_files_repo_deleted ON repository_files(repository_id, is_deleted);