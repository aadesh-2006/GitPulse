import { useState, useEffect, useCallback, useRef } from 'react';
import { evolutionApi } from '../api/evolution';
import {
  RepositoryEvolutionResponse,
  RepositoryEvolutionComparisonResponse,
  RepositoryEvolutionCompositionResponse,
} from '../types/evolution';
import { EvolutionDateRange, calculateDateRange } from '../utils/dateRanges';
import { ApiError } from '../api/client';

export interface EvolutionDataState {
  evolution: RepositoryEvolutionResponse | null;
  comparison: RepositoryEvolutionComparisonResponse | null;
  composition: RepositoryEvolutionCompositionResponse | null;
  dateRange: EvolutionDateRange;
  isLoading: boolean;
  error: string | null;
  setDateRange: (range: EvolutionDateRange) => void;
  refresh: () => Promise<void>;
}

export function useEvolutionData(repositoryId: number | null): EvolutionDataState {
  const [dateRange, setDateRange] = useState<EvolutionDateRange>(() =>
    calculateDateRange('6m')
  );
  const [evolution, setEvolution] = useState<RepositoryEvolutionResponse | null>(null);
  const [comparison, setComparison] = useState<RepositoryEvolutionComparisonResponse | null>(null);
  const [composition, setComposition] = useState<RepositoryEvolutionCompositionResponse | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const fetchIdRef = useRef<number>(0);

  const fetchData = useCallback(async () => {
    if (!repositoryId) {
      setEvolution(null);
      setComparison(null);
      setComposition(null);
      return;
    }

    const currentFetchId = ++fetchIdRef.current;
    setIsLoading(true);
    setError(null);

    try {
      const [evolutionRes, comparisonRes, compositionRes] = await Promise.all([
        evolutionApi.getRepositoryEvolution(repositoryId, {
          from: dateRange.currentFrom,
          to: dateRange.currentTo,
        }),
        evolutionApi.compareEvolution(repositoryId, {
          currentFrom: dateRange.currentFrom,
          currentTo: dateRange.currentTo,
          previousFrom: dateRange.previousFrom,
          previousTo: dateRange.previousTo,
        }),
        evolutionApi.getEvolutionComposition(repositoryId, {
          from: dateRange.currentFrom,
          to: dateRange.currentTo,
        }),
      ]);

      // Guard against race conditions if range/repo changed while requests were in-flight
      if (currentFetchId === fetchIdRef.current) {
        setEvolution(evolutionRes);
        setComparison(comparisonRes);
        setComposition(compositionRes);
      }
    } catch (err) {
      if (currentFetchId === fetchIdRef.current) {
        const message =
          err instanceof ApiError
            ? err.message
            : err instanceof Error
            ? err.message
            : 'Failed to load repository evolution data';
        setError(message);
      }
    } finally {
      if (currentFetchId === fetchIdRef.current) {
        setIsLoading(false);
      }
    }
  }, [repositoryId, dateRange]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  return {
    evolution,
    comparison,
    composition,
    dateRange,
    isLoading,
    error,
    setDateRange,
    refresh: fetchData,
  };
}
