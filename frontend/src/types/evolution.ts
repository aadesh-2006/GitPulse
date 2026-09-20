export interface RepositoryMonthlyEvolutionBucketResponse {
  month: string;
  totalCommits: number;
  totalAdditions: number;
  totalDeletions: number;
  totalChurn: number;
  activeContributors: number;
  filesChanged: number;
  featureCommits: number;
  bugFixCommits: number;
  refactorCommits: number;
  documentationCommits: number;
  testCommits: number;
  buildCommits: number;
  configurationCommits: number;
  dependencyCommits: number;
  otherCommits: number;
}

export interface RepositoryEvolutionResponse {
  repositoryId: number;
  from: string | null;
  to: string | null;
  buckets: RepositoryMonthlyEvolutionBucketResponse[];
}

export interface RepositoryEvolutionPeriodResponse {
  from: string | null;
  to: string | null;
  totalCommits: number;
  totalAdditions: number;
  totalDeletions: number;
  totalChurn: number;
  activeContributors: number;
  filesChanged: number;
  featureCommits: number;
  bugFixCommits: number;
  refactorCommits: number;
  documentationCommits: number;
  testCommits: number;
  buildCommits: number;
  configurationCommits: number;
  dependencyCommits: number;
  otherCommits: number;
}

export interface RepositoryEvolutionDeltaResponse {
  totalCommits: number;
  totalAdditions: number;
  totalDeletions: number;
  totalChurn: number;
  activeContributors: number;
  filesChanged: number;
  featureCommits: number;
  bugFixCommits: number;
  refactorCommits: number;
  documentationCommits: number;
  testCommits: number;
  buildCommits: number;
  configurationCommits: number;
  dependencyCommits: number;
  otherCommits: number;
}

export interface RepositoryEvolutionComparisonResponse {
  repositoryId: number;
  currentPeriod: RepositoryEvolutionPeriodResponse;
  previousPeriod: RepositoryEvolutionPeriodResponse;
  delta: RepositoryEvolutionDeltaResponse;
}

export interface RepositoryEvolutionCompositionRawMetrics {
  totalCommits: number;
  totalAdditions: number;
  totalDeletions: number;
  totalChurn: number;
  activeContributors: number;
  filesChanged: number;
  featureCommits: number;
  bugFixCommits: number;
  refactorCommits: number;
  documentationCommits: number;
  testCommits: number;
  buildCommits: number;
  configurationCommits: number;
  dependencyCommits: number;
  otherCommits: number;
}

export interface RepositoryEvolutionCompositionShares {
  classifiedCommits: number;
  unclassifiedCommits: number;
  featureShare: number;
  bugFixShare: number;
  refactorShare: number;
  documentationShare: number;
  testShare: number;
  buildShare: number;
  configurationShare: number;
  dependencyShare: number;
  otherShare: number;
}

export interface RepositoryEvolutionIntensityMetrics {
  averageChurnPerCommit: number;
  averageFilesChangedPerCommit: number;
  averageAdditionsPerCommit: number;
  averageDeletionsPerCommit: number;
}

export interface RepositoryEvolutionCompositionResponse {
  repositoryId: number;
  from: string | null;
  to: string | null;
  rawMetrics: RepositoryEvolutionCompositionRawMetrics;
  composition: RepositoryEvolutionCompositionShares;
  intensity: RepositoryEvolutionIntensityMetrics;
}
