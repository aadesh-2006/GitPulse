import { apiClient } from './client';
import { Page } from '../types/api';
import {
  ContributorResponse,
  RepositoryContributorResponse,
  ContributorQueryParams,
} from '../types/contributor';

export const contributorApi = {
  getRepositoryContributors(
    repositoryId: number,
    params?: ContributorQueryParams
  ): Promise<Page<RepositoryContributorResponse>> {
    return apiClient.get<Page<RepositoryContributorResponse>>(
      `/repositories/${repositoryId}/contributors`,
      {
        params: params as Record<string, string | number | undefined>,
      }
    );
  },

  getContributorById(contributorId: number): Promise<ContributorResponse> {
    return apiClient.get<ContributorResponse>(`/contributors/${contributorId}`);
  },

  getRepositoryContributor(
    repositoryId: number,
    contributorId: number
  ): Promise<RepositoryContributorResponse> {
    return apiClient.get<RepositoryContributorResponse>(
      `/repositories/${repositoryId}/contributors/${contributorId}`
    );
  },
};
