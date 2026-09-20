import React from 'react';
import { FileFiltersState, DeletedFilterOption } from '../../hooks/useFileIntelligence';
import { Button } from '../common/Button';

interface FileFiltersProps {
  filters: FileFiltersState;
  onExtensionChange: (ext: string) => void;
  onDeletedChange: (deleted: DeletedFilterOption) => void;
  onSortChange: (field: string, direction: 'asc' | 'desc') => void;
  onPageSizeChange: (size: number) => void;
  onReset: () => void;
  disabled?: boolean;
}

const SORT_OPTIONS = [
  { value: 'compositeScore', label: 'Composite Score' },
  { value: 'baselineScore', label: 'Baseline Score' },
  { value: 'totalChurn', label: 'Total Churn' },
  { value: 'totalRevisions', label: 'Total Revisions' },
  { value: 'totalAdditions', label: 'Additions' },
  { value: 'totalDeletions', label: 'Deletions' },
  { value: 'lastModifiedAt', label: 'Last Modified' },
  { value: 'firstModifiedAt', label: 'First Modified' },
  { value: 'filePath', label: 'File Path' },
];

export const FileFilters: React.FC<FileFiltersProps> = ({
  filters,
  onExtensionChange,
  onDeletedChange,
  onSortChange,
  onPageSizeChange,
  onReset,
  disabled = false,
}) => {
  return (
    <div className="flex flex-col gap-3 rounded-lg border border-slate-800 bg-slate-900/90 p-4 shadow-sm sm:flex-row sm:items-center sm:justify-between flex-wrap">
      <div className="flex flex-wrap items-center gap-3">
        {/* Extension input */}
        <div className="flex items-center space-x-2">
          <label htmlFor="ext-filter" className="text-xs text-slate-400 font-medium">
            Extension:
          </label>
          <input
            id="ext-filter"
            type="text"
            placeholder="e.g. java, ts, py"
            value={filters.extension}
            onChange={(e) => onExtensionChange(e.target.value)}
            disabled={disabled}
            className="w-32 rounded-md border border-slate-700 bg-slate-950 px-2.5 py-1 text-xs text-slate-200 placeholder-slate-500 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
        </div>

        {/* Deleted Filter Segment */}
        <div className="flex items-center space-x-1 rounded-md border border-slate-700/80 bg-slate-950 p-0.5">
          {(['all', 'active', 'deleted'] as DeletedFilterOption[]).map((option) => (
            <button
              key={option}
              type="button"
              disabled={disabled}
              onClick={() => onDeletedChange(option)}
              className={`rounded px-2.5 py-1 text-xs font-medium capitalize transition ${
                filters.deletedFilter === option
                  ? 'bg-blue-600 text-white font-semibold shadow-sm'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              {option}
            </button>
          ))}
        </div>

        {/* Sort Select */}
        <div className="flex items-center space-x-2">
          <label htmlFor="sort-field" className="text-xs text-slate-400 font-medium">
            Sort:
          </label>
          <select
            id="sort-field"
            value={filters.sortField}
            onChange={(e) => onSortChange(e.target.value, filters.sortDirection)}
            disabled={disabled}
            className="rounded-md border border-slate-700 bg-slate-950 px-2.5 py-1 text-xs text-slate-200 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          >
            {SORT_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value} className="bg-slate-900 text-slate-200">
                {opt.label}
              </option>
            ))}
          </select>

          <button
            type="button"
            disabled={disabled}
            onClick={() =>
              onSortChange(
                filters.sortField,
                filters.sortDirection === 'desc' ? 'asc' : 'desc'
              )
            }
            className="rounded border border-slate-700 bg-slate-950 px-2 py-1 text-xs text-slate-300 hover:text-white"
            title={filters.sortDirection === 'desc' ? 'Descending' : 'Ascending'}
          >
            {filters.sortDirection === 'desc' ? '↓ DESC' : '↑ ASC'}
          </button>
        </div>
      </div>

      <div className="flex items-center space-x-3">
        {/* Page size */}
        <div className="flex items-center space-x-1.5 text-xs text-slate-400">
          <span>Show:</span>
          <select
            value={filters.pageSize}
            onChange={(e) => onPageSizeChange(parseInt(e.target.value, 10))}
            disabled={disabled}
            className="rounded border border-slate-700 bg-slate-950 px-2 py-1 text-xs text-slate-200 focus:border-blue-500 focus:outline-none"
          >
            <option value={10}>10</option>
            <option value={20}>20</option>
            <option value={50}>50</option>
          </select>
        </div>

        <Button variant="outline" size="sm" onClick={onReset} disabled={disabled}>
          Reset
        </Button>
      </div>
    </div>
  );
};
