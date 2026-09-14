-- Step 5: Create commits table for GitHub commit history ingestion

CREATE TABLE commits (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    repository_id BIGINT NOT NULL,
    github_commit_sha VARCHAR(40) NOT NULL,
    message TEXT NOT NULL,
    author_name VARCHAR(200),
    author_email VARCHAR(200),
    author_username VARCHAR(100),
    committed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    additions INTEGER,
    deletions INTEGER,
    total_changes INTEGER,
    html_url VARCHAR(300),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_commits_repository_id FOREIGN KEY (repository_id) REFERENCES repositories(id) ON DELETE CASCADE,
    CONSTRAINT uq_commits_repo_sha UNIQUE (repository_id, github_commit_sha)
);

CREATE INDEX idx_commits_repository_id ON commits(repository_id);
CREATE INDEX idx_commits_repo_committed_at ON commits(repository_id, committed_at);
