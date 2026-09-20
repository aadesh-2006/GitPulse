import React from 'react';
import { CommitFileChangeResponse, FileChangeStatus } from '../../types/commit';
import { formatNumber } from '../../utils/formatters';

interface CommitFileChangesTableProps {
  fileChanges: CommitFileChangeResponse[];
  onSelectFile?: (filePath: string) => void;
}

const STATUS_BADGES: Record<FileChangeStatus, { label: string; bg: string; text: string; border: string }> = {
  ADDED: {
    label: 'ADDED',
    bg: 'bg-emerald-950/60',
    text: 'text-emerald-400',
    border: 'border-emerald-800/60',
  },
  MODIFIED: {
    label: 'MODIFIED',
    bg: 'bg-blue-950/60',
    text: 'text-blue-400',
    border: 'border-blue-800/60',
  },
  REMOVED: {
    label: 'REMOVED',
    bg: 'bg-rose-950/60',
    text: 'text-rose-400',
    border: 'border-rose-800/60',
  },
  RENAMED: {
    label: 'RENAMED',
    bg: 'bg-amber-950/60',
    text: 'text-amber-400',
    border: 'border-amber-800/60',
  },
  UNKNOWN: {
    label: 'UNKNOWN',
    bg: 'bg-slate-800/60',
    text: 'text-slate-400',
    border: 'border-slate-700/60',
  },
};

export const CommitFileChangesTable: React.FC<CommitFileChangesTableProps> = ({
  fileChanges,
  onSelectFile,
}) => {
  if (!fileChanges || fileChanges.length === 0) {
    return (
      <div className="rounded-lg border border-slate-800 bg-slate-950/40 p-6 text-center text-xs text-slate-500 italic">
        No file changes recorded for this commit.
      </div>
    );
  }

  return (
    <div className="overflow-x-auto rounded-lg border border-slate-800 bg-slate-900/60 shadow">
      <table className="w-full text-left text-xs text-slate-300">
        <thead className="border-b border-slate-800 bg-slate-900/90 text-[11px] uppercase tracking-wider text-slate-400">
          <tr>
            <th className="px-3 py-2.5">Status</th>
            <th className="px-3 py-2.5">File Path</th>
            <th className="px-3 py-2.5 text-right">Additions</th>
            <th className="px-3 py-2.5 text-right">Deletions</th>
            <th className="px-3 py-2.5 text-right">Changes</th>
            <th className="px-3 py-2.5 text-center">Links</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-800/60 font-sans">
          {fileChanges.map((change) => {
            const statusConfig = STATUS_BADGES[change.status] || STATUS_BADGES.UNKNOWN;
            return (
              <tr key={change.id || change.filePath} className="transition-colors hover:bg-slate-800/40">
                <td className="px-3 py-2.5 whitespace-nowrap">
                  <span
                    className={`inline-flex items-center rounded border px-1.5 py-0.5 text-[10px] font-semibold ${statusConfig.bg} ${statusConfig.text} ${statusConfig.border}`}
                  >
                    {statusConfig.label}
                  </span>
                </td>
                <td className="px-3 py-2.5 font-mono text-slate-200 break-all">
                  {onSelectFile ? (
                    <button
                      type="button"
                      onClick={() => onSelectFile(change.filePath)}
                      className="text-left text-blue-400 hover:text-blue-300 hover:underline transition font-mono"
                      title="Inspect file intelligence"
                    >
                      {change.filePath}
                    </button>
                  ) : (
                    <span>{change.filePath}</span>
                  )}
                </td>
                <td className="px-3 py-2.5 text-right font-mono text-emerald-400">
                  +{formatNumber(change.additions)}
                </td>
                <td className="px-3 py-2.5 text-right font-mono text-rose-400">
                  -{formatNumber(change.deletions)}
                </td>
                <td className="px-3 py-2.5 text-right font-mono font-semibold text-slate-100">
                  {formatNumber(change.changes)}
                </td>
                <td className="px-3 py-2.5 text-center space-x-2">
                  {change.blobUrl && (
                    <a
                      href={change.blobUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="text-[11px] text-blue-400 hover:text-blue-300 hover:underline"
                    >
                      Blob
                    </a>
                  )}
                  {change.rawUrl && (
                    <a
                      href={change.rawUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="text-[11px] text-slate-400 hover:text-slate-300 hover:underline"
                    >
                      Raw
                    </a>
                  )}
                  {!change.blobUrl && !change.rawUrl && (
                    <span className="text-slate-600">—</span>
                  )}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
};
