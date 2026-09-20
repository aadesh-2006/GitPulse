import { apiClient } from './client';
import {
  RepositoryEvolutionResponse,
  RepositoryEvolutionComparisonResponse,
  RepositoryEvolutionCompositionResponse,
} from '../types/evolution';

export interface EvolutionQueryParams {
  from?: string;
  to?: string;
}

export interface EvolutionCompareQueryParams {
  currentFrom?: string;
  currentTo?: string;
  previousFrom?: string;
  previousTo?: string;
}

export const evolutionApi = {
  getRepositoryEvolution(
    repositoryId: number,
    params?: EvolutionQueryParams
  ): Promise<RepositoryEvolutionResponse> {
    return apiClient.get<RepositoryEvolutionResponse>(
      `/repositories/${repositoryId}/evolution`,
      { params: params as Record<string, string | undefined> }
    );
  },

  compareEvolution(
    repositoryId: number,
    params?: EvolutionCompareQueryParams
  ): Promise<RepositoryEvolutionComparisonResponse> {
    return apiClient.get<RepositoryEvolutionComparisonResponse>(
      `/repositories/${repositoryId}/evolution/compare`,
      { params: params as Record<string, string | undefined> }
    );
  },

  getEvolutionComposition(
    repositoryId: number,
    params?: EvolutionQueryParams
  ): Promise<RepositoryEvolutionCompositionResponse> {
    return apiClient.get<RepositoryEvolutionCompositionResponse>(
      `/repositories/${repositoryId}/evolution/composition`,
      { params: params as Record<string, string | undefined> }
    );
  },
};
