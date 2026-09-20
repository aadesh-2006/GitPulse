export interface RepositoryResponse {
  id: number;
  owner: string;
  name: string;
  fullName: string;
  description: string | null;
  defaultBranch: string;
  githubId: number | null;
  htmlUrl: string | null;
  primaryLanguage: string | null;
  private: boolean;
  pushedAt: string | null;
  starsCount: number | null;
  forksCount: number | null;
  openIssuesCount: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateRepositoryRequest {
  owner: string;
  name: string;
}
