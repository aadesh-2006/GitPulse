import React from 'react';
import { RepositoryResponse } from '../../types/repository';
import { Button } from '../common/Button';

interface FileIntelligenceHeaderProps {
  repository: RepositoryResponse;
  onRefresh: () => void;
  isLoading: boolean;
}

export const FileIntelligenceHeader: React.FC<FileIntelligenceHeaderProps> = ({
  repository,
  onRefresh,
  isLoading,
}) => {
  return (
    <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between rounded-lg border border-slate-800 bg-slate-900/90 p-5 shadow-sm">
      <div className="space-y-1">
        <div className="flex items-center space-x-2">
          <h1 className="text-xl font-bold tracking-tight text-white font-mono sm:text-2xl">
            File Intelligence & Hotspots
          </h1>
          <span className="rounded bg-blue-950/80 px-2 py-0.5 text-xs font-semibold text-blue-400 border border-blue-800/60">
            P7.3
          </span>
        </div>
        <p className="text-xs text-slate-400 max-w-3xl">
          Files are ranked using the repository's deterministic hotspot and risk metrics for{' '}
          <strong className="text-slate-200 font-mono">{repository.fullName}</strong>. The composite score
          combines revision frequency, code churn, recency of modification, and contributor concentration into a normalized 0–1 signal.
        </p>
      </div>

      <div className="self-start md:self-auto">
        <Button
          variant="outline"
          size="sm"
          onClick={onRefresh}
          isLoading={isLoading}
          icon={
            <svg
              className="h-3.5 w-3.5"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth="2"
                d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"
              />
            </svg>
          }
        >
          Refresh
        </Button>
      </div>
    </div>
  );
};
