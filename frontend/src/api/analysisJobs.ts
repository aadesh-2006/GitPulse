import { apiClient } from './client';
import { AnalysisJobResponse } from '../types/analysisJob';

export const analysisJobApi = {
  createAnalysisJob(repositoryId: number): Promise<AnalysisJobResponse> {
    return apiClient.post<AnalysisJobResponse>(`/repositories/${repositoryId}/analysis-jobs`);
  },

  getAnalysisJobById(jobId: number): Promise<AnalysisJobResponse> {
    return apiClient.get<AnalysisJobResponse>(`/analysis-jobs/${jobId}`);
  },

  getJobsByRepositoryId(repositoryId: number): Promise<AnalysisJobResponse[]> {
    return apiClient.get<AnalysisJobResponse[]>(`/repositories/${repositoryId}/analysis-jobs`);
  },
};
