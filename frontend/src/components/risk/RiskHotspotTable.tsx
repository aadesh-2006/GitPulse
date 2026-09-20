import React from 'react';
import { RepositoryFileResponse } from '../../types/file';
import { Page } from '../../types/api';
import { FileScore } from '../files/FileScore';
import { PrimaryContributor } from '../files/PrimaryContributor';
import { Pagination } from '../common/Pagination';
import { LoadingSpinner } from '../common/LoadingSpinner';

interface RiskHotspotTableProps {
  filesPage: Page<RepositoryFileResponse> | null;
  isLoading: boolean;
  currentPage: number;
  onPageChange: (page: number) => void;
  onSelectFile: (filePath: string) => void;
  selectedFilePath: string | null;
}

export const RiskHotspotTable: React.FC<RiskHotspotTableProps> = ({
  filesPage,
  isLoading,
  currentPage,
  onPageChange,
  onSelectFile,
  selectedFilePath,
}) => {
  return (
    <div className="space-y-3">
      <div className="overflow-x-auto rounded-lg border border-slate-800 bg-slate-900/60 shadow">
        <table className="w-full text-left text-xs text-slate-300">
          <thead className="border-b border-slate-800 bg-slate-900/90 text-[11px] uppercase tracking-wider text-slate-400">
            <tr>
              <th className="px-4 py-3">File Path</th>
              <th className="px-4 py-3">Composite Score</th>
              <th className="px-4 py-3">Baseline</th>
              <th className="px-4 py-3">Diff (Δ)</th>
              <th className="px-4 py-3 text-right">Revision Score</th>
              <th className="px-4 py-3 text-right">Churn Score</th>
              <th className="px-4 py-3 text-right">Recency Score</th>
              <th className="px-4 py-3 text-right">Ownership Score</th>
              <th className="px-4 py-3">Primary Contributor</th>
              <th className="px-4 py-3 text-center">Action</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800/60 font-sans">
            {isLoading ? (
              <tr>
                <td colSpan={10} className="py-12 text-center text-slate-400">
                  <div className="flex flex-col items-center justify-center space-y-2">
                    <LoadingSpinner size="md" />
                    <span>Loading repository risk & stability signal...</span>
                  </div>
                </td>
              </tr>
            ) : !filesPage || filesPage.content.length === 0 ? (
              <tr>
                <td colSpan={10} className="py-8 text-center text-slate-500 italic">
                  No files analyzed or found for this repository.
                </td>
              </tr>
            ) : (
              filesPage.content.map((file) => {
                const isSelected = selectedFilePath === file.filePath;
                const baseline = file.baselineScore ?? 0;
                const composite = file.compositeScore ?? 0;
                const delta = composite - baseline;
                const deltaFormatted = delta >= 0 ? `+${delta.toFixed(2)}` : delta.toFixed(2);

                return (
                  <tr
                    key={file.id || file.filePath}
                    onClick={() => onSelectFile(file.filePath)}
                    className={`cursor-pointer transition-colors ${
                      isSelected
                        ? 'bg-blue-950/40 ring-1 ring-blue-500/30'
                        : 'hover:bg-slate-800/40'
                    }`}
                  >
                    <td className="px-4 py-3 font-mono text-slate-200 break-all max-w-xs">
                      {file.filePath}
                    </td>
                    <td className="px-4 py-3 whitespace-nowrap">
                      <FileScore score={file.compositeScore} />
                    </td>
                    <td className="px-4 py-3 font-mono text-slate-300 whitespace-nowrap">
                      {baseline.toFixed(2)}
                    </td>
                    <td className="px-4 py-3 font-mono text-xs whitespace-nowrap">
                      <span className={delta > 0 ? 'text-blue-400' : delta < 0 ? 'text-slate-400' : 'text-slate-500'}>
                        {deltaFormatted}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-right font-mono text-slate-300">
                      {(file.revisionFrequencyScore ?? 0).toFixed(2)}
                    </td>
                    <td className="px-4 py-3 text-right font-mono text-slate-300">
                      {(file.churnScore ?? 0).toFixed(2)}
                    </td>
                    <td className="px-4 py-3 text-right font-mono text-slate-300">
                      {(file.recencyScore ?? 0).toFixed(2)}
                    </td>
                    <td className="px-4 py-3 text-right font-mono text-slate-300">
                      {(file.ownershipConcentrationScore ?? 0).toFixed(2)}
                    </td>
                    <td className="px-4 py-3">
                      <PrimaryContributor contributor={file.primaryContributor} />
                    </td>
                    <td className="px-4 py-3 text-center">
                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation();
                          onSelectFile(file.filePath);
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
        pageData={filesPage}
        currentPage={currentPage}
        onPageChange={onPageChange}
        itemLabel="files"
        disabled={isLoading}
      />
    </div>
  );
};
