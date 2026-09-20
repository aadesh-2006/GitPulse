export interface PrimaryContributorSummaryResponse {
  id: number;
  email: string;
  username: string | null;
  name: string | null;
  avatarUrl: string | null;
}

export interface RepositoryFileResponse {
  id: number;
  repositoryId: number;
  filePath: string;
  fileName: string;
  extension: string | null;
  directoryPath: string | null;
  totalRevisions: number;
  totalAdditions: number;
  totalDeletions: number;
  totalChurn: number;
  isDeleted: boolean;
  firstModifiedAt: string;
  lastModifiedAt: string;
  primaryContributor: PrimaryContributorSummaryResponse | null;
  baselineScore: number;
  revisionFrequencyScore: number;
  churnScore: number;
  recencyScore: number;
  ownershipConcentrationScore: number;
  compositeScore: number;
  createdAt: string;
  updatedAt: string;
}

export interface FileQueryParams {
  page?: number;
  size?: number;
  sort?: string;
  extension?: string;
  isDeleted?: boolean;
}
