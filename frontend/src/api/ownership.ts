import { apiClient } from './client';
import { Page } from '../types/api';
import {
  RepositoryFileOwnershipResponse,
  RepositoryContributorFileResponse,
  OwnershipQueryParams,
} from '../types/ownership';

export const ownershipApi = {
  getFileOwnership(
    repositoryId: number,
    params?: OwnershipQueryParams
  ): Promise<Page<RepositoryFileOwnershipResponse>> {
    return apiClient.get<Page<RepositoryFileOwnershipResponse>>(
      `/repositories/${repositoryId}/file-ownership`,
      {
        params: params as Record<string, string | number | undefined>,
      }
    );
  },

  getContributorFiles(
    repositoryId: number,
    params?: OwnershipQueryParams
  ): Promise<Page<RepositoryContributorFileResponse>> {
    return apiClient.get<Page<RepositoryContributorFileResponse>>(
      `/repositories/${repositoryId}/contributor-files`,
      {
        params: params as Record<string, string | number | undefined>,
      }
    );
  },

  getContributorFilesByContributor(
    repositoryId: number,
    contributorId: number,
    params?: OwnershipQueryParams
  ): Promise<Page<RepositoryContributorFileResponse>> {
    return apiClient.get<Page<RepositoryContributorFileResponse>>(
      `/repositories/${repositoryId}/contributors/${contributorId}/files`,
      {
        params: params as Record<string, string | number | undefined>,
      }
    );
  },

  getContributorFilesByFilePath(
    repositoryId: number,
    params?: OwnershipQueryParams
  ): Promise<Page<RepositoryContributorFileResponse>> {
    return apiClient.get<Page<RepositoryContributorFileResponse>>(
      `/repositories/${repositoryId}/files/contributors`,
      {
        params: params as Record<string, string | number | undefined>,
      }
    );
  },
};
