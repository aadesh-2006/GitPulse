import React from 'react';
import { RepositoryResponse } from '../../types/repository';

interface CommitHeaderProps {
  repository: RepositoryResponse;
  onRefresh: () => void;
  isLoading: boolean;
  onNavigateToEvolution?: () => void;
}

export const CommitHeader: React.FC<CommitHeaderProps> = ({
  repository,
  onRefresh,
  isLoading,
  onNavigateToEvolution,
}) => {
  return (
    <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between border-b border-slate-800 pb-5">
      <div>
        <div className="flex items-center space-x-3">
          <h2 className="text-xl font-bold tracking-tight text-slate-100 sm:text-2xl">
            Commit Intelligence
          </h2>
          <span className="rounded-full bg-slate-800 px-2.5 py-0.5 text-xs font-mono text-slate-300">
            {repository.owner}/{repository.name}
          </span>
        </div>
        <p className="mt-1 text-xs sm:text-sm text-slate-400">
          Trace engineering changes, authoritative commit classifications, additions, deletions, and file diffs.
        </p>
      </div>

      <div className="flex flex-wrap items-center gap-3">
        {onNavigateToEvolution && (
          <button
            type="button"
            onClick={onNavigateToEvolution}
            className="rounded-lg border border-slate-700 bg-slate-800/80 px-3 py-2 text-xs font-medium text-slate-300 transition hover:bg-slate-700 hover:text-white"
            title="View full repository aggregate evolution composition"
          >
            Evolution Aggregates ↗
          </button>
        )}

        <button
          type="button"
          onClick={onRefresh}
          disabled={isLoading}
          className="flex items-center space-x-1.5 rounded-lg border border-slate-700 bg-slate-800/80 px-3 py-2 text-xs font-medium text-slate-200 transition hover:bg-slate-700 disabled:cursor-not-allowed disabled:opacity-50"
        >
          <span className={isLoading ? 'animate-spin' : ''}>🔄</span>
          <span>Refresh</span>
        </button>
      </div>
    </div>
  );
};
