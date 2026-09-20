import React, { useState } from 'react';
import { RepositoryResponse } from '../types/repository';
import { useRepositoryRisk } from '../hooks/useRepositoryRisk';
import { RiskHeader } from '../components/risk/RiskHeader';
import { RiskModelExplanation } from '../components/risk/RiskModelExplanation';
import { RiskSummary } from '../components/risk/RiskSummary';
import { BaselineCompositeChart } from '../components/risk/BaselineCompositeChart';
import { RiskScoreBreakdown } from '../components/risk/RiskScoreBreakdown';
import { RiskComparisonTable } from '../components/risk/RiskComparisonTable';
import { RiskHotspotTable } from '../components/risk/RiskHotspotTable';
import { FileDetailPanel } from '../components/files/FileDetailPanel';
import { ErrorMessage } from '../components/common/ErrorMessage';
import { EmptyState } from '../components/common/EmptyState';

interface RepositoryRiskPageProps {
  repository: RepositoryResponse | null;
}

export const RepositoryRiskPage: React.FC<RepositoryRiskPageProps> = ({
  repository,
}) => {
  const {
    hotspotsPage,
    isLoadingHotspots,
    hotspotsError,

    page,
    setPage,

    selectedFilePath,
    selectedFileDetail,
    isLoadingDetail,
    detailError,
    selectFile,

    refresh,
  } = useRepositoryRisk(repository ? repository.id : null);

  const [isDetailModalOpen, setIsDetailModalOpen] = useState<boolean>(false);

  if (!repository) {
    return (
      <div className="max-w-2xl mx-auto mt-12">
        <EmptyState
          title="No Repository Selected"
          description="Please select a repository from the header dropdown to view its risk and stability signal analysis."
        />
      </div>
    );
  }

  const files = hotspotsPage?.content || [];

  const handleSelectFile = (filePath: string) => {
    selectFile(filePath);
  };

  const handleInspectFile = (filePath: string) => {
    selectFile(filePath);
    setIsDetailModalOpen(true);
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <RiskHeader
        repository={repository}
        onRefresh={refresh}
        isLoading={isLoadingHotspots || isLoadingDetail}
      />

      {/* Global Error Notice */}
      {hotspotsError && (
        <ErrorMessage
          title="Risk & Stability Notice"
          message={hotspotsError}
          onRetry={refresh}
        />
      )}

      {/* Mathematical Scoring Model Explanation */}
      <RiskModelExplanation />

      {/* Sample-derived Summary Metrics */}
      <RiskSummary
        files={files}
        totalElements={hotspotsPage?.totalElements}
        isLoading={isLoadingHotspots}
      />

      {/* Visual Distribution & Selected File Breakdown Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <BaselineCompositeChart
          files={files}
          onSelectFile={handleSelectFile}
          selectedFilePath={selectedFilePath}
        />

        <RiskScoreBreakdown file={selectedFileDetail} />
      </div>

      {/* Scientific Research Comparison Table */}
      <RiskComparisonTable
        files={files}
        onSelectFile={handleInspectFile}
        selectedFilePath={selectedFilePath}
      />

      {/* Full Hotspots / Risk Table */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-bold uppercase tracking-wider text-slate-200">
            Repository Activity Hotspots & Risk Signals
          </h3>
          <span className="text-xs text-slate-400">
            Ordered by multidimensional composite score
          </span>
        </div>

        <RiskHotspotTable
          filesPage={hotspotsPage}
          isLoading={isLoadingHotspots}
          currentPage={page}
          onPageChange={setPage}
          onSelectFile={handleInspectFile}
          selectedFilePath={selectedFilePath}
        />
      </div>

      {/* Full File Detail Slide-over Panel */}
      {isDetailModalOpen && selectedFilePath && (
        <FileDetailPanel
          file={selectedFileDetail}
          isLoading={isLoadingDetail}
          error={detailError}
          onClose={() => setIsDetailModalOpen(false)}
        />
      )}
    </div>
  );
};
