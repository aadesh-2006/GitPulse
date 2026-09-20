import React, { useState } from 'react';
import { DateRangePreset, EvolutionDateRange, calculateDateRange } from '../../utils/dateRanges';
import { Button } from '../common/Button';

interface EvolutionDateRangePickerProps {
  dateRange: EvolutionDateRange;
  onChange: (range: EvolutionDateRange) => void;
  disabled?: boolean;
}

export const EvolutionDateRangePicker: React.FC<EvolutionDateRangePickerProps> = ({
  dateRange,
  onChange,
  disabled = false,
}) => {
  const [isCustomOpen, setIsCustomOpen] = useState<boolean>(dateRange.preset === 'custom');
  const [customStart, setCustomStart] = useState<string>(
    dateRange.customStartDate ||
      new Date(Date.now() - 180 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]
  );
  const [customEnd, setCustomEnd] = useState<string>(
    dateRange.customEndDate || new Date().toISOString().split('T')[0]
  );

  const handlePresetSelect = (preset: DateRangePreset) => {
    if (preset === 'custom') {
      setIsCustomOpen(true);
    } else {
      setIsCustomOpen(false);
      onChange(calculateDateRange(preset));
    }
  };

  const handleCustomApply = (e: React.FormEvent) => {
    e.preventDefault();
    if (customStart && customEnd && customStart <= customEnd) {
      onChange(calculateDateRange('custom', customStart, customEnd));
    }
  };

  return (
    <div className="flex flex-col gap-2 sm:flex-row sm:items-center">
      <div className="inline-flex rounded-md border border-slate-800 bg-slate-900/90 p-1 shadow-sm">
        <button
          type="button"
          disabled={disabled}
          onClick={() => handlePresetSelect('3m')}
          className={`rounded px-3 py-1 text-xs font-medium transition ${
            dateRange.preset === '3m'
              ? 'bg-blue-600 text-white font-semibold shadow-sm'
              : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/50'
          } disabled:cursor-not-allowed disabled:opacity-50`}
        >
          Last 3M
        </button>
        <button
          type="button"
          disabled={disabled}
          onClick={() => handlePresetSelect('6m')}
          className={`rounded px-3 py-1 text-xs font-medium transition ${
            dateRange.preset === '6m'
              ? 'bg-blue-600 text-white font-semibold shadow-sm'
              : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/50'
          } disabled:cursor-not-allowed disabled:opacity-50`}
        >
          Last 6M
        </button>
        <button
          type="button"
          disabled={disabled}
          onClick={() => handlePresetSelect('12m')}
          className={`rounded px-3 py-1 text-xs font-medium transition ${
            dateRange.preset === '12m'
              ? 'bg-blue-600 text-white font-semibold shadow-sm'
              : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/50'
          } disabled:cursor-not-allowed disabled:opacity-50`}
        >
          Last 12M
        </button>
        <button
          type="button"
          disabled={disabled}
          onClick={() => handlePresetSelect('custom')}
          className={`rounded px-3 py-1 text-xs font-medium transition ${
            dateRange.preset === 'custom'
              ? 'bg-blue-600 text-white font-semibold shadow-sm'
              : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/50'
          } disabled:cursor-not-allowed disabled:opacity-50`}
        >
          Custom
        </button>
      </div>

      {isCustomOpen && (
        <form
          onSubmit={handleCustomApply}
          className="flex items-center space-x-2 rounded-md border border-slate-800 bg-slate-900/90 p-1 px-2"
        >
          <input
            type="date"
            value={customStart}
            onChange={(e) => setCustomStart(e.target.value)}
            disabled={disabled}
            className="rounded border border-slate-700 bg-slate-950 px-2 py-0.5 text-xs text-slate-200 focus:border-blue-500 focus:outline-none"
            aria-label="Start Date"
          />
          <span className="text-xs text-slate-500">to</span>
          <input
            type="date"
            value={customEnd}
            onChange={(e) => setCustomEnd(e.target.value)}
            disabled={disabled}
            className="rounded border border-slate-700 bg-slate-950 px-2 py-0.5 text-xs text-slate-200 focus:border-blue-500 focus:outline-none"
            aria-label="End Date"
          />
          <Button type="submit" size="sm" variant="primary" disabled={disabled || !customStart || !customEnd}>
            Apply
          </Button>
        </form>
      )}
    </div>
  );
};
