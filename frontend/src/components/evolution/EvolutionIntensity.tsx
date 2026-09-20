import React from 'react';
import { RepositoryEvolutionIntensityMetrics } from '../../types/evolution';

interface EvolutionIntensityProps {
  intensity: RepositoryEvolutionIntensityMetrics | null;
}

export const EvolutionIntensity: React.FC<EvolutionIntensityProps> = ({ intensity }) => {
  const avgChurn = intensity?.averageChurnPerCommit ?? 0;
  const avgFiles = intensity?.averageFilesChangedPerCommit ?? 0;
  const avgAdditions = intensity?.averageAdditionsPerCommit ?? 0;
  const avgDeletions = intensity?.averageDeletionsPerCommit ?? 0;

  return (
    <div className="rounded-lg border border-slate-800 bg-slate-900/90 p-6 shadow-sm">
      <div className="pb-4 border-b border-slate-800/80">
        <h2 className="text-base font-semibold text-white">Engineering Intensity</h2>
        <p className="text-xs text-slate-400">
          Normalized activity metrics and change velocity per commit
        </p>
      </div>

      <div className="mt-4 grid grid-cols-2 gap-4 sm:grid-cols-4">
        <div className="rounded-md border border-slate-800/60 bg-slate-950/40 p-4">
          <span className="text-xs font-medium text-slate-400">Avg Churn / Commit</span>
          <div className="mt-1 text-2xl font-bold font-mono text-purple-400">
            {avgChurn.toFixed(1)}
          </div>
          <span className="text-[11px] text-slate-500">lines changed per commit</span>
        </div>

        <div className="rounded-md border border-slate-800/60 bg-slate-950/40 p-4">
          <span className="text-xs font-medium text-slate-400">Avg Files / Commit</span>
          <div className="mt-1 text-2xl font-bold font-mono text-amber-400">
            {avgFiles.toFixed(1)}
          </div>
          <span className="text-[11px] text-slate-500">files touched per commit</span>
        </div>

        <div className="rounded-md border border-slate-800/60 bg-slate-950/40 p-4">
          <span className="text-xs font-medium text-slate-400">Avg Additions / Commit</span>
          <div className="mt-1 text-2xl font-bold font-mono text-emerald-400">
            {avgAdditions.toFixed(1)}
          </div>
          <span className="text-[11px] text-slate-500">lines added per commit</span>
        </div>

        <div className="rounded-md border border-slate-800/60 bg-slate-950/40 p-4">
          <span className="text-xs font-medium text-slate-400">Avg Deletions / Commit</span>
          <div className="mt-1 text-2xl font-bold font-mono text-rose-400">
            {avgDeletions.toFixed(1)}
          </div>
          <span className="text-[11px] text-slate-500">lines removed per commit</span>
        </div>
      </div>
    </div>
  );
};
