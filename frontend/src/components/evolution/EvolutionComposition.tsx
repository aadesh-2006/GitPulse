import React from 'react';
import { ResponsiveContainer, PieChart, Pie, Cell, Tooltip } from 'recharts';
import { RepositoryEvolutionCompositionResponse } from '../../types/evolution';
import { formatNumber } from '../../utils/formatters';

interface EvolutionCompositionProps {
  composition: RepositoryEvolutionCompositionResponse | null;
}

interface CategoryItem {
  name: string;
  key: string;
  count: number;
  share: number; // 0.0 to 1.0 (share among classified)
  color: string;
}

const CATEGORY_COLORS: Record<string, string> = {
  FEATURE: '#3b82f6', // blue
  BUG_FIX: '#ef4444', // red
  REFACTOR: '#8b5cf6', // purple
  DOCUMENTATION: '#06b6d4', // cyan
  TEST: '#10b981', // emerald
  BUILD: '#f59e0b', // amber
  CONFIGURATION: '#ec4899', // pink
  DEPENDENCY: '#6366f1', // indigo
  OTHER: '#64748b', // slate
};

export const EvolutionComposition: React.FC<EvolutionCompositionProps> = ({ composition }) => {
  const raw = composition?.rawMetrics;
  const shares = composition?.composition;

  const categories: CategoryItem[] = [
    {
      name: 'Feature',
      key: 'FEATURE',
      count: raw?.featureCommits ?? 0,
      share: shares?.featureShare ?? 0,
      color: CATEGORY_COLORS.FEATURE,
    },
    {
      name: 'Bug Fix',
      key: 'BUG_FIX',
      count: raw?.bugFixCommits ?? 0,
      share: shares?.bugFixShare ?? 0,
      color: CATEGORY_COLORS.BUG_FIX,
    },
    {
      name: 'Refactor',
      key: 'REFACTOR',
      count: raw?.refactorCommits ?? 0,
      share: shares?.refactorShare ?? 0,
      color: CATEGORY_COLORS.REFACTOR,
    },
    {
      name: 'Documentation',
      key: 'DOCUMENTATION',
      count: raw?.documentationCommits ?? 0,
      share: shares?.documentationShare ?? 0,
      color: CATEGORY_COLORS.DOCUMENTATION,
    },
    {
      name: 'Test',
      key: 'TEST',
      count: raw?.testCommits ?? 0,
      share: shares?.testShare ?? 0,
      color: CATEGORY_COLORS.TEST,
    },
    {
      name: 'Build',
      key: 'BUILD',
      count: raw?.buildCommits ?? 0,
      share: shares?.buildShare ?? 0,
      color: CATEGORY_COLORS.BUILD,
    },
    {
      name: 'Configuration',
      key: 'CONFIGURATION',
      count: raw?.configurationCommits ?? 0,
      share: shares?.configurationShare ?? 0,
      color: CATEGORY_COLORS.CONFIGURATION,
    },
    {
      name: 'Dependency',
      key: 'DEPENDENCY',
      count: raw?.dependencyCommits ?? 0,
      share: shares?.dependencyShare ?? 0,
      color: CATEGORY_COLORS.DEPENDENCY,
    },
    {
      name: 'Other',
      key: 'OTHER',
      count: raw?.otherCommits ?? 0,
      share: shares?.otherShare ?? 0,
      color: CATEGORY_COLORS.OTHER,
    },
  ];

  const classifiedCount = shares?.classifiedCommits ?? 0;
  const unclassifiedCount = shares?.unclassifiedCommits ?? 0;
  const totalCommits = raw?.totalCommits ?? 0;

  // Chart data filter only categories with count > 0
  const chartData = categories
    .filter((c) => c.count > 0)
    .map((c) => ({
      name: c.name,
      value: c.count,
      color: c.color,
      sharePct: (c.share * 100).toFixed(1),
    }));

  return (
    <div className="rounded-lg border border-slate-800 bg-slate-900/90 p-6 shadow-sm">
      <div className="flex flex-col gap-1 pb-4 border-b border-slate-800/80">
        <h2 className="text-base font-semibold text-white">Commit Composition by Category</h2>
        <p className="text-xs text-slate-400">
          Deterministic classification distribution across 9 engineering categories
        </p>
      </div>

      <div className="mt-4 grid grid-cols-1 gap-6 lg:grid-cols-5 items-center">
        {/* Donut Chart */}
        <div className="lg:col-span-2 flex flex-col items-center justify-center">
          {chartData.length === 0 ? (
            <div className="flex h-56 w-56 flex-col items-center justify-center rounded-full border border-dashed border-slate-800 bg-slate-950/40 text-center text-xs text-slate-500">
              <span>No classified commits</span>
              <span>in selected range</span>
            </div>
          ) : (
            <div className="h-56 w-56 relative">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={chartData}
                    cx="50%"
                    cy="50%"
                    innerRadius={55}
                    outerRadius={85}
                    paddingAngle={3}
                    dataKey="value"
                  >
                    {chartData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} />
                    ))}
                  </Pie>
                  <Tooltip
                    content={({ active, payload }) => {
                      if (active && payload && payload.length) {
                        const data = payload[0].payload;
                        return (
                          <div className="rounded-md border border-slate-700 bg-slate-900/95 p-2.5 text-xs shadow-lg backdrop-blur">
                            <p className="font-semibold text-slate-200">{data.name}</p>
                            <p className="mt-0.5 font-mono text-slate-300">
                              Commits: <strong>{formatNumber(data.value)}</strong>
                            </p>
                            <p className="font-mono text-slate-400">Share: {data.sharePct}%</p>
                          </div>
                        );
                      }
                      return null;
                    }}
                  />
                </PieChart>
              </ResponsiveContainer>
              <div className="absolute inset-0 flex flex-col items-center justify-center pointer-events-none">
                <span className="text-xl font-bold font-mono text-white">
                  {formatNumber(classifiedCount)}
                </span>
                <span className="text-[10px] uppercase tracking-wider text-slate-400 font-medium">
                  Classified
                </span>
              </div>
            </div>
          )}
        </div>

        {/* Categories Legend / Table */}
        <div className="lg:col-span-3 space-y-2.5">
          <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
            {categories.map((cat) => {
              const sharePct = (cat.share * 100).toFixed(1);
              return (
                <div
                  key={cat.key}
                  className="flex items-center justify-between rounded-md border border-slate-800/60 bg-slate-950/40 px-3 py-2 text-xs"
                >
                  <div className="flex items-center space-x-2">
                    <span
                      className="h-2.5 w-2.5 rounded-full"
                      style={{ backgroundColor: cat.color }}
                    />
                    <span className="font-medium text-slate-300">{cat.name}</span>
                  </div>
                  <div className="flex items-center space-x-2 font-mono">
                    <span className="text-slate-200">{formatNumber(cat.count)}</span>
                    <span className="text-slate-500 text-[11px] w-12 text-right">
                      {classifiedCount > 0 ? `${sharePct}%` : '0.0%'}
                    </span>
                  </div>
                </div>
              );
            })}
          </div>

          {/* Unclassified Callout */}
          <div className="mt-3 flex items-center justify-between rounded-md border border-slate-800 bg-slate-950/70 px-3.5 py-2 text-xs">
            <div className="flex items-center space-x-2">
              <span className="h-2.5 w-2.5 rounded-full bg-slate-600" />
              <span className="font-medium text-slate-400">Unclassified (Pending/Untagged)</span>
            </div>
            <div className="flex items-center space-x-2 font-mono">
              <span className="text-slate-300">{formatNumber(unclassifiedCount)}</span>
              <span className="text-slate-500 text-[11px] w-12 text-right">
                {totalCommits > 0
                  ? `${((unclassifiedCount / totalCommits) * 100).toFixed(1)}%`
                  : '0.0%'}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
