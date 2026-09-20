import React from 'react';
import { RepositoryResponse } from '../../types/repository';

interface RepositorySelectorProps {
  repositories: RepositoryResponse[];
  selectedId: number | null;
  onSelect: (id: number) => void;
  isLoading?: boolean;
}

export const RepositorySelector: React.FC<RepositorySelectorProps> = ({
  repositories,
  selectedId,
  onSelect,
  isLoading = false,
}) => {
  if (isLoading) {
    return (
      <div className="flex items-center space-x-2 text-sm text-slate-400">
        <svg
          className="h-4 w-4 animate-spin text-slate-400"
          xmlns="http://www.w3.org/2000/svg"
          fill="none"
          viewBox="0 0 24 24"
        >
          <circle
            className="opacity-25"
            cx="12"
            cy="12"
            r="10"
            stroke="currentColor"
            strokeWidth="4"
          />
          <path
            className="opacity-75"
            fill="currentColor"
            d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"
          />
        </svg>
        <span>Loading repositories...</span>
      </div>
    );
  }

  if (repositories.length === 0) {
    return (
      <span className="text-sm italic text-slate-400">No repositories registered</span>
    );
  }

  return (
    <div className="relative inline-flex items-center">
      <label htmlFor="repository-select" className="sr-only">
        Select Repository
      </label>
      <select
        id="repository-select"
        value={selectedId || ''}
        onChange={(e) => {
          const val = parseInt(e.target.value, 10);
          if (!Number.isNaN(val)) {
            onSelect(val);
          }
        }}
        className="block w-64 rounded-md border border-slate-700 bg-slate-800/90 py-1.5 pl-3 pr-8 text-sm font-medium text-slate-100 shadow-sm transition hover:border-slate-600 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500 sm:text-sm"
      >
        {repositories.map((repo) => (
          <option key={repo.id} value={repo.id} className="bg-slate-900 text-slate-100">
            {repo.fullName}
          </option>
        ))}
      </select>
    </div>
  );
};
