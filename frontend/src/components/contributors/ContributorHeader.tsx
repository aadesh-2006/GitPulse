import React from 'react';
import { RepositoryResponse } from '../../types/repository';

export type ContributorViewMode = 'contributors' | 'ownership';

interface ContributorHeaderProps {
  repository: RepositoryResponse;
  viewMode: ContributorViewMode;
  onViewModeChange: (mode: ContributorViewMode) => void;
  onRefresh: () => void;
  isLoading: boolean;
}

export const ContributorHeader: React.FC<ContributorHeaderProps> = ({
  repository,
  viewMode,
  onViewModeChange,
  onRefresh,
  isLoading,
}) => {
  return (
    <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between border-b border-slate-800 pb-5">
      <div>
        <div className="flex items-center space-x-3">
          <h2 className="text-xl font-bold tracking-tight text-slate-100 sm:text-2xl">
            Contributor & Ownership Intelligence
          </h2>
          <span className="rounded-full bg-slate-800 px-2.5 py-0.5 text-xs font-mono text-slate-300">
            {repository.owner}/{repository.name}
          </span>
        </div>
        <p className="mt-1 text-xs sm:text-sm text-slate-400">
          Analyze contributor commit activity, file contributions, and codebase ownership concentration.
        </p>
      </div>

      <div className="flex flex-wrap items-center gap-3">
        {/* View Switcher Tabs */}
        <div className="inline-flex rounded-lg border border-slate-800 bg-slate-900 p-1">
          <button
            type="button"
            onClick={() => onViewModeChange('contributors')}
            className={`rounded-md px-3 py-1.5 text-xs font-medium transition ${
              viewMode === 'contributors'
                ? 'bg-blue-600 text-white shadow-sm'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            Contributor Activity
          </button>
          <button
            type="button"
            onClick={() => onViewModeChange('ownership')}
            className={`rounded-md px-3 py-1.5 text-xs font-medium transition ${
              viewMode === 'ownership'
                ? 'bg-blue-600 text-white shadow-sm'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            File Ownership Concentration
          </button>
        </div>

        {/* Refresh Button */}
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
