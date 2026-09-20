import React from 'react';
import { Page } from '../../types/api';
import { formatNumber } from '../../utils/formatters';

interface PaginationProps {
  pageData: Page<unknown> | null;
  currentPage: number;
  onPageChange: (page: number) => void;
  itemLabel?: string;
  disabled?: boolean;
}

export const Pagination: React.FC<PaginationProps> = ({
  pageData,
  currentPage,
  onPageChange,
  itemLabel = 'items',
  disabled = false,
}) => {
  if (!pageData || pageData.totalElements === 0) {
    return null;
  }

  const totalPages = pageData.totalPages;
  const isFirst = pageData.first;
  const isLast = pageData.last;

  const startElement = currentPage * pageData.size + 1;
  const endElement = Math.min((currentPage + 1) * pageData.size, pageData.totalElements);

  return (
    <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between px-2 py-3 text-xs text-slate-400">
      <div>
        Showing <span className="font-mono text-slate-200">{startElement}</span> to{' '}
        <span className="font-mono text-slate-200">{endElement}</span> of{' '}
        <span className="font-mono font-semibold text-slate-200">
          {formatNumber(pageData.totalElements)}
        </span>{' '}
        {itemLabel}
      </div>

      <div className="flex items-center space-x-2">
        <button
          type="button"
          disabled={disabled || isFirst}
          onClick={() => onPageChange(currentPage - 1)}
          className="rounded border border-slate-700 bg-slate-900 px-3 py-1.5 text-xs font-medium text-slate-200 transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-40"
        >
          Previous
        </button>

        <span className="font-mono text-slate-300">
          Page {currentPage + 1} of {Math.max(1, totalPages)}
        </span>

        <button
          type="button"
          disabled={disabled || isLast}
          onClick={() => onPageChange(currentPage + 1)}
          className="rounded border border-slate-700 bg-slate-900 px-3 py-1.5 text-xs font-medium text-slate-200 transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-40"
        >
          Next
        </button>
      </div>
    </div>
  );
};
