import React, { useState } from 'react';
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
} from 'recharts';
import { RepositoryMonthlyEvolutionBucketResponse } from '../../types/evolution';
import { formatMonthLabel } from '../../utils/dateRanges';
import { formatNumber } from '../../utils/formatters';

export type ActivityMetricKey =
  | 'totalCommits'
  | 'totalChurn'
  | 'totalAdditions'
  | 'totalDeletions'
  | 'filesChanged'
  | 'activeContributors';

interface EvolutionActivityChartProps {
  buckets: RepositoryMonthlyEvolutionBucketResponse[];
}

const metricConfig: Record<
  ActivityMetricKey,
  { label: string; color: string; barColor: string }
> = {
  totalCommits: { label: 'Commits', color: '#3b82f6', barColor: '#3b82f6' },
  totalChurn: { label: 'Churn', color: '#8b5cf6', barColor: '#8b5cf6' },
  totalAdditions: { label: 'Additions', color: '#10b981', barColor: '#10b981' },
  totalDeletions: { label: 'Deletions', color: '#ef4444', barColor: '#ef4444' },
  filesChanged: { label: 'Files Changed', color: '#f59e0b', barColor: '#f59e0b' },
  activeContributors: { label: 'Active Contributors', color: '#06b6d4', barColor: '#06b6d4' },
};

interface TooltipPayloadItem {
  value: number;
  dataKey: string;
}

interface CustomTooltipProps {
  active?: boolean;
  payload?: TooltipPayloadItem[];
  label?: string;
  metricLabel: string;
}

const CustomTooltip: React.FC<CustomTooltipProps> = ({
  active,
  payload,
  label,
  metricLabel,
}) => {
  if (active && payload && payload.length) {
    return (
      <div className="rounded-md border border-slate-700 bg-slate-900/95 p-3 text-xs shadow-lg backdrop-blur">
        <p className="font-semibold text-slate-200">{label}</p>
        <p className="mt-1 font-mono text-blue-400">
          {metricLabel}: <strong>{formatNumber(payload[0].value)}</strong>
        </p>
      </div>
    );
  }
  return null;
};

export const EvolutionActivityChart: React.FC<EvolutionActivityChartProps> = ({ buckets }) => {
  const [activeMetric, setActiveMetric] = useState<ActivityMetricKey>('totalCommits');

  const chartData = buckets.map((b) => ({
    rawMonth: b.month,
    formattedMonth: formatMonthLabel(b.month),
    totalCommits: b.totalCommits,
    totalChurn: b.totalChurn,
    totalAdditions: b.totalAdditions,
    totalDeletions: b.totalDeletions,
    filesChanged: b.filesChanged,
    activeContributors: b.activeContributors,
  }));

  const config = metricConfig[activeMetric];

  return (
    <div className="rounded-lg border border-slate-800 bg-slate-900/90 p-6 shadow-sm">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between pb-4 border-b border-slate-800/80">
        <div>
          <h2 className="text-base font-semibold text-white">Monthly Engineering Activity</h2>
          <p className="text-xs text-slate-400">
            Historical trend of engineering output by calendar month
          </p>
        </div>

        {/* Metric Switcher */}
        <div className="flex flex-wrap gap-1 rounded-md border border-slate-800 bg-slate-950 p-1">
          {(Object.keys(metricConfig) as ActivityMetricKey[]).map((key) => (
            <button
              key={key}
              type="button"
              onClick={() => setActiveMetric(key)}
              className={`rounded px-2.5 py-1 text-xs font-medium transition ${
                activeMetric === key
                  ? 'bg-blue-600 text-white font-semibold shadow-sm'
                  : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
              }`}
            >
              {metricConfig[key].label}
            </button>
          ))}
        </div>
      </div>

      {chartData.length === 0 ? (
        <div className="flex h-64 items-center justify-center text-xs text-slate-500 italic">
          No monthly data available for the requested range.
        </div>
      ) : (
        <div className="mt-6 h-72 w-full">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart
              data={chartData}
              margin={{ top: 10, right: 10, left: -20, bottom: 0 }}
            >
              <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" vertical={false} />
              <XAxis
                dataKey="formattedMonth"
                stroke="#64748b"
                fontSize={11}
                tickLine={false}
                axisLine={{ stroke: '#334155' }}
              />
              <YAxis
                stroke="#64748b"
                fontSize={11}
                tickLine={false}
                axisLine={{ stroke: '#334155' }}
                tickFormatter={(val) => (val >= 1000 ? `${(val / 1000).toFixed(0)}k` : val)}
              />
              <Tooltip
                content={
                  <CustomTooltip
                    metricLabel={config.label}
                  />
                }
              />
              <Bar
                dataKey={activeMetric}
                fill={config.barColor}
                radius={[4, 4, 0, 0]}
                maxBarSize={48}
              />
            </BarChart>
          </ResponsiveContainer>
        </div>
      )}
    </div>
  );
};
