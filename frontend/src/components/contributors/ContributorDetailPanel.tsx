import React from 'react';
import { RepositoryContributorResponse } from '../../types/contributor';
import { RepositoryContributorFileResponse } from '../../types/ownership';
import { Page } from '../../types/api';
import { ContributorIdentity } from './ContributorIdentity';
import { ContributorFilesTable } from './ContributorFilesTable';
import { LoadingSpinner } from '../common/LoadingSpinner';
import { ErrorMessage } from '../common/ErrorMessage';
import { formatDate, formatNumber } from '../../utils/formatters';

interface ContributorDetailPanelProps {
  contributorSummary: RepositoryContributorResponse | null;
  filesPage: Page<RepositoryContributorFileResponse> | null;
  isLoadingSummary: boolean;
  isLoadingFiles: boolean;
  error: string | null;
  onClose: () => void;
  filesPageNumber: number;
  onFilesPageChange: (page: number) => void;
  sortField: string;
  sortDirection: 'asc' | 'desc';
  onSortChange: (field: string, direction: 'asc' | 'desc') => void;
  onRetry?: () => void;
}

export const ContributorDetailPanel: React.FC<ContributorDetailPanelProps> = ({
  contributorSummary,
  filesPage,
  isLoadingSummary,
  isLoadingFiles,
  error,
  onClose,
  filesPageNumber,
  onFilesPageChange,
  sortField,
  sortDirection,
  onSortChange,
  onRetry,
}) => {
  return (
    <div className="rounded-xl border border-slate-700/80 bg-slate-900/95 p-6 shadow-2xl backdrop-blur">
      <div className="flex items-start justify-between border-b border-slate-800 pb-4">
        <div className="flex items-center space-x-4">
          {isLoadingSummary ? (
            <div className="flex items-center space-x-3">
              <LoadingSpinner size="sm" />
              <span className="text-sm text-slate-400">Loading contributor profile...</span>
            </div>
          ) : contributorSummary ? (
            <ContributorIdentity
              contributor={contributorSummary.contributor}
              size="lg"
            />
          ) : (
            <h3 className="text-lg font-semibold text-slate-100">Contributor Details</h3>
          )}
        </div>

        <button
          type="button"
          onClick={onClose}
          className="rounded-lg border border-slate-700 bg-slate-800/80 px-3 py-1.5 text-xs font-medium text-slate-300 transition hover:bg-slate-700 hover:text-white"
        >
          ✕ Close
        </button>
      </div>

      {error && (
        <div className="mt-4">
          <ErrorMessage
            title="Unable to load contributor details"
            message={error}
            onRetry={onRetry}
          />
        </div>
      )}

      {contributorSummary && (
        <div className="mt-6 space-y-6">
          {/* Summary Metric Cards */}
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6">
            <div className="rounded-lg border border-slate-800 bg-slate-950/60 p-3">
              <div className="text-[11px] font-medium uppercase text-slate-400">Total Commits</div>
              <div className="mt-1 font-mono text-lg font-bold text-blue-400">
                {formatNumber(contributorSummary.totalCommits)}
              </div>
            </div>

            <div className="rounded-lg border border-slate-800 bg-slate-950/60 p-3">
              <div className="text-[11px] font-medium uppercase text-slate-400">Additions</div>
              <div className="mt-1 font-mono text-lg font-bold text-emerald-400">
                +{formatNumber(contributorSummary.totalAdditions)}
              </div>
            </div>

            <div className="rounded-lg border border-slate-800 bg-slate-950/60 p-3">
              <div className="text-[11px] font-medium uppercase text-slate-400">Deletions</div>
              <div className="mt-1 font-mono text-lg font-bold text-rose-400">
                -{formatNumber(contributorSummary.totalDeletions)}
              </div>
            </div>

            <div className="rounded-lg border border-slate-800 bg-slate-950/60 p-3">
              <div className="text-[11px] font-medium uppercase text-slate-400">Total Changes</div>
              <div className="mt-1 font-mono text-lg font-bold text-slate-100">
                {formatNumber(contributorSummary.totalChanges)}
              </div>
            </div>

            <div className="rounded-lg border border-slate-800 bg-slate-950/60 p-3">
              <div className="text-[11px] font-medium uppercase text-slate-400">First Committed</div>
              <div className="mt-1 text-xs text-slate-300">
                {formatDate(contributorSummary.firstCommittedAt)}
              </div>
            </div>

            <div className="rounded-lg border border-slate-800 bg-slate-950/60 p-3">
              <div className="text-[11px] font-medium uppercase text-slate-400">Last Committed</div>
              <div className="mt-1 text-xs text-slate-300">
                {formatDate(contributorSummary.lastCommittedAt)}
              </div>
            </div>
          </div>

          {/* Contributed Files Table */}
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <h4 className="text-sm font-semibold text-slate-200">
                Contributed Repository Files
              </h4>
              <span className="text-xs text-slate-400">
                File-level contribution metrics & churn
              </span>
            </div>

            <ContributorFilesTable
              filesPage={filesPage}
              isLoading={isLoadingFiles}
              currentPage={filesPageNumber}
              onPageChange={onFilesPageChange}
              sortField={sortField}
              sortDirection={sortDirection}
              onSortChange={onSortChange}
            />
          </div>
        </div>
      )}
    </div>
  );
};
