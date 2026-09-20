import { useState, useEffect, useCallback, useRef } from 'react';
import { contributorApi } from '../api/contributors';
import { ownershipApi } from '../api/ownership';
import { RepositoryContributorResponse } from '../types/contributor';
import {
  RepositoryFileOwnershipResponse,
  RepositoryContributorFileResponse,
} from '../types/ownership';
import { Page } from '../types/api';
import { ApiError } from '../api/client';
import { ContributorViewMode } from '../components/contributors/ContributorHeader';

export interface UseContributorIntelligenceResult {
  viewMode: ContributorViewMode;
  setViewMode: (mode: ContributorViewMode) => void;

  // Repository Contributors List
  contributorsPage: Page<RepositoryContributorResponse> | null;
  isLoadingContributors: boolean;
  contributorsError: string | null;
  contributorsPageNumber: number;
  setContributorsPageNumber: (page: number) => void;
  contributorsSortField: string;
  contributorsSortDirection: 'asc' | 'desc';
  setContributorsSort: (field: string, direction: 'asc' | 'desc') => void;

  // Contributor Drill-down
  selectedContributorId: number | null;
  selectedContributorSummary: RepositoryContributorResponse | null;
  isLoadingContributorSummary: boolean;
  contributorFilesPage: Page<RepositoryContributorFileResponse> | null;
  isLoadingContributorFiles: boolean;
  contributorDetailError: string | null;
  contributorFilesPageNumber: number;
  setContributorFilesPageNumber: (page: number) => void;
  contributorFilesSortField: string;
  contributorFilesSortDirection: 'asc' | 'desc';
  setContributorFilesSort: (field: string, direction: 'asc' | 'desc') => void;
  selectContributor: (contributorId: number | null) => void;

  // File Ownership Concentration List
  ownershipPage: Page<RepositoryFileOwnershipResponse> | null;
  isLoadingOwnership: boolean;
  ownershipError: string | null;
  ownershipPageNumber: number;
  setOwnershipPageNumber: (page: number) => void;
  ownershipSortField: string;
  ownershipSortDirection: 'asc' | 'desc';
  setOwnershipSort: (field: string, direction: 'asc' | 'desc') => void;

  // File Contributors Drill-down
  selectedFilePath: string | null;
  fileContributorsPage: Page<RepositoryContributorFileResponse> | null;
  isLoadingFileContributors: boolean;
  fileContributorsError: string | null;
  fileContributorsPageNumber: number;
  setFileContributorsPageNumber: (page: number) => void;
  fileContributorsSortField: string;
  fileContributorsSortDirection: 'asc' | 'desc';
  setFileContributorsSort: (field: string, direction: 'asc' | 'desc') => void;
  selectFile: (filePath: string | null) => void;

  refresh: () => Promise<void>;
}

export function useContributorIntelligence(repositoryId: number | null): UseContributorIntelligenceResult {
  const [viewMode, setViewMode] = useState<ContributorViewMode>('contributors');

  // Contributors List State
  const [contributorsPage, setContributorsPage] = useState<Page<RepositoryContributorResponse> | null>(null);
  const [isLoadingContributors, setIsLoadingContributors] = useState<boolean>(false);
  const [contributorsError, setContributorsError] = useState<string | null>(null);
  const [contributorsPageNumber, setContributorsPageNumber] = useState<number>(0);
  const [contributorsSortField, setContributorsSortField] = useState<string>('totalCommits');
  const [contributorsSortDirection, setContributorsSortDirection] = useState<'asc' | 'desc'>('desc');

  // Contributor Drill-down State
  const [selectedContributorId, setSelectedContributorId] = useState<number | null>(null);
  const [selectedContributorSummary, setSelectedContributorSummary] = useState<RepositoryContributorResponse | null>(null);
  const [isLoadingContributorSummary, setIsLoadingContributorSummary] = useState<boolean>(false);
  const [contributorFilesPage, setContributorFilesPage] = useState<Page<RepositoryContributorFileResponse> | null>(null);
  const [isLoadingContributorFiles, setIsLoadingContributorFiles] = useState<boolean>(false);
  const [contributorDetailError, setContributorDetailError] = useState<string | null>(null);
  const [contributorFilesPageNumber, setContributorFilesPageNumber] = useState<number>(0);
  const [contributorFilesSortField, setContributorFilesSortField] = useState<string>('totalChurn');
  const [contributorFilesSortDirection, setContributorFilesSortDirection] = useState<'asc' | 'desc'>('desc');

  // File Ownership Concentration State
  const [ownershipPage, setOwnershipPage] = useState<Page<RepositoryFileOwnershipResponse> | null>(null);
  const [isLoadingOwnership, setIsLoadingOwnership] = useState<boolean>(false);
  const [ownershipError, setOwnershipError] = useState<string | null>(null);
  const [ownershipPageNumber, setOwnershipPageNumber] = useState<number>(0);
  const [ownershipSortField, setOwnershipSortField] = useState<string>('topContributorRevisionShare');
  const [ownershipSortDirection, setOwnershipSortDirection] = useState<'asc' | 'desc'>('desc');

  // File Contributors Drill-down State
  const [selectedFilePath, setSelectedFilePath] = useState<string | null>(null);
  const [fileContributorsPage, setFileContributorsPage] = useState<Page<RepositoryContributorFileResponse> | null>(null);
  const [isLoadingFileContributors, setIsLoadingFileContributors] = useState<boolean>(false);
  const [fileContributorsError, setFileContributorsError] = useState<string | null>(null);
  const [fileContributorsPageNumber, setFileContributorsPageNumber] = useState<number>(0);
  const [fileContributorsSortField, setFileContributorsSortField] = useState<string>('totalChurn');
  const [fileContributorsSortDirection, setFileContributorsSortDirection] = useState<'asc' | 'desc'>('desc');

  // Request ID refs for race-safety
  const contributorsFetchIdRef = useRef<number>(0);
  const contributorSummaryFetchIdRef = useRef<number>(0);
  const contributorFilesFetchIdRef = useRef<number>(0);
  const ownershipFetchIdRef = useRef<number>(0);
  const fileContributorsFetchIdRef = useRef<number>(0);

  // 1. Fetch Contributors List
  const fetchContributors = useCallback(async () => {
    if (!repositoryId) {
      setContributorsPage(null);
      return;
    }

    const currentFetchId = ++contributorsFetchIdRef.current;
    setIsLoadingContributors(true);
    setContributorsError(null);

    const sortParam = `${contributorsSortField},${contributorsSortDirection}`;

    try {
      const response = await contributorApi.getRepositoryContributors(repositoryId, {
        page: contributorsPageNumber,
        size: 20,
        sort: sortParam,
      });

      if (currentFetchId === contributorsFetchIdRef.current) {
        setContributorsPage(response);
      }
    } catch (err) {
      if (currentFetchId === contributorsFetchIdRef.current) {
        const message =
          err instanceof ApiError
            ? err.message
            : err instanceof Error
            ? err.message
            : 'Failed to load repository contributors';
        setContributorsError(message);
      }
    } finally {
      if (currentFetchId === contributorsFetchIdRef.current) {
        setIsLoadingContributors(false);
      }
    }
  }, [repositoryId, contributorsPageNumber, contributorsSortField, contributorsSortDirection]);

  // 2. Fetch Contributor Summary & Contributed Files
  const fetchContributorSummary = useCallback(async (contributorId: number) => {
    if (!repositoryId) return;

    const currentFetchId = ++contributorSummaryFetchIdRef.current;
    setIsLoadingContributorSummary(true);
    setContributorDetailError(null);

    try {
      const response = await contributorApi.getRepositoryContributor(repositoryId, contributorId);
      if (currentFetchId === contributorSummaryFetchIdRef.current) {
        setSelectedContributorSummary(response);
      }
    } catch (err) {
      if (currentFetchId === contributorSummaryFetchIdRef.current) {
        const message =
          err instanceof ApiError
            ? err.message
            : err instanceof Error
            ? err.message
            : 'Failed to load contributor profile';
        setContributorDetailError(message);
      }
    } finally {
      if (currentFetchId === contributorSummaryFetchIdRef.current) {
        setIsLoadingContributorSummary(false);
      }
    }
  }, [repositoryId]);

  const fetchContributorFiles = useCallback(async (contributorId: number) => {
    if (!repositoryId) return;

    const currentFetchId = ++contributorFilesFetchIdRef.current;
    setIsLoadingContributorFiles(true);

    const sortParam = `${contributorFilesSortField},${contributorFilesSortDirection}`;

    try {
      const response = await ownershipApi.getContributorFilesByContributor(
        repositoryId,
        contributorId,
        {
          page: contributorFilesPageNumber,
          size: 20,
          sort: sortParam,
        }
      );

      if (currentFetchId === contributorFilesFetchIdRef.current) {
        setContributorFilesPage(response);
      }
    } catch (err) {
      if (currentFetchId === contributorFilesFetchIdRef.current) {
        const message =
          err instanceof ApiError
            ? err.message
            : err instanceof Error
            ? err.message
            : 'Failed to load contributed files';
        setContributorDetailError(message);
      }
    } finally {
      if (currentFetchId === contributorFilesFetchIdRef.current) {
        setIsLoadingContributorFiles(false);
      }
    }
  }, [repositoryId, contributorFilesPageNumber, contributorFilesSortField, contributorFilesSortDirection]);

  // 3. Fetch Ownership Concentration
  const fetchOwnership = useCallback(async () => {
    if (!repositoryId) {
      setOwnershipPage(null);
      return;
    }

    const currentFetchId = ++ownershipFetchIdRef.current;
    setIsLoadingOwnership(true);
    setOwnershipError(null);

    const sortParam = `${ownershipSortField},${ownershipSortDirection}`;

    try {
      const response = await ownershipApi.getFileOwnership(repositoryId, {
        page: ownershipPageNumber,
        size: 20,
        sort: sortParam,
      });

      if (currentFetchId === ownershipFetchIdRef.current) {
        setOwnershipPage(response);
      }
    } catch (err) {
      if (currentFetchId === ownershipFetchIdRef.current) {
        const message =
          err instanceof ApiError
            ? err.message
            : err instanceof Error
            ? err.message
            : 'Failed to load file ownership data';
        setOwnershipError(message);
      }
    } finally {
      if (currentFetchId === ownershipFetchIdRef.current) {
        setIsLoadingOwnership(false);
      }
    }
  }, [repositoryId, ownershipPageNumber, ownershipSortField, ownershipSortDirection]);

  // 4. Fetch File Contributors
  const fetchFileContributors = useCallback(async (filePath: string) => {
    if (!repositoryId) return;

    const currentFetchId = ++fileContributorsFetchIdRef.current;
    setIsLoadingFileContributors(true);
    setFileContributorsError(null);

    const sortParam = `${fileContributorsSortField},${fileContributorsSortDirection}`;

    try {
      const response = await ownershipApi.getContributorFilesByFilePath(repositoryId, {
        filePath,
        page: fileContributorsPageNumber,
        size: 20,
        sort: sortParam,
      });

      if (currentFetchId === fileContributorsFetchIdRef.current) {
        setFileContributorsPage(response);
      }
    } catch (err) {
      if (currentFetchId === fileContributorsFetchIdRef.current) {
        const message =
          err instanceof ApiError
            ? err.message
            : err instanceof Error
            ? err.message
            : 'Failed to load file contributors';
        setFileContributorsError(message);
      }
    } finally {
      if (currentFetchId === fileContributorsFetchIdRef.current) {
        setIsLoadingFileContributors(false);
      }
    }
  }, [repositoryId, fileContributorsPageNumber, fileContributorsSortField, fileContributorsSortDirection]);

  // Reset & load on repository change
  useEffect(() => {
    setContributorsPageNumber(0);
    setOwnershipPageNumber(0);
    setSelectedContributorId(null);
    setSelectedContributorSummary(null);
    setContributorFilesPage(null);
    setSelectedFilePath(null);
    setFileContributorsPage(null);

    if (viewMode === 'contributors') {
      fetchContributors();
    } else {
      fetchOwnership();
    }
  }, [repositoryId, viewMode, fetchContributors, fetchOwnership]);

  // Load contributor drill-down when contributor ID or its page/sort changes
  useEffect(() => {
    if (selectedContributorId !== null) {
      fetchContributorSummary(selectedContributorId);
      fetchContributorFiles(selectedContributorId);
    }
  }, [selectedContributorId, fetchContributorSummary, fetchContributorFiles]);

  // Load file contributors drill-down when filePath or its page/sort changes
  useEffect(() => {
    if (selectedFilePath !== null) {
      fetchFileContributors(selectedFilePath);
    }
  }, [selectedFilePath, fetchFileContributors]);

  // Sort & page helper actions
  const setContributorsSort = (field: string, direction: 'asc' | 'desc') => {
    setContributorsSortField(field);
    setContributorsSortDirection(direction);
    setContributorsPageNumber(0);
  };

  const setContributorFilesSort = (field: string, direction: 'asc' | 'desc') => {
    setContributorFilesSortField(field);
    setContributorFilesSortDirection(direction);
    setContributorFilesPageNumber(0);
  };

  const setOwnershipSort = (field: string, direction: 'asc' | 'desc') => {
    setOwnershipSortField(field);
    setOwnershipSortDirection(direction);
    setOwnershipPageNumber(0);
  };

  const setFileContributorsSort = (field: string, direction: 'asc' | 'desc') => {
    setFileContributorsSortField(field);
    setFileContributorsSortDirection(direction);
    setFileContributorsPageNumber(0);
  };

  const selectContributor = (contributorId: number | null) => {
    setSelectedContributorId(contributorId);
    setContributorFilesPageNumber(0);
    if (contributorId === null) {
      setSelectedContributorSummary(null);
      setContributorFilesPage(null);
      setContributorDetailError(null);
    }
  };

  const selectFile = (filePath: string | null) => {
    setSelectedFilePath(filePath);
    setFileContributorsPageNumber(0);
    if (filePath === null) {
      setFileContributorsPage(null);
      setFileContributorsError(null);
    }
  };

  return {
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

    refresh: async () => {
      if (viewMode === 'contributors') {
        await fetchContributors();
        if (selectedContributorId !== null) {
          await Promise.all([
            fetchContributorSummary(selectedContributorId),
            fetchContributorFiles(selectedContributorId),
          ]);
        }
      } else {
        await fetchOwnership();
        if (selectedFilePath !== null) {
          await fetchFileContributors(selectedFilePath);
        }
      }
    },
  };
}
