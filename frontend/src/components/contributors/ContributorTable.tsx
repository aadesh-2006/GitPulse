import React from 'react';
import { RepositoryContributorResponse } from '../../types/contributor';
import { Page } from '../../types/api';
import { ContributorIdentity } from './ContributorIdentity';
import { Pagination } from '../common/Pagination';
import { LoadingSpinner } from '../common/LoadingSpinner';
import { formatDate, formatNumber } from '../../utils/formatters';

interface ContributorTableProps {
  contributorsPage: Page<RepositoryContributorResponse> | null;
  isLoading: boolean;
  currentPage: number;
  onPageChange: (page: number) => void;
  sortField: string;
  sortDirection: 'asc' | 'desc';
  onSortChange: (field: string, direction: 'asc' | 'desc') => void;
  onSelectContributor: (contributorId: number) => void;
  selectedContributorId: number | null;
}

export const ContributorTable: React.FC<ContributorTableProps> = ({
  contributorsPage,
  isLoading,
  currentPage,
  onPageChange,
  sortField,
  sortDirection,
  onSortChange,
  onSelectContributor,
  selectedContributorId,
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
              <th className="px-4 py-3">Contributor</th>
              <th
                className="cursor-pointer px-4 py-3 text-right hover:text-slate-200"
                onClick={() => handleSort('totalCommits')}
              >
                Commits {renderSortIndicator('totalCommits')}
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
                onClick={() => handleSort('totalChanges')}
              >
                Total Changes {renderSortIndicator('totalChanges')}
              </th>
              <th
                className="cursor-pointer px-4 py-3 text-right hover:text-slate-200"
                onClick={() => handleSort('firstCommittedAt')}
              >
                First Committed {renderSortIndicator('firstCommittedAt')}
              </th>
              <th
                className="cursor-pointer px-4 py-3 text-right hover:text-slate-200"
                onClick={() => handleSort('lastCommittedAt')}
              >
                Last Committed {renderSortIndicator('lastCommittedAt')}
              </th>
              <th className="px-4 py-3 text-center">Action</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800/60 font-sans">
            {isLoading ? (
              <tr>
                <td colSpan={8} className="py-12 text-center text-slate-400">
                  <div className="flex flex-col items-center justify-center space-y-2">
                    <LoadingSpinner size="md" />
                    <span>Loading contributors...</span>
                  </div>
                </td>
              </tr>
            ) : !contributorsPage || contributorsPage.content.length === 0 ? (
              <tr>
                <td colSpan={8} className="py-8 text-center text-slate-500 italic">
                  No contributors found for this repository.
                </td>
              </tr>
            ) : (
              contributorsPage.content.map((item) => {
                const isSelected = selectedContributorId === item.contributor.id;
                return (
                  <tr
                    key={item.id}
                    onClick={() => onSelectContributor(item.contributor.id)}
                    className={`cursor-pointer transition-colors ${
                      isSelected
                        ? 'bg-blue-950/40 ring-1 ring-blue-500/30'
                        : 'hover:bg-slate-800/40'
                    }`}
                  >
                    <td className="px-4 py-3">
                      <ContributorIdentity contributor={item.contributor} />
                    </td>
                    <td className="px-4 py-3 text-right font-mono font-semibold text-blue-400">
                      {formatNumber(item.totalCommits)}
                    </td>
                    <td className="px-4 py-3 text-right font-mono text-emerald-400">
                      +{formatNumber(item.totalAdditions)}
                    </td>
                    <td className="px-4 py-3 text-right font-mono text-rose-400">
                      -{formatNumber(item.totalDeletions)}
                    </td>
                    <td className="px-4 py-3 text-right font-mono font-semibold text-slate-100">
                      {formatNumber(item.totalChanges)}
                    </td>
                    <td className="px-4 py-3 text-right text-slate-400 whitespace-nowrap">
                      {formatDate(item.firstCommittedAt)}
                    </td>
                    <td className="px-4 py-3 text-right text-slate-400 whitespace-nowrap">
                      {formatDate(item.lastCommittedAt)}
                    </td>
                    <td className="px-4 py-3 text-center">
                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation();
                          onSelectContributor(item.contributor.id);
                        }}
                        className={`rounded px-2.5 py-1 text-xs font-medium transition ${
                          isSelected
                            ? 'bg-blue-600 text-white'
                            : 'border border-slate-700 bg-slate-800 text-slate-300 hover:bg-slate-700 hover:text-white'
                        }`}
                      >
                        {isSelected ? 'Viewing' : 'Inspect'}
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
        pageData={contributorsPage}
        currentPage={currentPage}
        onPageChange={onPageChange}
        itemLabel="contributors"
        disabled={isLoading}
      />
    </div>
  );
};
