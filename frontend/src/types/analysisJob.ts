export type AnalysisJobStatus = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED';

export interface AnalysisJobResponse {
  id: number;
  repositoryId: number | null;
  repositoryFullName: string | null;
  status: AnalysisJobStatus;
  startedAt: string | null;
  completedAt: string | null;
  errorMessage: string | null;
  createdAt: string;
  updatedAt: string;
}
