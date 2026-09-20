import React from 'react';

export const RiskModelExplanation: React.FC = () => {
  return (
    <div className="rounded-xl border border-slate-800 bg-slate-900/80 p-6 shadow-sm backdrop-blur space-y-4">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between border-b border-slate-800 pb-3">
        <div>
          <h3 className="text-sm font-bold uppercase tracking-wider text-slate-200">
            Deterministic Scoring Model
          </h3>
          <p className="text-xs text-slate-400">
            Mathematical formulation comparing single-dimensional revision baseline against multidimensional change signals.
          </p>
        </div>
        <span className="mt-2 sm:mt-0 inline-flex items-center rounded-md bg-blue-950/80 px-2.5 py-1 text-[11px] font-mono text-blue-300 border border-blue-800/60">
          Normalized [0.00 – 1.00]
        </span>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs">
        {/* Baseline Model */}
        <div className="rounded-lg border border-slate-800 bg-slate-950/60 p-4 space-y-2">
          <div className="flex items-center justify-between">
            <span className="font-semibold text-slate-200">Baseline Score</span>
            <span className="font-mono text-slate-400">Weight: 1.00</span>
          </div>
          <p className="text-slate-400 text-[11px]">
            Measures activity solely based on historical revision frequency.
          </p>
          <div className="rounded bg-slate-900 border border-slate-800 p-2.5 font-mono text-[11px] text-slate-300">
            <code>baselineScore = log1p(totalRevisions) / log1p(maxRepoRevisions)</code>
          </div>
        </div>

        {/* Multidimensional Composite Model */}
        <div className="rounded-lg border border-slate-800 bg-slate-950/60 p-4 space-y-2">
          <div className="flex items-center justify-between">
            <span className="font-semibold text-slate-200">Multidimensional Composite Score</span>
            <span className="font-mono text-slate-400">Sum of Weights: 1.00</span>
          </div>
          <p className="text-slate-400 text-[11px]">
            Combines frequency, volume, temporal recency, and ownership distribution.
          </p>
          <div className="rounded bg-slate-900 border border-slate-800 p-2.5 font-mono text-[11px] text-slate-300 space-y-1">
            <div><code>compositeScore = 0.30 × revisionFrequency</code></div>
            <div><code>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;+ 0.30 × churn</code></div>
            <div><code>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;+ 0.20 × recency (90d half-life)</code></div>
            <div><code>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;+ 0.20 × ownershipConcentration</code></div>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 pt-2 text-[11px] text-slate-400 border-t border-slate-800/80">
        <div>
          <span className="font-semibold text-slate-300">Revision Frequency (30%):</span>
          <p className="mt-0.5">Log-normalized revisions relative to repository max.</p>
        </div>
        <div>
          <span className="font-semibold text-slate-300">Code Churn (30%):</span>
          <p className="mt-0.5">Log-normalized additions/deletions relative to repository max.</p>
        </div>
        <div>
          <span className="font-semibold text-slate-300">Recency Decay (20%):</span>
          <p className="mt-0.5">Exponential decay with 90-day half-life: exp(-ln(2) × age / 90).</p>
        </div>
        <div>
          <span className="font-semibold text-slate-300">Ownership Share (20%):</span>
          <p className="mt-0.5">Top contributor historical revision proportion clamped [0, 1].</p>
        </div>
      </div>
    </div>
  );
};
