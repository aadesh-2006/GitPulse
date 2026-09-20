import React, { useState } from 'react';
import { CommitDetailResponse } from '../../types/commit';
import { CommitClassificationBadge } from './CommitClassificationBadge';
import { CommitFileChangesTable } from './CommitFileChangesTable';
import { LoadingSpinner } from '../common/LoadingSpinner';
import { ErrorMessage } from '../common/ErrorMessage';
import { formatDate, formatNumber, formatRelativeTime } from '../../utils/formatters';

interface CommitDetailPanelProps {
  commit: CommitDetailResponse | null;
  isLoading: boolean;
  error: string | null;
  onClose: () => void;
  onSelectFile?: (filePath: string) => void;
  onRetry?: () => void;
}

export const CommitDetailPanel: React.FC<CommitDetailPanelProps> = ({
  commit,
  isLoading,
  error,
  onClose,
  onSelectFile,
  onRetry,
}) => {
  const [copiedSha, setCopiedSha] = useState<boolean>(false);

  const handleCopySha = (sha: string) => {
    navigator.clipboard.writeText(sha);
    setCopiedSha(true);
    setTimeout(() => setCopiedSha(false), 2000);
  };

  return (
    <div className="rounded-xl border border-slate-700/80 bg-slate-900/95 p-6 shadow-2xl backdrop-blur">
      <div className="flex items-start justify-between border-b border-slate-800 pb-4">
        <div className="space-y-1">
          <div className="flex items-center space-x-3">
            <span className="text-xs font-semibold uppercase tracking-wider text-blue-400">
              Commit Detail
            </span>
            {commit && (
              <CommitClassificationBadge classification={commit.classification} size="md" />
            )}
          </div>
          {commit && (
            <div className="flex items-center space-x-2 pt-1 font-mono text-xs text-slate-300">
              <span className="font-semibold text-slate-100">{commit.githubCommitSha}</span>
              <button
                type="button"
                onClick={() => handleCopySha(commit.githubCommitSha)}
                className="rounded border border-slate-700 bg-slate-800 px-1.5 py-0.5 text-[10px] text-slate-300 hover:bg-slate-700 hover:text-white"
                title="Copy full SHA"
              >
                {copiedSha ? 'Copied!' : 'Copy SHA'}
              </button>
              {commit.htmlUrl && (
                <a
                  href={commit.htmlUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-blue-400 hover:text-blue-300 hover:underline"
                >
                  View on GitHub ↗
                </a>
              )}
            </div>
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

      {isLoading ? (
        <div className="py-16 text-center text-slate-400">
          <LoadingSpinner size="md" />
          <p className="mt-2 text-xs">Loading commit details...</p>
        </div>
      ) : error ? (
        <div className="mt-4">
          <ErrorMessage
            title="Failed to load commit details"
            message={error}
            onRetry={onRetry}
          />
        </div>
      ) : commit ? (
        <div className="mt-6 space-y-6">
          {/* Commit Message */}
          <div className="rounded-lg border border-slate-800 bg-slate-950/60 p-4">
            <div className="text-[11px] font-medium uppercase text-slate-400">Commit Message</div>
            <pre className="mt-2 font-mono text-xs text-slate-200 whitespace-pre-wrap break-words">
              {commit.message}
            </pre>
          </div>

          {/* Author & Timestamp Info */}
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 rounded-lg border border-slate-800 bg-slate-950/40 p-4 text-xs">
            <div>
              <span className="text-slate-400 font-medium">Author:</span>
              <div className="mt-1 font-semibold text-slate-200">
                {commit.authorName || commit.authorUsername || commit.authorEmail || 'Anonymous'}
              </div>
              {commit.authorEmail && (
                <div className="font-mono text-slate-400 text-[11px]">{commit.authorEmail}</div>
              )}
            </div>

            <div>
              <span className="text-slate-400 font-medium">Committed At:</span>
              <div className="mt-1 font-mono text-slate-200">
                {formatDate(commit.committedAt)}
              </div>
              <div className="text-slate-400 text-[11px]">
                {formatRelativeTime(commit.committedAt)}
              </div>
            </div>
          </div>

          {/* Metric Summary Cards */}
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
            <div className="rounded-lg border border-slate-800 bg-slate-950/60 p-3">
              <div className="text-[11px] font-medium uppercase text-slate-400">Additions</div>
              <div className="mt-1 font-mono text-lg font-bold text-emerald-400">
                +{formatNumber(commit.additions)}
              </div>
            </div>

            <div className="rounded-lg border border-slate-800 bg-slate-950/60 p-3">
              <div className="text-[11px] font-medium uppercase text-slate-400">Deletions</div>
              <div className="mt-1 font-mono text-lg font-bold text-rose-400">
                -{formatNumber(commit.deletions)}
              </div>
            </div>

            <div className="rounded-lg border border-slate-800 bg-slate-950/60 p-3">
              <div className="text-[11px] font-medium uppercase text-slate-400">Total Changes</div>
              <div className="mt-1 font-mono text-lg font-bold text-slate-100">
                {formatNumber(commit.totalChanges)}
              </div>
            </div>

            <div className="rounded-lg border border-slate-800 bg-slate-950/60 p-3">
              <div className="text-[11px] font-medium uppercase text-slate-400">Files Changed</div>
              <div className="mt-1 font-mono text-lg font-bold text-blue-400">
                {formatNumber(commit.fileChanges?.length ?? 0)}
              </div>
            </div>
          </div>

          {/* Associated File Changes */}
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <h4 className="text-sm font-semibold text-slate-200">
                Associated File Changes ({commit.fileChanges?.length ?? 0})
              </h4>
              <span className="text-xs text-slate-400">
                Click a file path to inspect its File Intelligence
              </span>
            </div>

            <CommitFileChangesTable
              fileChanges={commit.fileChanges || []}
              onSelectFile={onSelectFile}
            />
          </div>
        </div>
      ) : null}
    </div>
  );
};
