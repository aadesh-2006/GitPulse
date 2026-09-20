import React from 'react';
import { useRepositories } from './hooks/useRepositories';
import { AppShell } from './components/layout/AppShell';
import { RepositoryOverviewPage } from './pages/RepositoryOverviewPage';

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

  return (
    <AppShell
      repositories={repositories}
      selectedRepository={selectedRepository}
      selectedRepositoryId={selectedRepositoryId}
      onSelectRepository={selectRepository}
      isLoadingRepositories={isLoading}
    >
      <RepositoryOverviewPage
        repository={selectedRepository}
        isLoadingRepository={isLoading && repositories.length === 0}
        repositoryError={error}
        onRefreshRepository={refreshRepositories}
      />
    </AppShell>
  );
};

export default App;
