-- Step 7: Create contributors and repository_contributors tables

CREATE TABLE contributors (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email VARCHAR(200) NOT NULL,
    username VARCHAR(100),
    name VARCHAR(200),
    avatar_url VARCHAR(500),
    github_id BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_contributors_email UNIQUE (email)
);

CREATE INDEX idx_contributors_username ON contributors(username);

CREATE TABLE repository_contributors (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    repository_id BIGINT NOT NULL,
    contributor_id BIGINT NOT NULL,
    total_commits INTEGER NOT NULL DEFAULT 0,
    total_additions INTEGER NOT NULL DEFAULT 0,
    total_deletions INTEGER NOT NULL DEFAULT 0,
    total_changes INTEGER NOT NULL DEFAULT 0,
    first_committed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_committed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_repo_contrib_repo FOREIGN KEY (repository_id) REFERENCES repositories(id) ON DELETE CASCADE,
    CONSTRAINT fk_repo_contrib_contrib FOREIGN KEY (contributor_id) REFERENCES contributors(id) ON DELETE CASCADE,
    CONSTRAINT uq_repo_contrib UNIQUE (repository_id, contributor_id)
);

CREATE INDEX idx_repo_contrib_repo_id ON repository_contributors(repository_id);
CREATE INDEX idx_repo_contrib_contrib_id ON repository_contributors(contributor_id);
CREATE INDEX idx_repo_contrib_commits ON repository_contributors(repository_id, total_commits DESC);
