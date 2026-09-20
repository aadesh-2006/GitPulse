import React from 'react';
import { RepositoryResponse } from '../types/repository';
import { useAnalysisJob } from '../hooks/useAnalysisJob';
import { AnalysisJobStatusBadge } from '../components/repository/AnalysisJobStatusBadge';
import { Button } from '../components/common/Button';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { ErrorMessage } from '../components/common/ErrorMessage';
import { EmptyState } from '../components/common/EmptyState';
import { formatDate, formatRelativeTime, formatNumber } from '../utils/formatters';

interface RepositoryOverviewPageProps {
  repository: RepositoryResponse | null;
  isLoadingRepository: boolean;
  repositoryError: string | null;
  onRefreshRepository?: () => void;
}

export const RepositoryOverviewPage: React.FC<RepositoryOverviewPageProps> = ({
  repository,
  isLoadingRepository,
  repositoryError,
  onRefreshRepository,
}) => {
  const {
    jobs,
    latestJob,
    isLoading: isLoadingJobs,
    isTriggering,
    isPolling,
    error: jobError,
    triggerAnalysis,
    refreshJobs,
  } = useAnalysisJob(repository ? repository.id : null);

  if (isLoadingRepository) {
    return (
      <div className="flex h-96 items-center justify-center">
        <LoadingSpinner size="lg" label="Loading repository intelligence..." />
      </div>
    );
  }

  if (repositoryError) {
    return (
      <div className="max-w-2xl mx-auto mt-8">
        <ErrorMessage
          title="Failed to load repository"
          message={repositoryError}
          onRetry={onRefreshRepository}
        />
      </div>
    );
  }

  if (!repository) {
    return (
      <div className="max-w-2xl mx-auto mt-12">
        <EmptyState
          title="No Repository Selected"
          description="Register or select a repository from the header dropdown to view its intelligence overview and run analysis jobs."
        />
      </div>
    );
  }

  const isJobRunning = latestJob?.status === 'PENDING' || latestJob?.status === 'RUNNING';

  return (
    <div className="space-y-6">
      {/* Repository Header Info Card */}
      <div className="rounded-lg border border-slate-800 bg-slate-900/90 p-6 shadow-sm">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
          <div className="space-y-1.5">
            <div className="flex items-center space-x-3 flex-wrap gap-y-1">
              <h1 className="text-xl font-bold tracking-tight text-white font-mono sm:text-2xl">
                {repository.fullName}
              </h1>
              <span
                className={`rounded px-2 py-0.5 text-xs font-semibold ${
                  repository.private
                    ? 'bg-amber-950/60 text-amber-400 border border-amber-800/60'
                    : 'bg-emerald-950/60 text-emerald-400 border border-emerald-800/60'
                }`}
              >
                {repository.private ? 'Private' : 'Public'}
              </span>
              {repository.primaryLanguage && (
                <span className="rounded bg-slate-800 px-2 py-0.5 text-xs font-medium text-slate-300 border border-slate-700">
                  {repository.primaryLanguage}
                </span>
              )}
            </div>

            <p className="text-sm text-slate-400 max-w-3xl">
              {repository.description || 'No description provided for this repository.'}
            </p>

            <div className="flex items-center space-x-4 pt-1 text-xs text-slate-400 flex-wrap gap-y-1">
              {repository.htmlUrl && (
                <a
                  href={repository.htmlUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex items-center text-blue-400 hover:text-blue-300 transition-colors"
                >
                  <svg
                    className="mr-1 h-3.5 w-3.5"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth="2"
                      d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14"
                    />
                  </svg>
                  GitHub Repository
                </a>
              )}
              <span>Default branch: <strong className="text-slate-300 font-mono">{repository.defaultBranch}</strong></span>
              <span>Last pushed: <strong className="text-slate-300">{formatRelativeTime(repository.pushedAt)}</strong></span>
            </div>
          </div>

          <div className="flex items-center space-x-3">
            <Button
              variant="primary"
              size="md"
              isLoading={isTriggering || isJobRunning}
              onClick={triggerAnalysis}
              icon={
                <svg
                  className="h-4 w-4"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth="2"
                    d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z"
                  />
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth="2"
                    d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
                  />
                </svg>
              }
            >
              {isJobRunning ? 'Analysis Running...' : 'Run Analysis'}
            </Button>
          </div>
        </div>

        {/* GitHub Stats Grid */}
        <div className="mt-6 grid grid-cols-2 gap-4 border-t border-slate-800/80 pt-4 sm:grid-cols-4">
          <div className="space-y-0.5">
            <div className="text-xs text-slate-500">Stars</div>
            <div className="text-lg font-semibold font-mono text-slate-200">
              {formatNumber(repository.starsCount)}
            </div>
          </div>
          <div className="space-y-0.5">
            <div className="text-xs text-slate-500">Forks</div>
            <div className="text-lg font-semibold font-mono text-slate-200">
              {formatNumber(repository.forksCount)}
            </div>
          </div>
          <div className="space-y-0.5">
            <div className="text-xs text-slate-500">Open Issues</div>
            <div className="text-lg font-semibold font-mono text-slate-200">
              {formatNumber(repository.openIssuesCount)}
            </div>
          </div>
          <div className="space-y-0.5">
            <div className="text-xs text-slate-500">Registered In GitPulse</div>
            <div className="text-xs font-mono text-slate-300 pt-1">
              {formatDate(repository.createdAt)}
            </div>
          </div>
        </div>
      </div>

      {/* Analysis Error Alert */}
      {jobError && (
        <ErrorMessage
          title="Analysis Pipeline Notice"
          message={jobError}
          onRetry={refreshJobs}
        />
      )}

      {/* Latest Analysis Status Card */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        <div className="rounded-lg border border-slate-800 bg-slate-900/90 p-6 shadow-sm lg:col-span-2">
          <div className="flex items-center justify-between pb-4 border-b border-slate-800/80">
            <div>
              <h2 className="text-base font-semibold text-white">Latest Analysis Status</h2>
              <p className="text-xs text-slate-400">
                Current pipeline status and attribution state
              </p>
            </div>
            {latestJob && (
              <AnalysisJobStatusBadge status={latestJob.status} isPolling={isPolling} />
            )}
          </div>

          {isLoadingJobs && jobs.length === 0 ? (
            <div className="py-8">
              <LoadingSpinner size="md" label="Checking analysis history..." />
            </div>
          ) : latestJob ? (
            <div className="mt-4 space-y-4">
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
                <div className="rounded-md border border-slate-800/60 bg-slate-950/40 p-3">
                  <span className="text-xs text-slate-500">Job ID</span>
                  <p className="font-mono text-sm font-medium text-slate-200">#{latestJob.id}</p>
                </div>
                <div className="rounded-md border border-slate-800/60 bg-slate-950/40 p-3">
                  <span className="text-xs text-slate-500">Started At</span>
                  <p className="text-sm font-medium text-slate-200">
                    {formatDate(latestJob.startedAt)}
                  </p>
                </div>
                <div className="rounded-md border border-slate-800/60 bg-slate-950/40 p-3">
                  <span className="text-xs text-slate-500">Completed At</span>
                  <p className="text-sm font-medium text-slate-200">
                    {formatDate(latestJob.completedAt)}
                  </p>
                </div>
              </div>

              {latestJob.errorMessage && (
                <div className="rounded-md border border-rose-900/40 bg-rose-950/20 p-3 text-xs text-rose-300">
                  <strong className="font-semibold">Error Detail: </strong>
                  <span>{latestJob.errorMessage}</span>
                </div>
              )}

              {isPolling && (
                <div className="flex items-center space-x-2 text-xs text-blue-400">
                  <span className="h-2 w-2 animate-ping rounded-full bg-blue-400" />
                  <span>Polling pipeline updates (every 2.5s)...</span>
                </div>
              )}
            </div>
          ) : (
            <div className="mt-4">
              <EmptyState
                title="No Analysis Run Yet"
                description="Trigger an analysis job to ingest commit history, classify changes, aggregate churn, and generate intelligence metrics."
                actionLabel="Trigger First Analysis"
                onAction={triggerAnalysis}
              />
            </div>
          )}
        </div>

        {/* Quick Analytics Readiness Overview Card */}
        <div className="rounded-lg border border-slate-800 bg-slate-900/90 p-6 shadow-sm">
          <h2 className="text-base font-semibold text-white">Analytics Modules</h2>
          <p className="text-xs text-slate-400 mb-4">
            Engineered analytical capabilities
          </p>

          <ul className="space-y-3 text-xs">
            <li className="flex items-start justify-between rounded-md border border-slate-800/60 bg-slate-950/30 p-2.5">
              <div>
                <span className="font-medium text-slate-200">Evolution Intelligence</span>
                <p className="text-slate-500 text-[11px]">Monthly timelines & period comparisons</p>
              </div>
              <span className="rounded bg-emerald-950/70 text-emerald-400 border border-emerald-800/50 px-1.5 py-0.5 text-[10px] font-semibold">
                API Ready
              </span>
            </li>
            <li className="flex items-start justify-between rounded-md border border-slate-800/60 bg-slate-950/30 p-2.5">
              <div>
                <span className="font-medium text-slate-200">File Churn & Hotspots</span>
                <p className="text-slate-500 text-[11px]">Risk scoring & primary authors</p>
              </div>
              <span className="rounded bg-emerald-950/70 text-emerald-400 border border-emerald-800/50 px-1.5 py-0.5 text-[10px] font-semibold">
                API Ready
              </span>
            </li>
            <li className="flex items-start justify-between rounded-md border border-slate-800/60 bg-slate-950/30 p-2.5">
              <div>
                <span className="font-medium text-slate-200">Commit Classification</span>
                <p className="text-slate-500 text-[11px]">Categorized activity breakdown</p>
              </div>
              <span className="rounded bg-emerald-950/70 text-emerald-400 border border-emerald-800/50 px-1.5 py-0.5 text-[10px] font-semibold">
                API Ready
              </span>
            </li>
            <li className="flex items-start justify-between rounded-md border border-slate-800/60 bg-slate-950/30 p-2.5">
              <div>
                <span className="font-medium text-slate-200">Contributor Ownership</span>
                <p className="text-slate-500 text-[11px]">File concentration & author share</p>
              </div>
              <span className="rounded bg-emerald-950/70 text-emerald-400 border border-emerald-800/50 px-1.5 py-0.5 text-[10px] font-semibold">
                API Ready
              </span>
            </li>
          </ul>
        </div>
      </div>

      {/* Analysis Job History Table (if multiple jobs) */}
      {jobs.length > 1 && (
        <div className="rounded-lg border border-slate-800 bg-slate-900/90 p-6 shadow-sm">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-base font-semibold text-white">Recent Analysis History</h2>
            <Button size="sm" variant="outline" onClick={refreshJobs}>
              Refresh History
            </Button>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="border-b border-slate-800 text-slate-400 uppercase tracking-wider">
                <tr>
                  <th className="py-2.5 px-3">Job ID</th>
                  <th className="py-2.5 px-3">Status</th>
                  <th className="py-2.5 px-3">Started</th>
                  <th className="py-2.5 px-3">Completed</th>
                  <th className="py-2.5 px-3">Created</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60 font-mono">
                {jobs.slice(0, 5).map((job) => (
                  <tr key={job.id} className="hover:bg-slate-800/30">
                    <td className="py-2.5 px-3 font-semibold text-slate-200">#{job.id}</td>
                    <td className="py-2.5 px-3">
                      <AnalysisJobStatusBadge status={job.status} />
                    </td>
                    <td className="py-2.5 px-3 text-slate-400">{formatDate(job.startedAt)}</td>
                    <td className="py-2.5 px-3 text-slate-400">{formatDate(job.completedAt)}</td>
                    <td className="py-2.5 px-3 text-slate-400">{formatDate(job.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
};
