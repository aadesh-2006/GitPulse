import React from 'react';
import { RepositoryFileResponse } from '../../types/file';

interface RiskScoreBreakdownProps {
  file: RepositoryFileResponse | null;
}

export const RiskScoreBreakdown: React.FC<RiskScoreBreakdownProps> = ({ file }) => {
  if (!file) {
    return (
      <div className="rounded-xl border border-slate-800 bg-slate-900/60 p-6 text-center text-xs text-slate-500 italic">
        Select a file to inspect its deterministic score component breakdown.
      </div>
    );
  }

  const baseline = file.baselineScore ?? 0;
  const composite = file.compositeScore ?? 0;
  const delta = composite - baseline;
  const deltaFormatted = delta >= 0 ? `+${delta.toFixed(2)}` : delta.toFixed(2);

  const components = [
    {
      name: 'Revision Frequency',
      weight: '30%',
      rawScore: file.revisionFrequencyScore ?? 0,
      weightedScore: ((file.revisionFrequencyScore ?? 0) * 0.3).toFixed(3),
      description: 'Log-normalized revision count against repository peak',
    },
    {
      name: 'Code Churn',
      weight: '30%',
      rawScore: file.churnScore ?? 0,
      weightedScore: ((file.churnScore ?? 0) * 0.3).toFixed(3),
      description: 'Log-normalized added/deleted lines against repository peak',
    },
    {
      name: 'Recency',
      weight: '20%',
      rawScore: file.recencyScore ?? 0,
      weightedScore: ((file.recencyScore ?? 0) * 0.2).toFixed(3),
      description: '90-day exponential half-life decay from analysis timestamp',
    },
    {
      name: 'Ownership Concentration',
      weight: '20%',
      rawScore: file.ownershipConcentrationScore ?? 0,
      weightedScore: ((file.ownershipConcentrationScore ?? 0) * 0.2).toFixed(3),
      description: 'Top contributor revision share fraction [0.00 – 1.00]',
    },
  ];

  return (
    <div className="rounded-xl border border-slate-800 bg-slate-900/80 p-6 shadow-sm backdrop-blur space-y-5">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between border-b border-slate-800 pb-4">
        <div>
          <span className="text-[10px] font-semibold uppercase tracking-wider text-blue-400">
            Score Component Breakdown
          </span>
          <h4 className="font-mono text-sm font-bold text-slate-100 break-all mt-0.5">
            {file.filePath}
          </h4>
        </div>

        <div className="mt-3 sm:mt-0 flex items-center space-x-3">
          <div className="rounded-lg border border-slate-800 bg-slate-950/80 px-3 py-1.5 text-center">
            <div className="text-[10px] text-slate-400">Baseline</div>
            <div className="font-mono font-bold text-slate-200 text-sm">
              {baseline.toFixed(2)}
            </div>
          </div>

          <div className="rounded-lg border border-blue-900/60 bg-blue-950/40 px-3 py-1.5 text-center">
            <div className="text-[10px] text-blue-300">Composite</div>
            <div className="font-mono font-bold text-blue-400 text-sm">
              {composite.toFixed(2)}
            </div>
          </div>

          <div className="rounded-lg border border-slate-800 bg-slate-950/80 px-3 py-1.5 text-center">
            <div className="text-[10px] text-slate-400">Difference (Δ)</div>
            <div className="font-mono font-bold text-slate-300 text-sm">
              {deltaFormatted}
            </div>
          </div>
        </div>
      </div>

      {/* Component breakdown bars */}
      <div className="space-y-4 text-xs">
        {components.map((comp) => {
          const clamped = Math.max(0, Math.min(1, comp.rawScore));
          const pct = Math.round(clamped * 100);

          return (
            <div key={comp.name} className="space-y-1.5">
              <div className="flex items-center justify-between text-slate-300">
                <div className="flex items-center space-x-2">
                  <span className="font-medium text-slate-200">{comp.name}</span>
                  <span className="rounded bg-slate-800 px-1.5 py-0.2 text-[10px] font-mono text-slate-400">
                    weight {comp.weight}
                  </span>
                </div>
                <div className="font-mono text-xs space-x-2">
                  <span className="text-slate-400">raw: <span className="text-slate-200 font-semibold">{comp.rawScore.toFixed(2)}</span></span>
                  <span className="text-slate-500">|</span>
                  <span className="text-slate-400">contrib: <span className="text-blue-400 font-semibold">+{comp.weightedScore}</span></span>
                </div>
              </div>

              <div className="h-2 w-full rounded-full bg-slate-800 overflow-hidden ring-1 ring-slate-700/50">
                <div
                  className="h-full rounded-full bg-blue-500 transition-all duration-300"
                  style={{ width: `${pct}%` }}
                />
              </div>

              <div className="text-[10px] text-slate-500">{comp.description}</div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
