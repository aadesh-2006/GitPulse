-- Step 9: Add commit classification column and index for commit category analytics

ALTER TABLE commits ADD COLUMN classification VARCHAR(50);

CREATE INDEX idx_commits_repo_classification ON commits(repository_id, classification);
