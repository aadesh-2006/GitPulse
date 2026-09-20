import React from 'react';
import { RepositoryFileResponse } from '../../types/file';
import { formatNumber } from '../../utils/formatters';

interface RiskSummaryProps {
  files: RepositoryFileResponse[];
  totalElements?: number;
  isLoading: boolean;
}

export const RiskSummary: React.FC<RiskSummaryProps> = ({
  files,
  totalElements,
  isLoading,
}) => {
  if (isLoading || files.length === 0) {
    return null;
  }

  const sampleSize = files.length;
  const highCompositeCount = files.filter((f) => (f.compositeScore ?? 0) >= 0.6).length;
  const sumComposite = files.reduce((acc, f) => acc + (f.compositeScore ?? 0), 0);
  const sumBaseline = files.reduce((acc, f) => acc + (f.baselineScore ?? 0), 0);
  const avgComposite = sampleSize > 0 ? sumComposite / sampleSize : 0;
  const avgBaseline = sampleSize > 0 ? sumBaseline / sampleSize : 0;
  const avgDelta = avgComposite - avgBaseline;

  return (
    <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5">
      <div className="rounded-xl border border-slate-800 bg-slate-900/80 p-4 shadow-sm backdrop-blur">
        <div className="text-[11px] font-medium uppercase text-slate-400">
          Sample Files Analyzed
        </div>
        <div className="mt-1 font-mono text-xl font-bold text-slate-100">
          {formatNumber(sampleSize)}
          {totalElements && totalElements > sampleSize && (
            <span className="text-xs text-slate-500 font-normal ml-1">
              / {formatNumber(totalElements)}
            </span>
          )}
        </div>
        <div className="mt-0.5 text-[10px] text-slate-500">Loaded dataset sample</div>
      </div>

      <div className="rounded-xl border border-slate-800 bg-slate-900/80 p-4 shadow-sm backdrop-blur">
        <div className="text-[11px] font-medium uppercase text-slate-400">
          Sample High-Score Files
        </div>
        <div className="mt-1 font-mono text-xl font-bold text-blue-400">
          {formatNumber(highCompositeCount)}
        </div>
        <div className="mt-0.5 text-[10px] text-slate-500">Files with composite score ≥ 0.60</div>
      </div>

      <div className="rounded-xl border border-slate-800 bg-slate-900/80 p-4 shadow-sm backdrop-blur">
        <div className="text-[11px] font-medium uppercase text-slate-400">
          Sample Avg Composite
        </div>
        <div className="mt-1 font-mono text-xl font-bold text-slate-100">
          {avgComposite.toFixed(2)}
        </div>
        <div className="mt-0.5 text-[10px] text-slate-500">Multidimensional formula</div>
      </div>

      <div className="rounded-xl border border-slate-800 bg-slate-900/80 p-4 shadow-sm backdrop-blur">
        <div className="text-[11px] font-medium uppercase text-slate-400">
          Sample Avg Baseline
        </div>
        <div className="mt-1 font-mono text-xl font-bold text-slate-200">
          {avgBaseline.toFixed(2)}
        </div>
        <div className="mt-0.5 text-[10px] text-slate-500">Frequency-only formula</div>
      </div>

      <div className="rounded-xl border border-slate-800 bg-slate-900/80 p-4 shadow-sm backdrop-blur">
        <div className="text-[11px] font-medium uppercase text-slate-400">
          Sample Score Shift (Δ)
        </div>
        <div className="mt-1 font-mono text-xl font-bold text-slate-300">
          {avgDelta >= 0 ? `+${avgDelta.toFixed(2)}` : avgDelta.toFixed(2)}
        </div>
        <div className="mt-0.5 text-[10px] text-slate-500">Avg (Composite − Baseline)</div>
      </div>
    </div>
  );
};
