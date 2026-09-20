import React from 'react';
import { RepositoryResponse } from '../types/repository';
import { useEvolutionData } from '../hooks/useEvolutionData';
import { EvolutionDateRangePicker } from '../components/evolution/EvolutionDateRangePicker';
import { EvolutionSummaryCards } from '../components/evolution/EvolutionSummaryCards';
import { EvolutionActivityChart } from '../components/evolution/EvolutionActivityChart';
import { EvolutionComposition } from '../components/evolution/EvolutionComposition';
import { EvolutionIntensity } from '../components/evolution/EvolutionIntensity';
import { EvolutionPeriodComparison } from '../components/evolution/EvolutionPeriodComparison';
import { EvolutionClassificationDeltaTable } from '../components/evolution/EvolutionClassificationDeltaTable';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { ErrorMessage } from '../components/common/ErrorMessage';
import { EmptyState } from '../components/common/EmptyState';
import { Button } from '../components/common/Button';
import { formatDate } from '../utils/formatters';

interface EvolutionPageProps {
  repository: RepositoryResponse | null;
}

export const EvolutionPage: React.FC<EvolutionPageProps> = ({ repository }) => {
  const {
    evolution,
    comparison,
    composition,
    dateRange,
    isLoading,
    error,
    setDateRange,
    refresh,
  } = useEvolutionData(repository ? repository.id : null);

  if (!repository) {
    return (
      <div className="max-w-2xl mx-auto mt-12">
        <EmptyState
          title="No Repository Selected"
          description="Please select a repository from the header dropdown to view its evolution intelligence analytics."
        />
      </div>
    );
  }

  const buckets = evolution?.buckets ?? [];
  const rawMetrics = composition?.rawMetrics;
  const totalCommits = rawMetrics?.totalCommits ?? 0;
  const hasNoActivity = !isLoading && buckets.length > 0 && totalCommits === 0;

  return (
    <div className="space-y-6">
      {/* Page Header with Context & Controls */}
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between rounded-lg border border-slate-800 bg-slate-900/90 p-5 shadow-sm">
        <div>
          <div className="flex items-center space-x-2">
            <h1 className="text-xl font-bold tracking-tight text-white font-mono sm:text-2xl">
              Repository Evolution Intelligence
            </h1>
            <span className="rounded bg-blue-950/80 px-2 py-0.5 text-xs font-semibold text-blue-400 border border-blue-800/60">
              P7.2
            </span>
          </div>
          <p className="mt-1 text-xs text-slate-400">
            Historical activity dynamics, composition trends, intensity, and period deltas for{' '}
            <strong className="text-slate-200 font-mono">{repository.fullName}</strong>
          </p>
          <div className="mt-2 text-[11px] text-slate-500 font-mono">
            Range: {formatDate(dateRange.currentFrom)} — {formatDate(dateRange.currentTo)}
          </div>
        </div>

        <div className="flex items-center space-x-3 self-start md:self-auto">
          <EvolutionDateRangePicker
            dateRange={dateRange}
            onChange={setDateRange}
            disabled={isLoading}
          />
          <Button
            variant="outline"
            size="sm"
            onClick={refresh}
            isLoading={isLoading}
            title="Refresh evolution analytics"
            icon={
              <svg
                className="h-3.5 w-3.5"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth="2"
                  d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"
                />
              </svg>
            }
          >
            Refresh
          </Button>
        </div>
      </div>

      {/* Error Banner */}
      {error && (
        <ErrorMessage
          title="Evolution Analytics Notice"
          message={error}
          onRetry={refresh}
        />
      )}

      {/* Loading Skeleton/Spinner */}
      {isLoading && !evolution && (
        <div className="flex h-96 items-center justify-center rounded-lg border border-slate-800 bg-slate-900/60">
          <LoadingSpinner size="lg" label="Computing repository evolution intelligence..." />
        </div>
      )}

      {/* Zero activity notice banner if period has no commits */}
      {hasNoActivity && (
        <div className="rounded-lg border border-slate-800 bg-slate-900/40 p-4 text-xs text-slate-400">
          <strong className="text-slate-300">Notice:</strong> No commit activity recorded for{' '}
          <span className="font-mono text-slate-200">{repository.fullName}</span> in the selected date range. Charts and metrics display baseline values.
        </div>
      )}

      {/* Evolution Content */}
      {evolution && composition && comparison && (
        <div className="space-y-6">
          {/* Summary Row */}
          <EvolutionSummaryCards composition={composition} />

          {/* Monthly Activity Timeline Chart */}
          <EvolutionActivityChart buckets={buckets} />

          {/* Engineering Composition & Intensity Grid */}
          <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
            <EvolutionComposition composition={composition} />
            <EvolutionIntensity intensity={composition.intensity} />
          </div>

          {/* Period-over-Period Comparison */}
          <EvolutionPeriodComparison comparison={comparison} />

          {/* Classification Delta Table */}
          <EvolutionClassificationDeltaTable comparison={comparison} />
        </div>
      )}
    </div>
  );
};
