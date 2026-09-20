import React from 'react';
import { RepositoryFileResponse } from '../../types/file';

interface RiskComparisonTableProps {
  files: RepositoryFileResponse[];
  onSelectFile: (filePath: string) => void;
  selectedFilePath: string | null;
}

export const RiskComparisonTable: React.FC<RiskComparisonTableProps> = ({
  files,
  onSelectFile,
  selectedFilePath,
}) => {
  if (!files || files.length === 0) {
    return null;
  }

  // Show top 10 sample files for clean comparison
  const sampleFiles = files.slice(0, 10);

  return (
    <div className="rounded-xl border border-slate-800 bg-slate-900/80 p-6 shadow-sm backdrop-blur space-y-4">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between border-b border-slate-800 pb-3">
        <div>
          <h3 className="text-sm font-bold uppercase tracking-wider text-slate-200">
            Baseline vs Multidimensional Signal Comparison
          </h3>
          <p className="text-xs text-slate-400">
            Comparative analysis of frequency-only baseline vs 4-dimensional score components.
          </p>
        </div>
        <span className="mt-2 sm:mt-0 text-[11px] font-mono text-slate-400">
          Top {sampleFiles.length} sample files
        </span>
      </div>

      <div className="overflow-x-auto">
        <table className="w-full text-left text-xs text-slate-300">
          <thead className="border-b border-slate-800 bg-slate-900/90 text-[11px] uppercase tracking-wider text-slate-400 font-mono">
            <tr>
              <th className="px-3 py-2.5 font-sans">File</th>
              <th className="px-3 py-2.5 text-right">Baseline</th>
              <th className="px-3 py-2.5 text-right">Composite</th>
              <th className="px-3 py-2.5 text-right">Revision (30%)</th>
              <th className="px-3 py-2.5 text-right">Churn (30%)</th>
              <th className="px-3 py-2.5 text-right">Recency (20%)</th>
              <th className="px-3 py-2.5 text-right">Ownership (20%)</th>
              <th className="px-3 py-2.5 text-right">Difference (Δ)</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800/60 font-mono">
            {sampleFiles.map((file) => {
              const isSelected = selectedFilePath === file.filePath;
              const baseline = file.baselineScore ?? 0;
              const composite = file.compositeScore ?? 0;
              const delta = composite - baseline;
              const deltaFormatted = delta >= 0 ? `+${delta.toFixed(2)}` : delta.toFixed(2);

              return (
                <tr
                  key={file.filePath}
                  onClick={() => onSelectFile(file.filePath)}
                  className={`cursor-pointer transition-colors ${
                    isSelected
                      ? 'bg-blue-950/40 ring-1 ring-blue-500/30'
                      : 'hover:bg-slate-800/30'
                  }`}
                >
                  <td className="px-3 py-2.5 font-mono text-slate-200 max-w-xs truncate">
                    {file.filePath}
                  </td>
                  <td className="px-3 py-2.5 text-right text-slate-300">
                    {baseline.toFixed(2)}
                  </td>
                  <td className="px-3 py-2.5 text-right font-bold text-blue-400">
                    {composite.toFixed(2)}
                  </td>
                  <td className="px-3 py-2.5 text-right text-slate-400">
                    {(file.revisionFrequencyScore ?? 0).toFixed(2)}
                  </td>
                  <td className="px-3 py-2.5 text-right text-slate-400">
                    {(file.churnScore ?? 0).toFixed(2)}
                  </td>
                  <td className="px-3 py-2.5 text-right text-slate-400">
                    {(file.recencyScore ?? 0).toFixed(2)}
                  </td>
                  <td className="px-3 py-2.5 text-right text-slate-400">
                    {(file.ownershipConcentrationScore ?? 0).toFixed(2)}
                  </td>
                  <td className="px-3 py-2.5 text-right font-semibold">
                    <span className={delta > 0 ? 'text-blue-400' : delta < 0 ? 'text-slate-400' : 'text-slate-500'}>
                      {deltaFormatted}
                    </span>
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
