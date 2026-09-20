export interface ContributorResponse {
  id: number;
  email: string;
  username: string | null;
  name: string | null;
  avatarUrl: string | null;
  githubId: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface RepositoryContributorResponse {
  id: number;
  repositoryId: number;
  contributor: ContributorResponse;
  totalCommits: number;
  totalAdditions: number;
  totalDeletions: number;
  totalChanges: number;
  firstCommittedAt: string;
  lastCommittedAt: string;
  createdAt: string;
  updatedAt: string;
}

export interface ContributorQueryParams {
  page?: number;
  size?: number;
  sort?: string;
}
