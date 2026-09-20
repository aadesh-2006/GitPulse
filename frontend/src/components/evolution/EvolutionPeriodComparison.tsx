import React from 'react';
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
  Legend,
} from 'recharts';
import { RepositoryEvolutionComparisonResponse } from '../../types/evolution';
import { formatNumber } from '../../utils/formatters';

interface EvolutionPeriodComparisonProps {
  comparison: RepositoryEvolutionComparisonResponse | null;
}

interface ComparisonMetricItem {
  key: string;
  label: string;
  current: number;
  previous: number;
  delta: number;
}

export const EvolutionPeriodComparison: React.FC<EvolutionPeriodComparisonProps> = ({
  comparison,
}) => {
  const current = comparison?.currentPeriod;
  const previous = comparison?.previousPeriod;
  const delta = comparison?.delta;

  const primaryMetrics: ComparisonMetricItem[] = [
    {
      key: 'commits',
      label: 'Commits',
      current: current?.totalCommits ?? 0,
      previous: previous?.totalCommits ?? 0,
      delta: delta?.totalCommits ?? 0,
    },
    {
      key: 'churn',
      label: 'Total Churn',
      current: current?.totalChurn ?? 0,
      previous: previous?.totalChurn ?? 0,
      delta: delta?.totalChurn ?? 0,
    },
    {
      key: 'files',
      label: 'Files Changed',
      current: current?.filesChanged ?? 0,
      previous: previous?.filesChanged ?? 0,
      delta: delta?.filesChanged ?? 0,
    },
    {
      key: 'contributors',
      label: 'Active Contributors',
      current: current?.activeContributors ?? 0,
      previous: previous?.activeContributors ?? 0,
      delta: delta?.activeContributors ?? 0,
    },
  ];

  const chartData = primaryMetrics.map((m) => ({
    name: m.label,
    Current: m.current,
    Previous: m.previous,
  }));

  const formatDelta = (val: number): string => {
    if (val > 0) return `+${formatNumber(val)}`;
    if (val < 0) return `${formatNumber(val)}`;
    return '0';
  };

  return (
    <div className="rounded-lg border border-slate-800 bg-slate-900/90 p-6 shadow-sm">
      <div className="flex flex-col gap-1 pb-4 border-b border-slate-800/80">
        <h2 className="text-base font-semibold text-white">Period-over-Period Comparison</h2>
        <p className="text-xs text-slate-400">
          Comparison between the requested current window and the immediately preceding historical period
        </p>
      </div>

      {/* Side by side comparison cards */}
      <div className="mt-4 grid grid-cols-2 gap-3 sm:grid-cols-4">
        {primaryMetrics.map((item) => (
          <div
            key={item.key}
            className="rounded-md border border-slate-800/60 bg-slate-950/40 p-3 space-y-1.5"
          >
            <span className="text-xs font-medium text-slate-400">{item.label}</span>
            <div className="flex items-baseline justify-between font-mono">
              <span className="text-lg font-bold text-white">
                {formatNumber(item.current)}
              </span>
              <span
                className={`rounded px-1.5 py-0.2 text-xs font-semibold ${
                  item.delta !== 0
                    ? 'bg-slate-800 text-slate-200 border border-slate-700'
                    : 'bg-slate-900 text-slate-500'
                }`}
              >
                {formatDelta(item.delta)}
              </span>
            </div>
            <div className="text-[11px] text-slate-500 font-mono">
              Prev: {formatNumber(item.previous)}
            </div>
          </div>
        ))}
      </div>

      {/* Side-by-side Bar Chart */}
      <div className="mt-6 h-64 w-full">
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={chartData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" vertical={false} />
            <XAxis dataKey="name" stroke="#64748b" fontSize={11} tickLine={false} />
            <YAxis
              stroke="#64748b"
              fontSize={11}
              tickLine={false}
              tickFormatter={(val) => (val >= 1000 ? `${(val / 1000).toFixed(0)}k` : val)}
            />
            <Tooltip
              content={({ active, payload, label }) => {
                if (active && payload && payload.length) {
                  return (
                    <div className="rounded-md border border-slate-700 bg-slate-900/95 p-2.5 text-xs shadow-lg backdrop-blur">
                      <p className="font-semibold text-slate-200 mb-1">{label}</p>
                      {payload.map((entry, idx) => (
                        <p key={idx} className="font-mono text-slate-300" style={{ color: entry.color }}>
                          {entry.name}: <strong>{formatNumber(entry.value as number)}</strong>
                        </p>
                      ))}
                    </div>
                  );
                }
                return null;
              }}
            />
            <Legend
              wrapperStyle={{ fontSize: '11px', paddingTop: '10px' }}
              iconSize={10}
            />
            <Bar dataKey="Current" fill="#3b82f6" radius={[3, 3, 0, 0]} maxBarSize={32} />
            <Bar dataKey="Previous" fill="#64748b" radius={[3, 3, 0, 0]} maxBarSize={32} />
          </BarChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
};
