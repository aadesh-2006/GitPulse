import { apiClient } from './client';
import { RepositoryResponse, CreateRepositoryRequest } from '../types/repository';

export const repositoryApi = {
  getAllRepositories(): Promise<RepositoryResponse[]> {
    return apiClient.get<RepositoryResponse[]>('/repositories');
  },

  getRepositoryById(id: number): Promise<RepositoryResponse> {
    return apiClient.get<RepositoryResponse>(`/repositories/${id}`);
  },

  createRepository(request: CreateRepositoryRequest): Promise<RepositoryResponse> {
    return apiClient.post<RepositoryResponse>('/repositories', request);
  },

  syncRepository(id: number): Promise<RepositoryResponse> {
    return apiClient.post<RepositoryResponse>(`/repositories/${id}/sync`);
  },
};
