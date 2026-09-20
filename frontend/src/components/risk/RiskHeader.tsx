import React from 'react';
import { RepositoryResponse } from '../../types/repository';

interface RiskHeaderProps {
  repository: RepositoryResponse;
  onRefresh: () => void;
  isLoading: boolean;
}

export const RiskHeader: React.FC<RiskHeaderProps> = ({
  repository,
  onRefresh,
  isLoading,
}) => {
  return (
    <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between border-b border-slate-800 pb-5">
      <div>
        <div className="flex items-center space-x-3">
          <h2 className="text-xl font-bold tracking-tight text-slate-100 sm:text-2xl">
            Repository Risk & Stability Signal
          </h2>
          <span className="rounded-full bg-slate-800 px-2.5 py-0.5 text-xs font-mono text-slate-300">
            {repository.owner}/{repository.name}
          </span>
        </div>
        <p className="mt-1 text-xs sm:text-sm text-slate-400">
          Deterministic multidimensional evaluation of codebase activity, recency, churn, and contributor concentration signals.
        </p>
      </div>

      <div className="flex items-center space-x-3">
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
