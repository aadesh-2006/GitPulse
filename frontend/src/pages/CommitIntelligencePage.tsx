import React from 'react';
import { RepositoryResponse } from '../types/repository';
import { useCommitIntelligence } from '../hooks/useCommitIntelligence';
import { CommitHeader } from '../components/commits/CommitHeader';
import { CommitFilters } from '../components/commits/CommitFilters';
import { CommitTable } from '../components/commits/CommitTable';
import { CommitDetailPanel } from '../components/commits/CommitDetailPanel';
import { FileDetailPanel } from '../components/files/FileDetailPanel';
import { ErrorMessage } from '../components/common/ErrorMessage';
import { EmptyState } from '../components/common/EmptyState';

interface CommitIntelligencePageProps {
  repository: RepositoryResponse | null;
  onNavigateToEvolution?: () => void;
}

export const CommitIntelligencePage: React.FC<CommitIntelligencePageProps> = ({
  repository,
  onNavigateToEvolution,
}) => {
  const {
    commitPage,
    isLoadingCommits,
    commitsError,

    filters,
    page,
    setPage,
    setClassification,
    setAuthorEmail,
    setDatePreset,
    setCustomDate,
    setSort,
    setPageSize,
    resetFilters,

    selectedCommitId,
    selectedCommitDetail,
    isLoadingDetail,
    detailError,
    selectCommit,

    selectedFilePath,
    selectedFileDetail,
    isLoadingFileDetail,
    fileDetailError,
    selectFile,

    refresh,
  } = useCommitIntelligence(repository ? repository.id : null);

  if (!repository) {
    return (
      <div className="max-w-2xl mx-auto mt-12">
        <EmptyState
          title="No Repository Selected"
          description="Please select a repository from the header dropdown to view its commit history and classification intelligence."
        />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <CommitHeader
        repository={repository}
        onRefresh={refresh}
        isLoading={isLoadingCommits || isLoadingDetail}
        onNavigateToEvolution={onNavigateToEvolution}
      />

      {/* Error Alert */}
      {commitsError && (
        <ErrorMessage
          title="Commit Intelligence Notice"
          message={commitsError}
          onRetry={refresh}
        />
      )}

      {/* Filter Bar */}
      <CommitFilters
        filters={filters}
        onClassificationChange={setClassification}
        onAuthorEmailChange={setAuthorEmail}
        onDatePresetChange={setDatePreset}
        onCustomDateChange={setCustomDate}
        onSortChange={setSort}
        onPageSizeChange={setPageSize}
        onReset={resetFilters}
        disabled={isLoadingCommits}
      />

      {/* Paginated Commit Table */}
      <CommitTable
        commitPage={commitPage}
        isLoading={isLoadingCommits}
        currentPage={page}
        onPageChange={setPage}
        onSelectCommit={selectCommit}
        selectedCommitId={selectedCommitId}
      />

      {/* Commit Detail View */}
      {selectedCommitId !== null && (
        <CommitDetailPanel
          commit={selectedCommitDetail}
          isLoading={isLoadingDetail}
          error={detailError}
          onClose={() => selectCommit(null)}
          onSelectFile={selectFile}
          onRetry={refresh}
        />
      )}

      {/* File Detail Modal when file clicked in commit file changes */}
      {selectedFilePath !== null && (
        <FileDetailPanel
          file={selectedFileDetail}
          isLoading={isLoadingFileDetail}
          error={fileDetailError}
          onClose={() => selectFile(null)}
        />
      )}
    </div>
  );
};
