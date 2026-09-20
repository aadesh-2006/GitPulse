import React, { useState } from 'react';
import { RepositoryResponse } from '../../types/repository';
import { AppHeader } from './AppHeader';

interface AppShellProps {
  repositories: RepositoryResponse[];
  selectedRepository: RepositoryResponse | null;
  selectedRepositoryId: number | null;
  onSelectRepository: (id: number) => void;
  isLoadingRepositories: boolean;
  children: React.ReactNode;
}

export type NavigationTab = 'overview' | 'evolution' | 'files' | 'commits' | 'contributors';

export const AppShell: React.FC<AppShellProps> = ({
  repositories,
  selectedRepository,
  selectedRepositoryId,
  onSelectRepository,
  isLoadingRepositories,
  children,
}) => {
  const [activeTab, setActiveTab] = useState<NavigationTab>('overview');

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
              onClick={() => setActiveTab('overview')}
              className={`whitespace-nowrap border-b-2 py-2 px-1 text-xs sm:text-sm font-medium transition-colors ${
                activeTab === 'overview'
                  ? 'border-blue-500 text-blue-400 font-semibold'
                  : 'border-transparent text-slate-400 hover:border-slate-700 hover:text-slate-200'
              }`}
            >
              Overview
            </button>
            <button
              onClick={() => setActiveTab('overview')}
              className="group flex items-center space-x-1 whitespace-nowrap border-b-2 border-transparent py-2 px-1 text-xs sm:text-sm font-medium text-slate-500 cursor-default"
              title="Will be fully activated in upcoming P7 chunks"
            >
              <span>Evolution</span>
              <span className="rounded bg-slate-800 px-1.5 py-0.2 text-[9px] text-slate-400">P7.2</span>
            </button>
            <button
              onClick={() => setActiveTab('overview')}
              className="group flex items-center space-x-1 whitespace-nowrap border-b-2 border-transparent py-2 px-1 text-xs sm:text-sm font-medium text-slate-500 cursor-default"
              title="Will be fully activated in upcoming P7 chunks"
            >
              <span>Files & Hotspots</span>
              <span className="rounded bg-slate-800 px-1.5 py-0.2 text-[9px] text-slate-400">P7.3</span>
            </button>
            <button
              onClick={() => setActiveTab('overview')}
              className="group flex items-center space-x-1 whitespace-nowrap border-b-2 border-transparent py-2 px-1 text-xs sm:text-sm font-medium text-slate-500 cursor-default"
              title="Will be fully activated in upcoming P7 chunks"
            >
              <span>Commits</span>
              <span className="rounded bg-slate-800 px-1.5 py-0.2 text-[9px] text-slate-400">P7.4</span>
            </button>
            <button
              onClick={() => setActiveTab('overview')}
              className="group flex items-center space-x-1 whitespace-nowrap border-b-2 border-transparent py-2 px-1 text-xs sm:text-sm font-medium text-slate-500 cursor-default"
              title="Will be fully activated in upcoming P7 chunks"
            >
              <span>Contributors & Ownership</span>
              <span className="rounded bg-slate-800 px-1.5 py-0.2 text-[9px] text-slate-400">P7.5</span>
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
