import React from 'react';
import { RepositoryContributorFileResponse } from '../../types/ownership';
import { Page } from '../../types/api';
import { Pagination } from '../common/Pagination';
import { LoadingSpinner } from '../common/LoadingSpinner';
import { formatDate, formatNumber } from '../../utils/formatters';

interface ContributorFilesTableProps {
  filesPage: Page<RepositoryContributorFileResponse> | null;
  isLoading: boolean;
  currentPage: number;
  onPageChange: (page: number) => void;
  sortField: string;
  sortDirection: 'asc' | 'desc';
  onSortChange: (field: string, direction: 'asc' | 'desc') => void;
}

export const ContributorFilesTable: React.FC<ContributorFilesTableProps> = ({
  filesPage,
  isLoading,
  currentPage,
  onPageChange,
  sortField,
  sortDirection,
  onSortChange,
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
    <div className="space-y-3">
      <div className="overflow-x-auto rounded-lg border border-slate-800 bg-slate-900/60 shadow">
        <table className="w-full text-left text-xs text-slate-300">
          <thead className="border-b border-slate-800 bg-slate-900/90 text-[11px] uppercase tracking-wider text-slate-400">
            <tr>
              <th
                className="cursor-pointer px-4 py-3 hover:text-slate-200"
                onClick={() => handleSort('filePath')}
              >
                File Path {renderSortIndicator('filePath')}
              </th>
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
                    <span>Loading contributed files...</span>
                  </div>
                </td>
              </tr>
            ) : !filesPage || filesPage.content.length === 0 ? (
              <tr>
                <td colSpan={7} className="py-8 text-center text-slate-500 italic">
                  No contributed files recorded for this contributor.
                </td>
              </tr>
            ) : (
              filesPage.content.map((item) => (
                <tr key={item.id} className="transition-colors hover:bg-slate-800/40">
                  <td className="px-4 py-3 font-mono text-slate-200 break-all">
                    {item.filePath}
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
        pageData={filesPage}
        currentPage={currentPage}
        onPageChange={onPageChange}
        itemLabel="contributed files"
        disabled={isLoading}
      />
    </div>
  );
};
