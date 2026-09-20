import React from 'react';
import { RepositoryFileResponse } from '../../types/file';
import { FileScore } from './FileScore';
import { PrimaryContributor } from './PrimaryContributor';
import { formatNumber, formatRelativeTime } from '../../utils/formatters';

interface HotspotTableProps {
  hotspots: RepositoryFileResponse[];
  isLoading: boolean;
  onSelectFile: (filePath: string) => void;
  selectedFilePath?: string | null;
}

export const HotspotTable: React.FC<HotspotTableProps> = ({
  hotspots,
  isLoading,
  onSelectFile,
  selectedFilePath,
}) => {
  if (isLoading && hotspots.length === 0) {
    return (
      <div className="rounded-lg border border-slate-800 bg-slate-900/90 p-6 shadow-sm">
        <div className="h-48 flex items-center justify-center text-xs text-slate-400">
          Loading hotspot rankings...
        </div>
      </div>
    );
  }

  if (hotspots.length === 0) {
    return (
      <div className="rounded-lg border border-slate-800 bg-slate-900/90 p-6 shadow-sm text-center">
        <p className="text-xs text-slate-400 italic">No code hotspots detected for this repository.</p>
      </div>
    );
  }

  const formatDifference = (composite: number, baseline: number): string => {
    const diff = composite - baseline;
    if (Math.abs(diff) < 0.005) return '0.00';
    return diff > 0 ? `+${diff.toFixed(2)}` : diff.toFixed(2);
  };

  return (
    <div className="rounded-lg border border-slate-800 bg-slate-900/90 p-6 shadow-sm">
      <div className="flex flex-col gap-1 pb-4 border-b border-slate-800/80">
        <div className="flex items-center space-x-2">
          <h2 className="text-base font-semibold text-white">Repository Code Hotspots</h2>
          <span className="rounded bg-rose-950/80 text-rose-400 border border-rose-800/60 px-1.5 py-0.2 text-[10px] font-semibold">
            Top 10 Ranked
          </span>
        </div>
        <p className="text-xs text-slate-400">
          Files exhibiting high combined activity density, revision velocity, and multidimensional change signals
        </p>
      </div>

      <div className="mt-4 overflow-x-auto">
        <table className="w-full text-left text-xs">
          <thead className="border-b border-slate-800 text-slate-400 uppercase tracking-wider text-[11px]">
            <tr>
              <th className="py-2.5 px-3">File</th>
              <th className="py-2.5 px-3">Composite Score</th>
              <th className="py-2.5 px-3">Baseline Score</th>
              <th className="py-2.5 px-3 text-center">Difference</th>
              <th className="py-2.5 px-3 text-right">Revisions</th>
              <th className="py-2.5 px-3 text-right">Total Churn</th>
              <th className="py-2.5 px-3">Primary Contributor</th>
              <th className="py-2.5 px-3">Last Modified</th>
              <th className="py-2.5 px-3 text-center">Status</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800/60 font-sans">
            {hotspots.map((file) => {
              const isSelected = selectedFilePath === file.filePath;
              const diffText = formatDifference(file.compositeScore, file.baselineScore);

              return (
                <tr
                  key={file.id}
                  onClick={() => onSelectFile(file.filePath)}
                  className={`cursor-pointer transition-colors ${
                    isSelected
                      ? 'bg-blue-950/40 border-l-2 border-l-blue-500'
                      : 'hover:bg-slate-800/40'
                  }`}
                >
                  <td className="py-3 px-3">
                    <div className="flex items-center space-x-2">
                      <span className="font-mono text-slate-200 font-medium break-all hover:text-blue-400">
                        {file.filePath}
                      </span>
                    </div>
                  </td>

                  <td className="py-3 px-3">
                    <FileScore score={file.compositeScore} />
                  </td>

                  <td className="py-3 px-3">
                    <FileScore score={file.baselineScore} showBar={false} />
                  </td>

                  <td className="py-3 px-3 text-center font-mono text-xs">
                    <span className="rounded bg-slate-800/90 px-1.5 py-0.5 text-slate-300 border border-slate-700/60">
                      {diffText}
                    </span>
                  </td>

                  <td className="py-3 px-3 text-right font-mono text-slate-300">
                    {formatNumber(file.totalRevisions)}
                  </td>

                  <td className="py-3 px-3 text-right font-mono text-slate-300">
                    {formatNumber(file.totalChurn)}
                  </td>

                  <td className="py-3 px-3">
                    <PrimaryContributor contributor={file.primaryContributor} />
                  </td>

                  <td className="py-3 px-3 text-slate-400 text-xs">
                    {formatRelativeTime(file.lastModifiedAt)}
                  </td>

                  <td className="py-3 px-3 text-center">
                    {file.isDeleted ? (
                      <span className="rounded bg-slate-800 px-1.5 py-0.5 text-[10px] font-semibold text-rose-400 border border-rose-900/50">
                        Deleted
                      </span>
                    ) : (
                      <span className="rounded bg-slate-800 px-1.5 py-0.5 text-[10px] font-semibold text-emerald-400 border border-emerald-900/50">
                        Active
                      </span>
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
};
