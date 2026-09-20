import React from 'react';
import { RepositoryEvolutionCompositionResponse } from '../../types/evolution';
import { formatNumber } from '../../utils/formatters';

interface EvolutionSummaryCardsProps {
  composition: RepositoryEvolutionCompositionResponse | null;
}

export const EvolutionSummaryCards: React.FC<EvolutionSummaryCardsProps> = ({ composition }) => {
  const raw = composition?.rawMetrics;
  const comp = composition?.composition;

  const totalCommits = raw?.totalCommits ?? 0;
  const totalChurn = raw?.totalChurn ?? 0;
  const filesChanged = raw?.filesChanged ?? 0;
  const activeContributors = raw?.activeContributors ?? 0;
  const classifiedCommits = comp?.classifiedCommits ?? 0;
  const unclassifiedCommits = comp?.unclassifiedCommits ?? 0;

  return (
    <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-6">
      <div className="rounded-lg border border-slate-800 bg-slate-900/90 p-4 shadow-sm">
        <span className="text-xs font-medium text-slate-400">Total Commits</span>
        <div className="mt-1 text-2xl font-bold font-mono text-white">
          {formatNumber(totalCommits)}
        </div>
      </div>

      <div className="rounded-lg border border-slate-800 bg-slate-900/90 p-4 shadow-sm">
        <span className="text-xs font-medium text-slate-400">Total Churn</span>
        <div className="mt-1 text-2xl font-bold font-mono text-slate-100">
          {formatNumber(totalChurn)}
        </div>
        <span className="text-[11px] text-slate-500 font-mono">
          +{formatNumber(raw?.totalAdditions ?? 0)} / -{formatNumber(raw?.totalDeletions ?? 0)}
        </span>
      </div>

      <div className="rounded-lg border border-slate-800 bg-slate-900/90 p-4 shadow-sm">
        <span className="text-xs font-medium text-slate-400">Files Changed</span>
        <div className="mt-1 text-2xl font-bold font-mono text-slate-100">
          {formatNumber(filesChanged)}
        </div>
      </div>

      <div className="rounded-lg border border-slate-800 bg-slate-900/90 p-4 shadow-sm">
        <span className="text-xs font-medium text-slate-400">Active Contributors</span>
        <div className="mt-1 text-2xl font-bold font-mono text-slate-100">
          {formatNumber(activeContributors)}
        </div>
      </div>

      <div className="rounded-lg border border-slate-800 bg-slate-900/90 p-4 shadow-sm">
        <span className="text-xs font-medium text-slate-400">Classified Commits</span>
        <div className="mt-1 text-2xl font-bold font-mono text-blue-400">
          {formatNumber(classifiedCommits)}
        </div>
        {totalCommits > 0 && (
          <span className="text-[11px] text-blue-500/80 font-mono">
            {((classifiedCommits / totalCommits) * 100).toFixed(1)}% of total
          </span>
        )}
      </div>

      <div className="rounded-lg border border-slate-800 bg-slate-900/90 p-4 shadow-sm">
        <span className="text-xs font-medium text-slate-400">Unclassified</span>
        <div className="mt-1 text-2xl font-bold font-mono text-slate-400">
          {formatNumber(unclassifiedCommits)}
        </div>
        {totalCommits > 0 && (
          <span className="text-[11px] text-slate-500 font-mono">
            {((unclassifiedCommits / totalCommits) * 100).toFixed(1)}% of total
          </span>
        )}
      </div>
    </div>
  );
};
