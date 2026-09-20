import React from 'react';
import { RepositoryResponse } from '../../types/repository';
import { AppHeader } from './AppHeader';

export type NavigationTab = 'overview' | 'evolution' | 'files' | 'commits' | 'contributors';

interface AppShellProps {
  repositories: RepositoryResponse[];
  selectedRepository: RepositoryResponse | null;
  selectedRepositoryId: number | null;
  onSelectRepository: (id: number) => void;
  isLoadingRepositories: boolean;
  activeTab: NavigationTab;
  onTabChange: (tab: NavigationTab) => void;
  children: React.ReactNode;
}

export const AppShell: React.FC<AppShellProps> = ({
  repositories,
  selectedRepository,
  selectedRepositoryId,
  onSelectRepository,
  isLoadingRepositories,
  activeTab,
  onTabChange,
  children,
}) => {
  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 antialiased selection:bg-blue-600 selection:text-white">
      <AppHeader
        repositories={repositories}
        selectedRepository={selectedRepository}
        selectedRepositoryId={selectedRepositoryId}
        onSelectRepository={onSelectRepository}
        isLoadingRepositories={isLoadingRepositories}
      />

      {/* Sub-navigation bar */}
      <div className="border-b border-slate-800/80 bg-slate-900/60 backdrop-blur">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <nav className="-mb-px flex space-x-6 overflow-x-auto py-2">
            <button
              onClick={() => onTabChange('overview')}
              className={`whitespace-nowrap border-b-2 py-2 px-1 text-xs sm:text-sm font-medium transition-colors ${
                activeTab === 'overview'
                  ? 'border-blue-500 text-blue-400 font-semibold'
                  : 'border-transparent text-slate-400 hover:border-slate-700 hover:text-slate-200'
              }`}
            >
              Overview
            </button>
            <button
              onClick={() => onTabChange('evolution')}
              className={`whitespace-nowrap border-b-2 py-2 px-1 text-xs sm:text-sm font-medium transition-colors ${
                activeTab === 'evolution'
                  ? 'border-blue-500 text-blue-400 font-semibold'
                  : 'border-transparent text-slate-400 hover:border-slate-700 hover:text-slate-200'
              }`}
            >
              Evolution
            </button>
            <button
              onClick={() => onTabChange('files')}
              className={`flex items-center space-x-1 whitespace-nowrap border-b-2 py-2 px-1 text-xs sm:text-sm font-medium transition-colors ${
                activeTab === 'files'
                  ? 'border-blue-500 text-blue-400 font-semibold'
                  : 'border-transparent text-slate-400 hover:border-slate-700 hover:text-slate-200'
              }`}
            >
              <span>Files & Hotspots</span>
              <span className="rounded bg-blue-950/80 text-blue-400 border border-blue-800/60 px-1.5 py-0.2 text-[9px] font-semibold">
                Active
              </span>
            </button>
            <button
              onClick={() => onTabChange('commits')}
              className={`flex items-center space-x-1 whitespace-nowrap border-b-2 py-2 px-1 text-xs sm:text-sm font-medium transition-colors ${
                activeTab === 'commits'
                  ? 'border-blue-500 text-blue-400 font-semibold'
                  : 'border-transparent text-slate-400 hover:border-slate-700 hover:text-slate-200'
              }`}
            >
              <span>Commits</span>
              <span className="rounded bg-blue-950/80 text-blue-400 border border-blue-800/60 px-1.5 py-0.2 text-[9px] font-semibold">
                Active
              </span>
            </button>
            <button
              onClick={() => onTabChange('contributors')}
              className={`flex items-center space-x-1 whitespace-nowrap border-b-2 py-2 px-1 text-xs sm:text-sm font-medium transition-colors ${
                activeTab === 'contributors'
                  ? 'border-blue-500 text-blue-400 font-semibold'
                  : 'border-transparent text-slate-400 hover:border-slate-700 hover:text-slate-200'
              }`}
            >
              <span>Contributors & Ownership</span>
              <span className="rounded bg-blue-950/80 text-blue-400 border border-blue-800/60 px-1.5 py-0.2 text-[9px] font-semibold">
                Active
              </span>
            </button>
          </nav>
        </div>
      </div>

      {/* Main Content Area */}
      <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        {children}
      </main>
    </div>
  );
};
