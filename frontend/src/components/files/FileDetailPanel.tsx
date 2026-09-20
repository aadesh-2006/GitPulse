import React from 'react';
import { RepositoryFileResponse } from '../../types/file';
import { FileScore } from './FileScore';
import { PrimaryContributor } from './PrimaryContributor';
import { formatDate, formatNumber, formatRelativeTime } from '../../utils/formatters';

interface FileDetailPanelProps {
  file: RepositoryFileResponse | null;
  isLoading: boolean;
  error: string | null;
  onClose: () => void;
}

export const FileDetailPanel: React.FC<FileDetailPanelProps> = ({
  file,
  isLoading,
  error,
  onClose,
}) => {
  if (!file && !isLoading && !error) {
    return null;
  }

  return (
    <div className="fixed inset-y-0 right-0 z-50 flex max-w-full pl-10">
      <div className="w-screen max-w-md bg-slate-900 border-l border-slate-800 shadow-2xl p-6 overflow-y-auto">
        {/* Header */}
        <div className="flex items-start justify-between pb-4 border-b border-slate-800">
          <div>
            <span className="text-[10px] font-semibold uppercase tracking-wider text-blue-400">
              File Intelligence Detail
            </span>
            <h3 className="mt-1 font-mono text-sm font-bold text-white break-all">
              {file?.fileName || 'Loading...'}
            </h3>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-md p-1.5 text-slate-400 hover:bg-slate-800 hover:text-slate-200"
            aria-label="Close panel"
          >
            <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {isLoading ? (
          <div className="py-24 text-center text-xs text-slate-400">
            <svg
              className="mx-auto h-6 w-6 animate-spin text-blue-500 mb-2"
              fill="none"
              viewBox="0 0 24 24"
            >
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
            </svg>
            Loading file intelligence...
          </div>
        ) : error ? (
          <div className="mt-6 rounded-md border border-rose-900/50 bg-rose-950/30 p-3 text-xs text-rose-300">
            {error}
          </div>
        ) : file ? (
          <div className="mt-6 space-y-6 text-xs">
            {/* Path & Metadata */}
            <div className="space-y-2 rounded-md border border-slate-800 bg-slate-950/60 p-3.5">
              <div>
                <span className="text-slate-500">Full Path</span>
                <p className="font-mono text-slate-200 break-all text-xs mt-0.5">{file.filePath}</p>
              </div>
              <div className="grid grid-cols-2 gap-2 pt-2 border-t border-slate-800/80">
                <div>
                  <span className="text-slate-500">Extension</span>
                  <p className="font-mono text-slate-300">{file.extension || '—'}</p>
                </div>
                <div>
                  <span className="text-slate-500">Status</span>
                  <p>
                    {file.isDeleted ? (
                      <span className="text-rose-400 font-semibold">Deleted</span>
                    ) : (
                      <span className="text-emerald-400 font-semibold">Active</span>
                    )}
                  </p>
                </div>
              </div>
            </div>

            {/* Deterministic Risk Scores Card */}
            <div className="space-y-3 rounded-md border border-slate-800 bg-slate-950/60 p-3.5">
              <span className="font-semibold text-slate-200 uppercase tracking-wider text-[11px]">
                Deterministic Risk Signal
              </span>

              <div className="grid grid-cols-2 gap-3 pt-1">
                <div className="rounded border border-slate-800 bg-slate-900 p-2.5">
                  <span className="text-slate-400 text-[11px]">Composite Score</span>
                  <div className="mt-1">
                    <FileScore score={file.compositeScore} size="lg" />
                  </div>
                  <span className="text-[10px] text-slate-500">Multidimensional Model</span>
                </div>

                <div className="rounded border border-slate-800 bg-slate-900 p-2.5">
                  <span className="text-slate-400 text-[11px]">Baseline Score</span>
                  <div className="mt-1">
                    <FileScore score={file.baselineScore} size="lg" showBar={false} />
                  </div>
                  <span className="text-[10px] text-slate-500">Frequency Only</span>
                </div>
              </div>

              {/* Sub-components breakdown */}
              <div className="space-y-2 pt-2 border-t border-slate-800/80 text-[11px]">
                <span className="text-slate-400 font-medium">Model Component Breakdown:</span>
                <div className="flex items-center justify-between">
                  <span className="text-slate-500">Revision Frequency:</span>
                  <span className="font-mono text-slate-300">{(file.revisionFrequencyScore ?? 0).toFixed(2)}</span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-slate-500">Code Churn:</span>
                  <span className="font-mono text-slate-300">{(file.churnScore ?? 0).toFixed(2)}</span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-slate-500">Recency:</span>
                  <span className="font-mono text-slate-300">{(file.recencyScore ?? 0).toFixed(2)}</span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-slate-500">Ownership Concentration:</span>
                  <span className="font-mono text-slate-300">{(file.ownershipConcentrationScore ?? 0).toFixed(2)}</span>
                </div>
              </div>
            </div>

            {/* Activity Totals */}
            <div className="space-y-2.5 rounded-md border border-slate-800 bg-slate-950/60 p-3.5 font-mono">
              <span className="font-sans font-semibold text-slate-200 uppercase tracking-wider text-[11px]">
                Historical Activity Totals
              </span>

              <div className="grid grid-cols-2 gap-2 pt-1">
                <div>
                  <span className="font-sans text-slate-500">Revisions</span>
                  <p className="text-sm font-bold text-slate-200">{formatNumber(file.totalRevisions)}</p>
                </div>
                <div>
                  <span className="font-sans text-slate-500">Total Churn</span>
                  <p className="text-sm font-bold text-slate-200">{formatNumber(file.totalChurn)}</p>
                </div>
                <div>
                  <span className="font-sans text-slate-500">Additions</span>
                  <p className="text-sm font-bold text-emerald-400">+{formatNumber(file.totalAdditions)}</p>
                </div>
                <div>
                  <span className="font-sans text-slate-500">Deletions</span>
                  <p className="text-sm font-bold text-rose-400">-{formatNumber(file.totalDeletions)}</p>
                </div>
              </div>
            </div>

            {/* Authorship & Timeline */}
            <div className="space-y-2.5 rounded-md border border-slate-800 bg-slate-950/60 p-3.5">
              <span className="font-semibold text-slate-200 uppercase tracking-wider text-[11px]">
                Attribution & Timeline
              </span>

              <div className="space-y-2 pt-1">
                <div>
                  <span className="text-slate-500">Primary Contributor:</span>
                  <div className="mt-1">
                    <PrimaryContributor contributor={file.primaryContributor} />
                  </div>
                </div>
                <div className="pt-2 border-t border-slate-800/80">
                  <span className="text-slate-500">First Modified:</span>
                  <p className="text-slate-300 font-mono">{formatDate(file.firstModifiedAt)}</p>
                </div>
                <div>
                  <span className="text-slate-500">Last Modified:</span>
                  <p className="text-slate-300 font-mono">
                    {formatDate(file.lastModifiedAt)} ({formatRelativeTime(file.lastModifiedAt)})
                  </p>
                </div>
              </div>
            </div>
          </div>
        ) : null}
      </div>
    </div>
  );
};
