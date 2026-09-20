import React, { useState } from 'react';
import { useRepositories } from './hooks/useRepositories';
import { AppShell, NavigationTab } from './components/layout/AppShell';
import { RepositoryOverviewPage } from './pages/RepositoryOverviewPage';
import { EvolutionPage } from './pages/EvolutionPage';
import { FileIntelligencePage } from './pages/FileIntelligencePage';
import { CommitIntelligencePage } from './pages/CommitIntelligencePage';
import { ContributorIntelligencePage } from './pages/ContributorIntelligencePage';

export const App: React.FC = () => {
  const {
    repositories,
    selectedRepository,
    selectedRepositoryId,
    isLoading,
    error,
    selectRepository,
    refreshRepositories,
  } = useRepositories();

  const [activeTab, setActiveTab] = useState<NavigationTab>('overview');

  return (
    <AppShell
      repositories={repositories}
      selectedRepository={selectedRepository}
      selectedRepositoryId={selectedRepositoryId}
      onSelectRepository={selectRepository}
      isLoadingRepositories={isLoading}
      activeTab={activeTab}
      onTabChange={setActiveTab}
    >
      {activeTab === 'overview' && (
        <RepositoryOverviewPage
          repository={selectedRepository}
          isLoadingRepository={isLoading && repositories.length === 0}
          repositoryError={error}
          onRefreshRepository={refreshRepositories}
        />
      )}

      {activeTab === 'evolution' && (
        <EvolutionPage repository={selectedRepository} />
      )}

      {activeTab === 'files' && (
        <FileIntelligencePage repository={selectedRepository} />
      )}

      {activeTab === 'commits' && (
        <CommitIntelligencePage
          repository={selectedRepository}
          onNavigateToEvolution={() => setActiveTab('evolution')}
        />
      )}

      {activeTab === 'contributors' && (
        <ContributorIntelligencePage repository={selectedRepository} />
      )}
    </AppShell>
  );
};

export default App;
