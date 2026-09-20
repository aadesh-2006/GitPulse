import React from 'react';
import { RepositoryResponse } from '../types/repository';
import { useContributorIntelligence } from '../hooks/useContributorIntelligence';
import { ContributorHeader } from '../components/contributors/ContributorHeader';
import { ContributorTable } from '../components/contributors/ContributorTable';
import { ContributorDetailPanel } from '../components/contributors/ContributorDetailPanel';
import { OwnershipTable } from '../components/contributors/OwnershipTable';
import { FileContributorsPanel } from '../components/contributors/FileContributorsPanel';
import { ErrorMessage } from '../components/common/ErrorMessage';
import { EmptyState } from '../components/common/EmptyState';

interface ContributorIntelligencePageProps {
  repository: RepositoryResponse | null;
}

export const ContributorIntelligencePage: React.FC<ContributorIntelligencePageProps> = ({
  repository,
}) => {
  const {
    viewMode,
    setViewMode,

    contributorsPage,
    isLoadingContributors,
    contributorsError,
    contributorsPageNumber,
    setContributorsPageNumber,
    contributorsSortField,
    contributorsSortDirection,
    setContributorsSort,

    selectedContributorId,
    selectedContributorSummary,
    isLoadingContributorSummary,
    contributorFilesPage,
    isLoadingContributorFiles,
    contributorDetailError,
    contributorFilesPageNumber,
    setContributorFilesPageNumber,
    contributorFilesSortField,
    contributorFilesSortDirection,
    setContributorFilesSort,
    selectContributor,

    ownershipPage,
    isLoadingOwnership,
    ownershipError,
    ownershipPageNumber,
    setOwnershipPageNumber,
    ownershipSortField,
    ownershipSortDirection,
    setOwnershipSort,

    selectedFilePath,
    fileContributorsPage,
    isLoadingFileContributors,
    fileContributorsError,
    fileContributorsPageNumber,
    setFileContributorsPageNumber,
    fileContributorsSortField,
    fileContributorsSortDirection,
    setFileContributorsSort,
    selectFile,

    refresh,
  } = useContributorIntelligence(repository ? repository.id : null);

  if (!repository) {
    return (
      <div className="max-w-2xl mx-auto mt-12">
        <EmptyState
          title="No Repository Selected"
          description="Please select a repository from the header dropdown to view its contributor analytics and ownership concentration."
        />
      </div>
    );
  }

  const activeError =
    viewMode === 'contributors' ? contributorsError : ownershipError;

  return (
    <div className="space-y-6">
      {/* Header */}
      <ContributorHeader
        repository={repository}
        viewMode={viewMode}
        onViewModeChange={setViewMode}
        onRefresh={refresh}
        isLoading={
          isLoadingContributors ||
          isLoadingOwnership ||
          isLoadingContributorFiles ||
          isLoadingFileContributors
        }
      />

      {/* Global Notice / Error */}
      {activeError && (
        <ErrorMessage
          title="Contributor Intelligence Notice"
          message={activeError}
          onRetry={refresh}
        />
      )}

      {/* View 1: Contributor Activity */}
      {viewMode === 'contributors' && (
        <div className="space-y-6">
          <ContributorTable
            contributorsPage={contributorsPage}
            isLoading={isLoadingContributors}
            currentPage={contributorsPageNumber}
            onPageChange={setContributorsPageNumber}
            sortField={contributorsSortField}
            sortDirection={contributorsSortDirection}
            onSortChange={setContributorsSort}
            onSelectContributor={selectContributor}
            selectedContributorId={selectedContributorId}
          />

          {selectedContributorId !== null && (
            <ContributorDetailPanel
              contributorSummary={selectedContributorSummary}
              filesPage={contributorFilesPage}
              isLoadingSummary={isLoadingContributorSummary}
              isLoadingFiles={isLoadingContributorFiles}
              error={contributorDetailError}
              onClose={() => selectContributor(null)}
              filesPageNumber={contributorFilesPageNumber}
              onFilesPageChange={setContributorFilesPageNumber}
              sortField={contributorFilesSortField}
              sortDirection={contributorFilesSortDirection}
              onSortChange={setContributorFilesSort}
              onRetry={refresh}
            />
          )}
        </div>
      )}

      {/* View 2: File Ownership Concentration */}
      {viewMode === 'ownership' && (
        <div className="space-y-6">
          <OwnershipTable
            ownershipPage={ownershipPage}
            isLoading={isLoadingOwnership}
            currentPage={ownershipPageNumber}
            onPageChange={setOwnershipPageNumber}
            sortField={ownershipSortField}
            sortDirection={ownershipSortDirection}
            onSortChange={setOwnershipSort}
            onSelectFile={selectFile}
            selectedFilePath={selectedFilePath}
          />

          {selectedFilePath !== null && (
            <FileContributorsPanel
              filePath={selectedFilePath}
              contributorsPage={fileContributorsPage}
              isLoading={isLoadingFileContributors}
              error={fileContributorsError}
              onClose={() => selectFile(null)}
              currentPage={fileContributorsPageNumber}
              onPageChange={setFileContributorsPageNumber}
              sortField={fileContributorsSortField}
              sortDirection={fileContributorsSortDirection}
              onSortChange={setFileContributorsSort}
              onRetry={refresh}
            />
          )}
        </div>
      )}
    </div>
  );
};
