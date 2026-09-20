import React from 'react';
import { RepositoryResponse } from '../../types/repository';
import { RepositorySelector } from '../repository/RepositorySelector';

interface AppHeaderProps {
  repositories: RepositoryResponse[];
  selectedRepository: RepositoryResponse | null;
  selectedRepositoryId: number | null;
  onSelectRepository: (id: number) => void;
  isLoadingRepositories: boolean;
}

export const AppHeader: React.FC<AppHeaderProps> = ({
  repositories,
  selectedRepository,
  selectedRepositoryId,
  onSelectRepository,
  isLoadingRepositories,
}) => {
  return (
    <header className="sticky top-0 z-30 border-b border-slate-800 bg-slate-900 text-white">
      <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
        <div className="flex items-center space-x-4">
          <div className="flex items-center space-x-2.5">
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-blue-600 text-white shadow-md">
              <svg
                className="h-5 w-5"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2.2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <path d="M15 6v12a3 3 0 1 0 3-3H6a3 3 0 1 0 3 3V6a3 3 0 1 0-3 3h12a3 3 0 1 0-3-3" />
              </svg>
            </div>
            <div>
              <span className="text-lg font-bold tracking-tight text-white font-mono">
                Git<span className="text-blue-400">Pulse</span>
              </span>
              <span className="ml-2 hidden rounded bg-slate-800 px-1.5 py-0.5 text-[10px] font-semibold uppercase tracking-wider text-slate-400 sm:inline-block">
                Analytics
              </span>
            </div>
          </div>

          <div className="hidden h-5 w-px bg-slate-800 md:block" />

          <div className="hidden items-center space-x-2 text-xs text-slate-400 md:flex">
            <span>Repository:</span>
            <RepositorySelector
              repositories={repositories}
              selectedId={selectedRepositoryId}
              onSelect={onSelectRepository}
              isLoading={isLoadingRepositories}
            />
          </div>
        </div>

        <div className="flex items-center space-x-3">
          {/* Mobile repo selector */}
          <div className="md:hidden">
            <RepositorySelector
              repositories={repositories}
              selectedId={selectedRepositoryId}
              onSelect={onSelectRepository}
              isLoading={isLoadingRepositories}
            />
          </div>

          {selectedRepository && (
            <div className="hidden items-center space-x-2 rounded-md bg-slate-800/80 px-3 py-1.5 text-xs text-slate-300 lg:flex border border-slate-700/50">
              <span className="inline-block h-2 w-2 rounded-full bg-emerald-400" />
              <span className="font-mono">{selectedRepository.defaultBranch}</span>
              {selectedRepository.primaryLanguage && (
                <span className="text-slate-400">({selectedRepository.primaryLanguage})</span>
              )}
            </div>
          )}
        </div>
      </div>
    </header>
  );
};
