import React from 'react';
import { RepositoryResponse } from '../types/repository';
import { useFileIntelligence } from '../hooks/useFileIntelligence';
import { FileIntelligenceHeader } from '../components/files/FileIntelligenceHeader';
import { HotspotTable } from '../components/files/HotspotTable';
import { FileFilters } from '../components/files/FileFilters';
import { FileTable } from '../components/files/FileTable';
import { FileDetailPanel } from '../components/files/FileDetailPanel';
import { ErrorMessage } from '../components/common/ErrorMessage';
import { EmptyState } from '../components/common/EmptyState';

interface FileIntelligencePageProps {
  repository: RepositoryResponse | null;
}

export const FileIntelligencePage: React.FC<FileIntelligencePageProps> = ({ repository }) => {
  const {
    hotspots,
    isLoadingHotspots,
    hotspotsError,

    filePage,
    isLoadingFiles,
    filesError,

    filters,
    page,
    setPage,
    setExtension,
    setDeletedFilter,
    setSort,
    setPageSize,
    resetFilters,

    selectedFilePath,
    selectedFileDetail,
    isLoadingDetail,
    detailError,
    selectFile,

    refresh,
  } = useFileIntelligence(repository ? repository.id : null);

  if (!repository) {
    return (
      <div className="max-w-2xl mx-auto mt-12">
        <EmptyState
          title="No Repository Selected"
          description="Please select a repository from the header dropdown to view its file activity and hotspot intelligence."
        />
      </div>
    );
  }

  const combinedError = hotspotsError || filesError;

  return (
    <div className="space-y-6">
      {/* Header */}
      <FileIntelligenceHeader
        repository={repository}
        onRefresh={refresh}
        isLoading={isLoadingHotspots || isLoadingFiles}
      />

      {/* Error Alert */}
      {combinedError && (
        <ErrorMessage
          title="File Intelligence Notice"
          message={combinedError}
          onRetry={refresh}
        />
      )}

      {/* Top 10 Code Hotspots */}
      <HotspotTable
        hotspots={hotspots}
        isLoading={isLoadingHotspots}
        onSelectFile={selectFile}
        selectedFilePath={selectedFilePath}
      />

      {/* Filter Bar */}
      <FileFilters
        filters={filters}
        onExtensionChange={setExtension}
        onDeletedChange={setDeletedFilter}
        onSortChange={setSort}
        onPageSizeChange={setPageSize}
        onReset={resetFilters}
        disabled={isLoadingFiles}
      />

      {/* Paginated File Table */}
      <FileTable
        filePage={filePage}
        isLoading={isLoadingFiles}
        currentPage={page}
        onPageChange={setPage}
        onSelectFile={selectFile}
        selectedFilePath={selectedFilePath}
      />

      {/* File Drill-down Detail Panel */}
      {selectedFilePath && (
        <FileDetailPanel
          file={selectedFileDetail}
          isLoading={isLoadingDetail}
          error={detailError}
          onClose={() => selectFile(null)}
        />
      )}
    </div>
  );
};
