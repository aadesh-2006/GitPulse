export type CommitClassification =
  | 'FEATURE'
  | 'BUG_FIX'
  | 'REFACTOR'
  | 'DOCUMENTATION'
  | 'TEST'
  | 'BUILD'
  | 'CONFIGURATION'
  | 'DEPENDENCY'
  | 'OTHER';

export type FileChangeStatus = 'ADDED' | 'MODIFIED' | 'REMOVED' | 'RENAMED' | 'UNKNOWN';

export interface CommitResponse {
  id: number;
  repositoryId: number | null;
  githubCommitSha: string;
  message: string;
  authorName: string | null;
  authorEmail: string | null;
  authorUsername: string | null;
  committedAt: string;
  additions: number | null;
  deletions: number | null;
  totalChanges: number | null;
  htmlUrl: string | null;
  classification: CommitClassification | null;
  createdAt: string;
}

export interface CommitFileChangeResponse {
  id: number;
  commitId: number | null;
  filePath: string;
  status: FileChangeStatus;
  additions: number | null;
  deletions: number | null;
  changes: number | null;
  blobUrl: string | null;
  rawUrl: string | null;
  createdAt: string;
}

export interface CommitDetailResponse {
  id: number;
  repositoryId: number | null;
  githubCommitSha: string;
  message: string;
  authorName: string | null;
  authorEmail: string | null;
  authorUsername: string | null;
  committedAt: string;
  additions: number | null;
  deletions: number | null;
  totalChanges: number | null;
  htmlUrl: string | null;
  classification: CommitClassification | null;
  createdAt: string;
  fileChanges: CommitFileChangeResponse[];
}

export interface CommitQueryParams {
  page?: number;
  size?: number;
  sort?: string;
  classification?: CommitClassification;
  authorEmail?: string;
  from?: string;
  to?: string;
}
