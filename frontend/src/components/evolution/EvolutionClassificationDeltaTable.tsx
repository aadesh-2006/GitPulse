import React from 'react';
import { RepositoryEvolutionComparisonResponse } from '../../types/evolution';
import { formatNumber } from '../../utils/formatters';

interface EvolutionClassificationDeltaTableProps {
  comparison: RepositoryEvolutionComparisonResponse | null;
}

interface ClassificationDeltaRow {
  key: string;
  name: string;
  current: number;
  previous: number;
  delta: number;
}

export const EvolutionClassificationDeltaTable: React.FC<
  EvolutionClassificationDeltaTableProps
> = ({ comparison }) => {
  const current = comparison?.currentPeriod;
  const previous = comparison?.previousPeriod;
  const delta = comparison?.delta;

  const rows: ClassificationDeltaRow[] = [
    {
      key: 'FEATURE',
      name: 'FEATURE',
      current: current?.featureCommits ?? 0,
      previous: previous?.featureCommits ?? 0,
      delta: delta?.featureCommits ?? 0,
    },
    {
      key: 'BUG_FIX',
      name: 'BUG_FIX',
      current: current?.bugFixCommits ?? 0,
      previous: previous?.bugFixCommits ?? 0,
      delta: delta?.bugFixCommits ?? 0,
    },
    {
      key: 'REFACTOR',
      name: 'REFACTOR',
      current: current?.refactorCommits ?? 0,
      previous: previous?.refactorCommits ?? 0,
      delta: delta?.refactorCommits ?? 0,
    },
    {
      key: 'DOCUMENTATION',
      name: 'DOCUMENTATION',
      current: current?.documentationCommits ?? 0,
      previous: previous?.documentationCommits ?? 0,
      delta: delta?.documentationCommits ?? 0,
    },
    {
      key: 'TEST',
      name: 'TEST',
      current: current?.testCommits ?? 0,
      previous: previous?.testCommits ?? 0,
      delta: delta?.testCommits ?? 0,
    },
    {
      key: 'BUILD',
      name: 'BUILD',
      current: current?.buildCommits ?? 0,
      previous: previous?.buildCommits ?? 0,
      delta: delta?.buildCommits ?? 0,
    },
    {
      key: 'CONFIGURATION',
      name: 'CONFIGURATION',
      current: current?.configurationCommits ?? 0,
      previous: previous?.configurationCommits ?? 0,
      delta: delta?.configurationCommits ?? 0,
    },
    {
      key: 'DEPENDENCY',
      name: 'DEPENDENCY',
      current: current?.dependencyCommits ?? 0,
      previous: previous?.dependencyCommits ?? 0,
      delta: delta?.dependencyCommits ?? 0,
    },
    {
      key: 'OTHER',
      name: 'OTHER',
      current: current?.otherCommits ?? 0,
      previous: previous?.otherCommits ?? 0,
      delta: delta?.otherCommits ?? 0,
    },
  ];

  const formatDelta = (val: number): string => {
    if (val > 0) return `+${formatNumber(val)}`;
    if (val < 0) return `${formatNumber(val)}`;
    return '0';
  };

  return (
    <div className="rounded-lg border border-slate-800 bg-slate-900/90 p-6 shadow-sm">
      <div className="pb-4 border-b border-slate-800/80">
        <h2 className="text-base font-semibold text-white">Classification Category Deltas</h2>
        <p className="text-xs text-slate-400">
          Categorized commit volume changes between current and previous periods
        </p>
      </div>

      <div className="mt-4 overflow-x-auto">
        <table className="w-full text-left text-xs font-mono">
          <thead className="border-b border-slate-800 text-slate-400 uppercase tracking-wider text-[11px]">
            <tr>
              <th className="py-2.5 px-3">Classification</th>
              <th className="py-2.5 px-3 text-right">Current Period</th>
              <th className="py-2.5 px-3 text-right">Previous Period</th>
              <th className="py-2.5 px-3 text-right">Delta</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800/60">
            {rows.map((row) => (
              <tr key={row.key} className="hover:bg-slate-800/30">
                <td className="py-2.5 px-3 font-semibold text-slate-200">{row.name}</td>
                <td className="py-2.5 px-3 text-right text-slate-300">
                  {formatNumber(row.current)}
                </td>
                <td className="py-2.5 px-3 text-right text-slate-400">
                  {formatNumber(row.previous)}
                </td>
                <td className="py-2.5 px-3 text-right font-semibold">
                  <span
                    className={`rounded px-1.5 py-0.5 text-xs ${
                      row.delta !== 0
                        ? 'bg-slate-800 text-slate-200 border border-slate-700'
                        : 'text-slate-500'
                    }`}
                  >
                    {formatDelta(row.delta)}
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};
