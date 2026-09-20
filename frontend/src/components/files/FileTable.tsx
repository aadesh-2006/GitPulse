import React from 'react';
import { Page } from '../../types/api';
import { RepositoryFileResponse } from '../../types/file';
import { FileScore } from './FileScore';
import { PrimaryContributor } from './PrimaryContributor';
import { FilePagination } from './FilePagination';
import { formatNumber, formatRelativeTime } from '../../utils/formatters';

interface FileTableProps {
  filePage: Page<RepositoryFileResponse> | null;
  isLoading: boolean;
  currentPage: number;
  onPageChange: (page: number) => void;
  onSelectFile: (filePath: string) => void;
  selectedFilePath?: string | null;
}

export const FileTable: React.FC<FileTableProps> = ({
  filePage,
  isLoading,
  currentPage,
  onPageChange,
  onSelectFile,
  selectedFilePath,
}) => {
  const files = filePage?.content ?? [];

  return (
    <div className="rounded-lg border border-slate-800 bg-slate-900/90 p-6 shadow-sm space-y-4">
      <div className="flex flex-col gap-1 pb-4 border-b border-slate-800/80">
        <h2 className="text-base font-semibold text-white">Repository Files</h2>
        <p className="text-xs text-slate-400">
          Paginated and filtered catalog of tracked repository files and activity metrics
        </p>
      </div>

      {isLoading && files.length === 0 ? (
        <div className="h-64 flex items-center justify-center text-xs text-slate-400">
          Loading repository files...
        </div>
      ) : files.length === 0 ? (
        <div className="h-48 flex items-center justify-center text-xs text-slate-500 italic">
          No files found matching the selected filter criteria.
        </div>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="border-b border-slate-800 text-slate-400 uppercase tracking-wider text-[11px]">
              <tr>
                <th className="py-2.5 px-3">File Path</th>
                <th className="py-2.5 px-3">Composite</th>
                <th className="py-2.5 px-3">Baseline</th>
                <th className="py-2.5 px-3 text-right">Revisions</th>
                <th className="py-2.5 px-3 text-right">Total Churn</th>
                <th className="py-2.5 px-3">Primary Contributor</th>
                <th className="py-2.5 px-3">Last Modified</th>
                <th className="py-2.5 px-3 text-center">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60 font-sans">
              {files.map((file) => {
                const isSelected = selectedFilePath === file.filePath;

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

                    <td className="py-3 px-3 text-right font-mono text-slate-300">
                      {formatNumber(file.totalRevisions)}
                    </td>

                    <td className="py-3 px-3 text-right font-mono text-slate-300">
                      <div>{formatNumber(file.totalChurn)}</div>
                      <div className="text-[10px] text-slate-500 font-mono">
                        +{formatNumber(file.totalAdditions)} / -{formatNumber(file.totalDeletions)}
                      </div>
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
      )}

      {/* Server Pagination */}
      <FilePagination
        pageData={filePage}
        currentPage={currentPage}
        onPageChange={onPageChange}
        disabled={isLoading}
      />
    </div>
  );
};
