export interface ContributorSummary {
  id: number;
  email: string;
  username: string | null;
  name: string | null;
  avatarUrl: string | null;
}

export interface RepositoryContributorFileResponse {
  id: number;
  repositoryId: number | null;
  contributor: ContributorSummary;
  filePath: string;
  totalRevisions: number;
  totalAdditions: number;
  totalDeletions: number;
  totalChurn: number;
  firstContributedAt: string;
  lastContributedAt: string;
  createdAt: string;
  updatedAt: string;
}

export interface RepositoryFileOwnershipResponse {
  repositoryId: number;
  filePath: string;
  contributorCount: number;
  totalRevisionsAcrossContributors: number;
  topContributor: ContributorSummary | null;
  topContributorRevisionShare: number | null;
}

export interface OwnershipQueryParams {
  page?: number;
  size?: number;
  sort?: string;
  filePath?: string;
}
