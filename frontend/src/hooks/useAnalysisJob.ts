import { useState, useEffect, useCallback, useRef } from 'react';
import { analysisJobApi } from '../api/analysisJobs';
import { AnalysisJobResponse, AnalysisJobStatus } from '../types/analysisJob';
import { ApiError } from '../api/client';

const POLLING_INTERVAL_MS = 2500;

export interface UseAnalysisJobResult {
  jobs: AnalysisJobResponse[];
  latestJob: AnalysisJobResponse | null;
  isLoading: boolean;
  isTriggering: boolean;
  isPolling: boolean;
  error: string | null;
  triggerAnalysis: () => Promise<void>;
  refreshJobs: () => Promise<void>;
}

export function useAnalysisJob(repositoryId: number | null): UseAnalysisJobResult {
  const [jobs, setJobs] = useState<AnalysisJobResponse[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [isTriggering, setIsTriggering] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [isPolling, setIsPolling] = useState<boolean>(false);

  const pollTimerRef = useRef<number | null>(null);

  const clearPolling = useCallback(() => {
    if (pollTimerRef.current !== null) {
      window.clearTimeout(pollTimerRef.current);
      pollTimerRef.current = null;
    }
    setIsPolling(false);
  }, []);

  const fetchJobs = useCallback(async () => {
    if (!repositoryId) {
      setJobs([]);
      return;
    }
    try {
      const data = await analysisJobApi.getJobsByRepositoryId(repositoryId);
      setJobs(data);
      return data;
    } catch (err) {
      const message = err instanceof ApiError ? err.message : 'Failed to fetch analysis jobs';
      setError(message);
      return [];
    }
  }, [repositoryId]);

  // Initial load when repository changes
  useEffect(() => {
    clearPolling();
    setError(null);
    if (repositoryId) {
      setIsLoading(true);
      fetchJobs().finally(() => setIsLoading(false));
    } else {
      setJobs([]);
    }
    return () => clearPolling();
  }, [repositoryId, fetchJobs, clearPolling]);

  // Active status checker
  const isJobActive = (status: AnalysisJobStatus): boolean => {
    return status === 'PENDING' || status === 'RUNNING';
  };

  // Setup polling loop if latest job is active
  useEffect(() => {
    const latest = jobs.length > 0 ? jobs[0] : null;

    if (latest && isJobActive(latest.status)) {
      setIsPolling(true);
      pollTimerRef.current = window.setTimeout(async () => {
        const updatedJobs = await fetchJobs();
        const newLatest = updatedJobs && updatedJobs.length > 0 ? updatedJobs[0] : null;
        if (!newLatest || !isJobActive(newLatest.status)) {
          clearPolling();
        }
      }, POLLING_INTERVAL_MS);
    } else {
      clearPolling();
    }

    return () => {
      if (pollTimerRef.current !== null) {
        window.clearTimeout(pollTimerRef.current);
      }
    };
  }, [jobs, fetchJobs, clearPolling]);

  const triggerAnalysis = useCallback(async () => {
    if (!repositoryId || isTriggering) return;
    setIsTriggering(true);
    setError(null);
    try {
      const newJob = await analysisJobApi.createAnalysisJob(repositoryId);
      // Immediately prepend new job so UI updates instantly
      setJobs((prev) => [newJob, ...prev.filter((j) => j.id !== newJob.id)]);
    } catch (err) {
      const message = err instanceof ApiError ? err.message : 'Failed to trigger analysis job';
      setError(message);
    } finally {
      setIsTriggering(false);
    }
  }, [repositoryId, isTriggering]);

  const latestJob = jobs.length > 0 ? jobs[0] : null;

  return {
    jobs,
    latestJob,
    isLoading,
    isTriggering,
    isPolling,
    error,
    triggerAnalysis,
    refreshJobs: async () => {
      setIsLoading(true);
      await fetchJobs();
      setIsLoading(false);
    },
  };
}
