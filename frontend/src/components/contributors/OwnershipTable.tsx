import React from 'react';
import { RepositoryFileOwnershipResponse } from '../../types/ownership';
import { Page } from '../../types/api';
import { ContributorIdentity } from './ContributorIdentity';
import { OwnershipShareBar } from './OwnershipShareBar';
import { Pagination } from '../common/Pagination';
import { LoadingSpinner } from '../common/LoadingSpinner';
import { formatNumber } from '../../utils/formatters';

interface OwnershipTableProps {
  ownershipPage: Page<RepositoryFileOwnershipResponse> | null;
  isLoading: boolean;
  currentPage: number;
  onPageChange: (page: number) => void;
  sortField: string;
  sortDirection: 'asc' | 'desc';
  onSortChange: (field: string, direction: 'asc' | 'desc') => void;
  onSelectFile: (filePath: string) => void;
  selectedFilePath: string | null;
}

export const OwnershipTable: React.FC<OwnershipTableProps> = ({
  ownershipPage,
  isLoading,
  currentPage,
  onPageChange,
  sortField,
  sortDirection,
  onSortChange,
  onSelectFile,
  selectedFilePath,
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
                onClick={() => handleSort('contributorCount')}
              >
                Contributors {renderSortIndicator('contributorCount')}
              </th>
              <th
                className="cursor-pointer px-4 py-3 text-right hover:text-slate-200"
                onClick={() => handleSort('totalRevisionsAcrossContributors')}
              >
                Total Revisions {renderSortIndicator('totalRevisionsAcrossContributors')}
              </th>
              <th className="px-4 py-3">Top Contributor</th>
              <th
                className="cursor-pointer px-4 py-3 hover:text-slate-200"
                onClick={() => handleSort('topContributorRevisionShare')}
              >
                Top Contributor Share {renderSortIndicator('topContributorRevisionShare')}
              </th>
              <th className="px-4 py-3 text-center">Action</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800/60 font-sans">
            {isLoading ? (
              <tr>
                <td colSpan={6} className="py-12 text-center text-slate-400">
                  <div className="flex flex-col items-center justify-center space-y-2">
                    <LoadingSpinner size="md" />
                    <span>Loading file ownership concentration...</span>
                  </div>
                </td>
              </tr>
            ) : !ownershipPage || ownershipPage.content.length === 0 ? (
              <tr>
                <td colSpan={6} className="py-8 text-center text-slate-500 italic">
                  No file ownership data available for this repository.
                </td>
              </tr>
            ) : (
              ownershipPage.content.map((item) => {
                const isSelected = selectedFilePath === item.filePath;
                return (
                  <tr
                    key={item.filePath}
                    onClick={() => onSelectFile(item.filePath)}
                    className={`cursor-pointer transition-colors ${
                      isSelected
                        ? 'bg-blue-950/40 ring-1 ring-blue-500/30'
                        : 'hover:bg-slate-800/40'
                    }`}
                  >
                    <td className="px-4 py-3 font-mono text-slate-200 break-all">
                      {item.filePath}
                    </td>
                    <td className="px-4 py-3 text-right font-mono font-medium text-slate-200">
                      {formatNumber(item.contributorCount)}
                    </td>
                    <td className="px-4 py-3 text-right font-mono font-medium text-slate-200">
                      {formatNumber(item.totalRevisionsAcrossContributors)}
                    </td>
                    <td className="px-4 py-3">
                      <ContributorIdentity
                        contributor={item.topContributor}
                        showEmail={false}
                        size="sm"
                      />
                    </td>
                    <td className="px-4 py-3">
                      <OwnershipShareBar share={item.topContributorRevisionShare} />
                    </td>
                    <td className="px-4 py-3 text-center">
                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation();
                          onSelectFile(item.filePath);
                        }}
                        className={`rounded px-2.5 py-1 text-xs font-medium transition ${
                          isSelected
                            ? 'bg-blue-600 text-white'
                            : 'border border-slate-700 bg-slate-800 text-slate-300 hover:bg-slate-700 hover:text-white'
                        }`}
                      >
                        {isSelected ? 'Viewing' : 'Contributors'}
                      </button>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      <Pagination
        pageData={ownershipPage}
        currentPage={currentPage}
        onPageChange={onPageChange}
        itemLabel="files"
        disabled={isLoading}
      />
    </div>
  );
};
