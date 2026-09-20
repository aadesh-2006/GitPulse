import React from 'react';
import { RepositoryContributorFileResponse } from '../../types/ownership';
import { Page } from '../../types/api';
import { ContributorIdentity } from './ContributorIdentity';
import { Pagination } from '../common/Pagination';
import { LoadingSpinner } from '../common/LoadingSpinner';
import { ErrorMessage } from '../common/ErrorMessage';
import { formatDate, formatNumber } from '../../utils/formatters';

interface FileContributorsPanelProps {
  filePath: string;
  contributorsPage: Page<RepositoryContributorFileResponse> | null;
  isLoading: boolean;
  error: string | null;
  onClose: () => void;
  currentPage: number;
  onPageChange: (page: number) => void;
  sortField: string;
  sortDirection: 'asc' | 'desc';
  onSortChange: (field: string, direction: 'asc' | 'desc') => void;
  onRetry?: () => void;
}

export const FileContributorsPanel: React.FC<FileContributorsPanelProps> = ({
  filePath,
  contributorsPage,
  isLoading,
  error,
  onClose,
  currentPage,
  onPageChange,
  sortField,
  sortDirection,
  onSortChange,
  onRetry,
}) => {
  const handleSort = (field: string) => {
    if (sortField === field) {
      onSortChange(field, sortDirection === 'asc' ? 'desc' : 'asc');
    } else {
      onSortChange(field, 'desc');
    }
  };

  const renderSortIndicator = (field: string) => {
    if (sortField !== field) {
      return <span className="ml-1 text-slate-600">↕</span>;
    }
    return <span className="ml-1 text-blue-400">{sortDirection === 'asc' ? '↑' : '↓'}</span>;
  };

  return (
    <div className="rounded-xl border border-slate-700/80 bg-slate-900/95 p-6 shadow-2xl backdrop-blur">
      <div className="flex items-start justify-between border-b border-slate-800 pb-4">
        <div className="space-y-1">
          <div className="flex items-center space-x-2">
            <span className="rounded bg-blue-950 px-2 py-0.5 text-[11px] font-semibold text-blue-400 border border-blue-800/50">
              File Contributors
            </span>
            <h3 className="font-mono text-sm font-semibold text-slate-100 break-all">
              {filePath}
            </h3>
          </div>
          <p className="text-xs text-slate-400">
            Contributors and activity breakdown for this specific file.
          </p>
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
            title="Unable to load file contributors"
            message={error}
            onRetry={onRetry}
          />
        </div>
      )}

      <div className="mt-6 space-y-3">
        <div className="overflow-x-auto rounded-lg border border-slate-800 bg-slate-900/60 shadow">
          <table className="w-full text-left text-xs text-slate-300">
            <thead className="border-b border-slate-800 bg-slate-900/90 text-[11px] uppercase tracking-wider text-slate-400">
              <tr>
                <th className="px-4 py-3">Contributor</th>
                <th
                  className="cursor-pointer px-4 py-3 text-right hover:text-slate-200"
                  onClick={() => handleSort('totalRevisions')}
                >
                  Revisions {renderSortIndicator('totalRevisions')}
                </th>
                <th
                  className="cursor-pointer px-4 py-3 text-right hover:text-slate-200"
                  onClick={() => handleSort('totalAdditions')}
                >
                  Additions {renderSortIndicator('totalAdditions')}
                </th>
                <th
                  className="cursor-pointer px-4 py-3 text-right hover:text-slate-200"
                  onClick={() => handleSort('totalDeletions')}
                >
                  Deletions {renderSortIndicator('totalDeletions')}
                </th>
                <th
                  className="cursor-pointer px-4 py-3 text-right hover:text-slate-200"
                  onClick={() => handleSort('totalChurn')}
                >
                  Total Churn {renderSortIndicator('totalChurn')}
                </th>
                <th
                  className="cursor-pointer px-4 py-3 text-right hover:text-slate-200"
                  onClick={() => handleSort('firstContributedAt')}
                >
                  First Contributed {renderSortIndicator('firstContributedAt')}
                </th>
                <th
                  className="cursor-pointer px-4 py-3 text-right hover:text-slate-200"
                  onClick={() => handleSort('lastContributedAt')}
                >
                  Last Contributed {renderSortIndicator('lastContributedAt')}
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60 font-sans">
              {isLoading ? (
                <tr>
                  <td colSpan={7} className="py-12 text-center text-slate-400">
                    <div className="flex flex-col items-center justify-center space-y-2">
                      <LoadingSpinner size="md" />
                      <span>Loading file contributors...</span>
                    </div>
                  </td>
                </tr>
              ) : !contributorsPage || contributorsPage.content.length === 0 ? (
                <tr>
                  <td colSpan={7} className="py-8 text-center text-slate-500 italic">
                    No contributors found for this file.
                  </td>
                </tr>
              ) : (
                contributorsPage.content.map((item) => (
                  <tr key={item.id} className="transition-colors hover:bg-slate-800/40">
                    <td className="px-4 py-3">
                      <ContributorIdentity contributor={item.contributor} />
                    </td>
                    <td className="px-4 py-3 text-right font-mono font-medium text-slate-200">
                      {formatNumber(item.totalRevisions)}
                    </td>
                    <td className="px-4 py-3 text-right font-mono text-emerald-400">
                      +{formatNumber(item.totalAdditions)}
                    </td>
                    <td className="px-4 py-3 text-right font-mono text-rose-400">
                      -{formatNumber(item.totalDeletions)}
                    </td>
                    <td className="px-4 py-3 text-right font-mono font-semibold text-slate-100">
                      {formatNumber(item.totalChurn)}
                    </td>
                    <td className="px-4 py-3 text-right text-slate-400 whitespace-nowrap">
                      {formatDate(item.firstContributedAt)}
                    </td>
                    <td className="px-4 py-3 text-right text-slate-400 whitespace-nowrap">
                      {formatDate(item.lastContributedAt)}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        <Pagination
          pageData={contributorsPage}
          currentPage={currentPage}
          onPageChange={onPageChange}
          itemLabel="contributors"
          disabled={isLoading}
        />
      </div>
    </div>
  );
};
