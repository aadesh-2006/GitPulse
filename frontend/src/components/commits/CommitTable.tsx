import React from 'react';
import { CommitResponse } from '../../types/commit';
import { Page } from '../../types/api';
import { CommitClassificationBadge } from './CommitClassificationBadge';
import { Pagination } from '../common/Pagination';
import { LoadingSpinner } from '../common/LoadingSpinner';
import { formatDate, formatNumber, formatRelativeTime } from '../../utils/formatters';

interface CommitTableProps {
  commitPage: Page<CommitResponse> | null;
  isLoading: boolean;
  currentPage: number;
  onPageChange: (page: number) => void;
  onSelectCommit: (commitId: number) => void;
  selectedCommitId: number | null;
}

export const CommitTable: React.FC<CommitTableProps> = ({
  commitPage,
  isLoading,
  currentPage,
  onPageChange,
  onSelectCommit,
  selectedCommitId,
}) => {
  const getPrimaryCommitLine = (message: string): string => {
    if (!message) return 'No commit message';
    const firstLine = message.split('\n')[0].trim();
    return firstLine || 'No commit message';
  };

  return (
    <div className="space-y-3">
      <div className="overflow-x-auto rounded-lg border border-slate-800 bg-slate-900/60 shadow">
        <table className="w-full text-left text-xs text-slate-300">
          <thead className="border-b border-slate-800 bg-slate-900/90 text-[11px] uppercase tracking-wider text-slate-400">
            <tr>
              <th className="px-4 py-3">Commit Message</th>
              <th className="px-4 py-3">SHA</th>
              <th className="px-4 py-3">Classification</th>
              <th className="px-4 py-3">Author</th>
              <th className="px-4 py-3">Committed At</th>
              <th className="px-4 py-3 text-right">Additions</th>
              <th className="px-4 py-3 text-right">Deletions</th>
              <th className="px-4 py-3 text-right">Total Changes</th>
              <th className="px-4 py-3 text-center">Action</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800/60 font-sans">
            {isLoading ? (
              <tr>
                <td colSpan={9} className="py-12 text-center text-slate-400">
                  <div className="flex flex-col items-center justify-center space-y-2">
                    <LoadingSpinner size="md" />
                    <span>Loading commits...</span>
                  </div>
                </td>
              </tr>
            ) : !commitPage || commitPage.content.length === 0 ? (
              <tr>
                <td colSpan={9} className="py-8 text-center text-slate-500 italic">
                  No commits found matching the filter criteria.
                </td>
              </tr>
            ) : (
              commitPage.content.map((commit) => {
                const isSelected = selectedCommitId === commit.id;
                const shortSha = commit.githubCommitSha ? commit.githubCommitSha.substring(0, 7) : '—';
                const authorDisplay = commit.authorName || commit.authorUsername || commit.authorEmail || 'Anonymous';
                const primaryLine = getPrimaryCommitLine(commit.message);

                return (
                  <tr
                    key={commit.id}
                    onClick={() => onSelectCommit(commit.id)}
                    className={`cursor-pointer transition-colors ${
                      isSelected
                        ? 'bg-blue-950/40 ring-1 ring-blue-500/30'
                        : 'hover:bg-slate-800/40'
                    }`}
                  >
                    <td className="px-4 py-3 max-w-xs sm:max-w-md">
                      <div
                        className="font-medium text-slate-100 truncate"
                        title={commit.message}
                      >
                        {primaryLine}
                      </div>
                    </td>
                    <td className="px-4 py-3 font-mono text-slate-300 whitespace-nowrap">
                      {commit.htmlUrl ? (
                        <a
                          href={commit.htmlUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          onClick={(e) => e.stopPropagation()}
                          className="text-blue-400 hover:text-blue-300 hover:underline"
                          title="View on GitHub"
                        >
                          {shortSha}
                        </a>
                      ) : (
                        <span>{shortSha}</span>
                      )}
                    </td>
                    <td className="px-4 py-3 whitespace-nowrap">
                      <CommitClassificationBadge classification={commit.classification} />
                    </td>
                    <td className="px-4 py-3">
                      <div className="font-medium text-slate-200 truncate max-w-[140px]">
                        {authorDisplay}
                      </div>
                      {commit.authorEmail && commit.authorEmail !== authorDisplay && (
                        <div className="text-[10px] font-mono text-slate-500 truncate max-w-[140px]">
                          {commit.authorEmail}
                        </div>
                      )}
                    </td>
                    <td className="px-4 py-3 whitespace-nowrap">
                      <div className="text-slate-300">{formatDate(commit.committedAt)}</div>
                      <div className="text-[10px] text-slate-500">{formatRelativeTime(commit.committedAt)}</div>
                    </td>
                    <td className="px-4 py-3 text-right font-mono text-emerald-400">
                      +{formatNumber(commit.additions)}
                    </td>
                    <td className="px-4 py-3 text-right font-mono text-rose-400">
                      -{formatNumber(commit.deletions)}
                    </td>
                    <td className="px-4 py-3 text-right font-mono font-semibold text-slate-100">
                      {formatNumber(commit.totalChanges)}
                    </td>
                    <td className="px-4 py-3 text-center">
                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation();
                          onSelectCommit(commit.id);
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
        pageData={commitPage}
        currentPage={currentPage}
        onPageChange={onPageChange}
        itemLabel="commits"
        disabled={isLoading}
      />
    </div>
  );
};
