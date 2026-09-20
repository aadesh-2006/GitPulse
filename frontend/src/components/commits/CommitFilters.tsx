import React, { useState, useEffect } from 'react';
import { CommitClassification } from '../../types/commit';

export type DateRangePreset = 'all' | '30d' | '90d' | '180d' | 'custom';

export interface CommitFiltersState {
  classification: CommitClassification | '';
  authorEmail: string;
  datePreset: DateRangePreset;
  from: string;
  to: string;
  sortField: string;
  sortDirection: 'asc' | 'desc';
  pageSize: number;
}

interface CommitFiltersProps {
  filters: CommitFiltersState;
  onClassificationChange: (classification: CommitClassification | '') => void;
  onAuthorEmailChange: (email: string) => void;
  onDatePresetChange: (preset: DateRangePreset) => void;
  onCustomDateChange: (from: string, to: string) => void;
  onSortChange: (field: string, direction: 'asc' | 'desc') => void;
  onPageSizeChange: (size: number) => void;
  onReset: () => void;
  disabled?: boolean;
}

const CLASSIFICATION_OPTIONS: { label: string; value: CommitClassification | '' }[] = [
  { label: 'All Classifications', value: '' },
  { label: 'Feature', value: 'FEATURE' },
  { label: 'Bug Fix', value: 'BUG_FIX' },
  { label: 'Refactor', value: 'REFACTOR' },
  { label: 'Documentation', value: 'DOCUMENTATION' },
  { label: 'Test', value: 'TEST' },
  { label: 'Build', value: 'BUILD' },
  { label: 'Configuration', value: 'CONFIGURATION' },
  { label: 'Dependency', value: 'DEPENDENCY' },
  { label: 'Other', value: 'OTHER' },
];

const SORT_OPTIONS: { label: string; field: string; direction: 'asc' | 'desc' }[] = [
  { label: 'Newest First (Committed At ↓)', field: 'committedAt', direction: 'desc' },
  { label: 'Oldest First (Committed At ↑)', field: 'committedAt', direction: 'asc' },
  { label: 'Highest Churn (Total Changes ↓)', field: 'totalChanges', direction: 'desc' },
  { label: 'Most Additions (Additions ↓)', field: 'additions', direction: 'desc' },
  { label: 'Most Deletions (Deletions ↓)', field: 'deletions', direction: 'desc' },
  { label: 'Commit SHA (A-Z)', field: 'githubCommitSha', direction: 'asc' },
];

export const CommitFilters: React.FC<CommitFiltersProps> = ({
  filters,
  onClassificationChange,
  onAuthorEmailChange,
  onDatePresetChange,
  onCustomDateChange,
  onSortChange,
  onPageSizeChange,
  onReset,
  disabled = false,
}) => {
  const [authorInput, setAuthorInput] = useState<string>(filters.authorEmail);
  const [customFrom, setCustomFrom] = useState<string>(filters.from ? filters.from.slice(0, 10) : '');
  const [customTo, setCustomTo] = useState<string>(filters.to ? filters.to.slice(0, 10) : '');

  useEffect(() => {
    setAuthorInput(filters.authorEmail);
  }, [filters.authorEmail]);

  useEffect(() => {
    setCustomFrom(filters.from ? filters.from.slice(0, 10) : '');
    setCustomTo(filters.to ? filters.to.slice(0, 10) : '');
  }, [filters.from, filters.to]);

  const handleAuthorKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      onAuthorEmailChange(authorInput);
    }
  };

  const handleCustomDateApply = () => {
    const fromIso = customFrom ? new Date(`${customFrom}T00:00:00Z`).toISOString() : '';
    const toIso = customTo ? new Date(`${customTo}T23:59:59.999Z`).toISOString() : '';
    onCustomDateChange(fromIso, toIso);
  };

  const currentSortKey = `${filters.sortField}_${filters.sortDirection}`;

  return (
    <div className="rounded-xl border border-slate-800 bg-slate-900/70 p-4 shadow-sm backdrop-blur space-y-3 text-xs">
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
        {/* Classification Filter */}
        <div>
          <label className="block text-[11px] font-medium text-slate-400 mb-1">
            Classification
          </label>
          <select
            value={filters.classification}
            onChange={(e) =>
              onClassificationChange(e.target.value as CommitClassification | '')
            }
            disabled={disabled}
            className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-1.5 text-slate-200 focus:border-blue-500 focus:outline-none disabled:opacity-50"
          >
            {CLASSIFICATION_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </div>

        {/* Author Email Filter */}
        <div>
          <label className="block text-[11px] font-medium text-slate-400 mb-1">
            Author Email
          </label>
          <div className="flex space-x-1.5">
            <input
              type="text"
              placeholder="e.g. dev@example.com"
              value={authorInput}
              onChange={(e) => setAuthorInput(e.target.value)}
              onKeyDown={handleAuthorKeyDown}
              disabled={disabled}
              className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-1.5 font-mono text-slate-200 placeholder-slate-500 focus:border-blue-500 focus:outline-none disabled:opacity-50"
            />
            <button
              type="button"
              onClick={() => onAuthorEmailChange(authorInput)}
              disabled={disabled}
              className="rounded-lg border border-slate-700 bg-slate-800 px-2.5 py-1.5 text-slate-300 hover:bg-slate-700 hover:text-white disabled:opacity-50"
              title="Apply Author Filter"
            >
              Filter
            </button>
          </div>
        </div>

        {/* Date Range Preset */}
        <div>
          <label className="block text-[11px] font-medium text-slate-400 mb-1">
            Date Window
          </label>
          <select
            value={filters.datePreset}
            onChange={(e) => onDatePresetChange(e.target.value as DateRangePreset)}
            disabled={disabled}
            className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-1.5 text-slate-200 focus:border-blue-500 focus:outline-none disabled:opacity-50"
          >
            <option value="all">All Time</option>
            <option value="30d">Last 30 Days</option>
            <option value="90d">Last 90 Days</option>
            <option value="180d">Last 6 Months</option>
            <option value="custom">Custom Date Range</option>
          </select>
        </div>

        {/* Sort Order */}
        <div>
          <label className="block text-[11px] font-medium text-slate-400 mb-1">
            Sort Order
          </label>
          <select
            value={currentSortKey}
            onChange={(e) => {
              const selected = SORT_OPTIONS.find(
                (opt) => `${opt.field}_${opt.direction}` === e.target.value
              );
              if (selected) {
                onSortChange(selected.field, selected.direction);
              }
            }}
            disabled={disabled}
            className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-1.5 text-slate-200 focus:border-blue-500 focus:outline-none disabled:opacity-50"
          >
            {SORT_OPTIONS.map((opt) => (
              <option
                key={`${opt.field}_${opt.direction}`}
                value={`${opt.field}_${opt.direction}`}
              >
                {opt.label}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Custom Date Range Picker Row (visible only when datePreset is custom) */}
      {filters.datePreset === 'custom' && (
        <div className="flex flex-wrap items-center gap-3 pt-2 border-t border-slate-800">
          <div className="flex items-center space-x-2">
            <span className="text-slate-400">From:</span>
            <input
              type="date"
              value={customFrom}
              onChange={(e) => setCustomFrom(e.target.value)}
              disabled={disabled}
              className="rounded border border-slate-700 bg-slate-950 px-2 py-1 text-slate-200 focus:border-blue-500 focus:outline-none disabled:opacity-50"
            />
          </div>

          <div className="flex items-center space-x-2">
            <span className="text-slate-400">To:</span>
            <input
              type="date"
              value={customTo}
              onChange={(e) => setCustomTo(e.target.value)}
              disabled={disabled}
              className="rounded border border-slate-700 bg-slate-950 px-2 py-1 text-slate-200 focus:border-blue-500 focus:outline-none disabled:opacity-50"
            />
          </div>

          <button
            type="button"
            onClick={handleCustomDateApply}
            disabled={disabled}
            className="rounded border border-blue-700 bg-blue-600 px-3 py-1 text-xs font-semibold text-white hover:bg-blue-500 disabled:opacity-50"
          >
            Apply Dates
          </button>
        </div>
      )}

      {/* Page Size & Reset Actions */}
      <div className="flex flex-wrap items-center justify-between gap-3 pt-2 border-t border-slate-800/80">
        <div className="flex items-center space-x-2">
          <span className="text-slate-400">Per page:</span>
          <select
            value={filters.pageSize}
            onChange={(e) => onPageSizeChange(Number(e.target.value))}
            disabled={disabled}
            className="rounded border border-slate-700 bg-slate-950 px-2 py-1 text-slate-200 focus:border-blue-500 focus:outline-none disabled:opacity-50"
          >
            <option value={10}>10</option>
            <option value={20}>20</option>
            <option value={50}>50</option>
            <option value={100}>100</option>
          </select>
        </div>

        <button
          type="button"
          onClick={onReset}
          disabled={disabled}
          className="rounded border border-slate-700 bg-slate-800 px-3 py-1 text-slate-300 hover:bg-slate-700 hover:text-white disabled:opacity-50"
        >
          Reset Filters
        </button>
      </div>
    </div>
  );
};
