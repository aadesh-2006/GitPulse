import { useState, useEffect, useCallback } from 'react';
import { repositoryApi } from '../api/repositories';
import { RepositoryResponse } from '../types/repository';
import { storage } from '../utils/storage';
import { ApiError } from '../api/client';

export interface UseRepositoriesResult {
  repositories: RepositoryResponse[];
  selectedRepository: RepositoryResponse | null;
  selectedRepositoryId: number | null;
  isLoading: boolean;
  error: string | null;
  selectRepository: (id: number) => void;
  refreshRepositories: () => Promise<void>;
}

export function useRepositories(): UseRepositoriesResult {
  const [repositories, setRepositories] = useState<RepositoryResponse[]>([]);
  const [selectedRepositoryId, setSelectedRepositoryId] = useState<number | null>(() =>
    storage.getSelectedRepositoryId()
  );
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const fetchRepositories = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await repositoryApi.getAllRepositories();
      setRepositories(data);

      // Auto-select or validate stored selection
      if (data.length > 0) {
        const storedId = storage.getSelectedRepositoryId();
        const exists = data.some((r) => r.id === storedId);
        const targetId = exists && storedId !== null ? storedId : data[0].id;
        setSelectedRepositoryId(targetId);
        storage.setSelectedRepositoryId(targetId);
      } else {
        setSelectedRepositoryId(null);
        storage.setSelectedRepositoryId(null);
      }
    } catch (err) {
      const message = err instanceof ApiError ? err.message : 'Failed to fetch repositories';
      setError(message);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchRepositories();
  }, [fetchRepositories]);

  const selectRepository = useCallback((id: number) => {
    setSelectedRepositoryId(id);
    storage.setSelectedRepositoryId(id);
  }, []);

  const selectedRepository =
    repositories.find((r) => r.id === selectedRepositoryId) || null;

  return {
    repositories,
    selectedRepository,
    selectedRepositoryId,
    isLoading,
    error,
    selectRepository,
    refreshRepositories: fetchRepositories,
  };
}
