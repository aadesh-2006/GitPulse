-- Step 3: Add GitHub metadata fields to repositories table

ALTER TABLE repositories ADD COLUMN html_url VARCHAR(300);
ALTER TABLE repositories ADD COLUMN primary_language VARCHAR(100);
ALTER TABLE repositories ADD COLUMN is_private BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE repositories ADD COLUMN pushed_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE repositories ADD COLUMN stars_count INTEGER DEFAULT 0;
ALTER TABLE repositories ADD COLUMN forks_count INTEGER DEFAULT 0;
ALTER TABLE repositories ADD COLUMN open_issues_count INTEGER DEFAULT 0;
