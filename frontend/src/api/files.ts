import { apiClient } from './client';
import { Page } from '../types/api';
import { RepositoryFileResponse, FileQueryParams } from '../types/file';

export const fileApi = {
  getRepositoryFiles(
    repositoryId: number,
    params?: FileQueryParams
  ): Promise<Page<RepositoryFileResponse>> {
    return apiClient.get<Page<RepositoryFileResponse>>(`/repositories/${repositoryId}/files`, {
      params: params as Record<string, string | number | boolean | undefined>,
    });
  },

  getRepositoryHotspots(
    repositoryId: number,
    params?: FileQueryParams
  ): Promise<Page<RepositoryFileResponse>> {
    return apiClient.get<Page<RepositoryFileResponse>>(
      `/repositories/${repositoryId}/files/hotspots`,
      {
        params: params as Record<string, string | number | boolean | undefined>,
      }
    );
  },

  getRepositoryFileByPath(
    repositoryId: number,
    filePath: string
  ): Promise<RepositoryFileResponse> {
    const cleanPath = filePath.startsWith('/') ? filePath.substring(1) : filePath;
    const encodedPath = cleanPath
      .split('/')
      .map((segment) => encodeURIComponent(segment))
      .join('/');
    return apiClient.get<RepositoryFileResponse>(
      `/repositories/${repositoryId}/files/${encodedPath}`
    );
  },
};
