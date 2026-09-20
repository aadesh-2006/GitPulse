import { apiClient } from './client';
import { Page } from '../types/api';
import { CommitResponse, CommitDetailResponse, CommitQueryParams } from '../types/commit';

export const commitApi = {
  getRepositoryCommits(
    repositoryId: number,
    params?: CommitQueryParams
  ): Promise<Page<CommitResponse>> {
    return apiClient.get<Page<CommitResponse>>(`/repositories/${repositoryId}/commits`, {
      params: params as Record<string, string | number | boolean | undefined>,
    });
  },

  getCommitDetail(repositoryId: number, commitId: number): Promise<CommitDetailResponse> {
    return apiClient.get<CommitDetailResponse>(`/repositories/${repositoryId}/commits/${commitId}`);
  },
};
