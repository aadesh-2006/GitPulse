-- Step 6: Create file_changes table for GitHub commit file-change history

CREATE TABLE file_changes (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    commit_id BIGINT NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    status VARCHAR(50) NOT NULL,
    additions INTEGER,
    deletions INTEGER,
    changes INTEGER,
    blob_url VARCHAR(500),
    raw_url VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_file_changes_commit_id FOREIGN KEY (commit_id) REFERENCES commits(id) ON DELETE CASCADE,
    CONSTRAINT uq_file_changes_commit_file UNIQUE (commit_id, file_path)
);

CREATE INDEX idx_file_changes_commit_id ON file_changes(commit_id);
CREATE INDEX idx_file_changes_file_path ON file_changes(file_path);
