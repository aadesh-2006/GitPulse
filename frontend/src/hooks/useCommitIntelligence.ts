import { useState, useEffect, useCallback, useRef } from 'react';
import { commitApi } from '../api/commits';
import { fileApi } from '../api/files';
import { CommitResponse, CommitDetailResponse, CommitClassification } from '../types/commit';
import { RepositoryFileResponse } from '../types/file';
import { Page } from '../types/api';
import { ApiError } from '../api/client';
import { CommitFiltersState, DateRangePreset } from '../components/commits/CommitFilters';

export interface UseCommitIntelligenceResult {
  commitPage: Page<CommitResponse> | null;
  isLoadingCommits: boolean;
  commitsError: string | null;

  filters: CommitFiltersState;
  page: number;
  setPage: (page: number) => void;
  setClassification: (classification: CommitClassification | '') => void;
  setAuthorEmail: (email: string) => void;
  setDatePreset: (preset: DateRangePreset) => void;
  setCustomDate: (from: string, to: string) => void;
  setSort: (field: string, direction: 'asc' | 'desc') => void;
  setPageSize: (size: number) => void;
  resetFilters: () => void;

  selectedCommitId: number | null;
  selectedCommitDetail: CommitDetailResponse | null;
  isLoadingDetail: boolean;
  detailError: string | null;
  selectCommit: (commitId: number | null) => void;

  selectedFilePath: string | null;
  selectedFileDetail: RepositoryFileResponse | null;
  isLoadingFileDetail: boolean;
  fileDetailError: string | null;
  selectFile: (filePath: string | null) => void;

  refresh: () => Promise<void>;
}

const DEFAULT_FILTERS: CommitFiltersState = {
  classification: '',
  authorEmail: '',
  datePreset: 'all',
  from: '',
  to: '',
  sortField: 'committedAt',
  sortDirection: 'desc',
  pageSize: 20,
};

function calculatePresetDates(preset: DateRangePreset): { from: string; to: string } {
  const now = new Date();
  if (preset === '30d') {
    const from = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000);
    return { from: from.toISOString(), to: now.toISOString() };
  }
  if (preset === '90d') {
    const from = new Date(now.getTime() - 90 * 24 * 60 * 60 * 1000);
    return { from: from.toISOString(), to: now.toISOString() };
  }
  if (preset === '180d') {
    const from = new Date(now.getTime() - 180 * 24 * 60 * 60 * 1000);
    return { from: from.toISOString(), to: now.toISOString() };
  }
  return { from: '', to: '' };
}

export function useCommitIntelligence(repositoryId: number | null): UseCommitIntelligenceResult {
  const [commitPage, setCommitPage] = useState<Page<CommitResponse> | null>(null);
  const [isLoadingCommits, setIsLoadingCommits] = useState<boolean>(false);
  const [commitsError, setCommitsError] = useState<string | null>(null);

  const [filters, setFilters] = useState<CommitFiltersState>(DEFAULT_FILTERS);
  const [page, setPage] = useState<number>(0);

  const [selectedCommitId, setSelectedCommitId] = useState<number | null>(null);
  const [selectedCommitDetail, setSelectedCommitDetail] = useState<CommitDetailResponse | null>(null);
  const [isLoadingDetail, setIsLoadingDetail] = useState<boolean>(false);
  const [detailError, setDetailError] = useState<string | null>(null);

  const [selectedFilePath, setSelectedFilePath] = useState<string | null>(null);
  const [selectedFileDetail, setSelectedFileDetail] = useState<RepositoryFileResponse | null>(null);
  const [isLoadingFileDetail, setIsLoadingFileDetail] = useState<boolean>(false);
  const [fileDetailError, setFileDetailError] = useState<string | null>(null);

  const commitsFetchIdRef = useRef<number>(0);
  const detailFetchIdRef = useRef<number>(0);
  const fileDetailFetchIdRef = useRef<number>(0);

  // 1. Fetch Paginated Commits
  const fetchCommits = useCallback(async () => {
    if (!repositoryId) {
      setCommitPage(null);
      return;
    }

    const currentFetchId = ++commitsFetchIdRef.current;
    setIsLoadingCommits(true);
    setCommitsError(null);

    const sortParam = `${filters.sortField},${filters.sortDirection}`;
    const classificationParam = filters.classification || undefined;
    const authorEmailParam = filters.authorEmail.trim() || undefined;
    const fromParam = filters.from.trim() || undefined;
    const toParam = filters.to.trim() || undefined;

    try {
      const response = await commitApi.getRepositoryCommits(repositoryId, {
        page,
        size: filters.pageSize,
        sort: sortParam,
        classification: classificationParam,
        authorEmail: authorEmailParam,
        from: fromParam,
        to: toParam,
      });

      if (currentFetchId === commitsFetchIdRef.current) {
        setCommitPage(response);
      }
    } catch (err) {
      if (currentFetchId === commitsFetchIdRef.current) {
        const message =
          err instanceof ApiError
            ? err.message
            : err instanceof Error
            ? err.message
            : 'Failed to load repository commits';
        setCommitsError(message);
      }
    } finally {
      if (currentFetchId === commitsFetchIdRef.current) {
        setIsLoadingCommits(false);
      }
    }
  }, [repositoryId, page, filters]);

  // 2. Fetch Single Commit Detail
  const fetchCommitDetail = useCallback(async (commitId: number) => {
    if (!repositoryId || !commitId) return;

    const currentFetchId = ++detailFetchIdRef.current;
    setIsLoadingDetail(true);
    setDetailError(null);

    try {
      const response = await commitApi.getCommitDetail(repositoryId, commitId);
      if (currentFetchId === detailFetchIdRef.current) {
        setSelectedCommitDetail(response);
      }
    } catch (err) {
      if (currentFetchId === detailFetchIdRef.current) {
        const message =
          err instanceof ApiError
            ? err.message
            : err instanceof Error
            ? err.message
            : 'Failed to load commit detail';
        setDetailError(message);
      }
    } finally {
      if (currentFetchId === detailFetchIdRef.current) {
        setIsLoadingDetail(false);
      }
    }
  }, [repositoryId]);

  // 3. Fetch Single File Intelligence Detail for file click
  const fetchFileDetail = useCallback(async (filePath: string) => {
    if (!repositoryId || !filePath) return;

    const currentFetchId = ++fileDetailFetchIdRef.current;
    setIsLoadingFileDetail(true);
    setFileDetailError(null);

    try {
      const response = await fileApi.getRepositoryFileByPath(repositoryId, filePath);
      if (currentFetchId === fileDetailFetchIdRef.current) {
        setSelectedFileDetail(response);
      }
    } catch (err) {
      if (currentFetchId === fileDetailFetchIdRef.current) {
        const message =
          err instanceof ApiError
            ? err.message
            : err instanceof Error
            ? err.message
            : 'Failed to load file detail';
        setFileDetailError(message);
      }
    } finally {
      if (currentFetchId === fileDetailFetchIdRef.current) {
        setIsLoadingFileDetail(false);
      }
    }
  }, [repositoryId]);

  // Initial load / repository switch
  useEffect(() => {
    setPage(0);
    setSelectedCommitId(null);
    setSelectedCommitDetail(null);
    setSelectedFilePath(null);
    setSelectedFileDetail(null);
    fetchCommits();
  }, [repositoryId, fetchCommits]);

  // Commit detail load
  useEffect(() => {
    if (selectedCommitId !== null) {
      fetchCommitDetail(selectedCommitId);
    } else {
      setSelectedCommitDetail(null);
      setDetailError(null);
    }
  }, [selectedCommitId, fetchCommitDetail]);

  // File detail load
  useEffect(() => {
    if (selectedFilePath !== null) {
      fetchFileDetail(selectedFilePath);
    } else {
      setSelectedFileDetail(null);
      setFileDetailError(null);
    }
  }, [selectedFilePath, fetchFileDetail]);

  // Filter modifiers (reset page to 0)
  const setClassification = (classification: CommitClassification | '') => {
    setFilters((prev) => ({ ...prev, classification }));
    setPage(0);
  };

  const setAuthorEmail = (email: string) => {
    setFilters((prev) => ({ ...prev, authorEmail: email }));
    setPage(0);
  };

  const setDatePreset = (preset: DateRangePreset) => {
    const dates = calculatePresetDates(preset);
    setFilters((prev) => ({
      ...prev,
      datePreset: preset,
      from: dates.from,
      to: dates.to,
    }));
    setPage(0);
  };

  const setCustomDate = (from: string, to: string) => {
    setFilters((prev) => ({
      ...prev,
      datePreset: 'custom',
      from,
      to,
    }));
    setPage(0);
  };

  const setSort = (field: string, direction: 'asc' | 'desc') => {
    setFilters((prev) => ({ ...prev, sortField: field, sortDirection: direction }));
    setPage(0);
  };

  const setPageSize = (size: number) => {
    setFilters((prev) => ({ ...prev, pageSize: size }));
    setPage(0);
  };

  const resetFilters = () => {
    setFilters(DEFAULT_FILTERS);
    setPage(0);
  };

  const selectCommit = (commitId: number | null) => {
    setSelectedCommitId(commitId);
  };

  const selectFile = (filePath: string | null) => {
    setSelectedFilePath(filePath);
  };

  return {
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

    refresh: async () => {
      await fetchCommits();
      if (selectedCommitId !== null) {
        await fetchCommitDetail(selectedCommitId);
      }
      if (selectedFilePath !== null) {
        await fetchFileDetail(selectedFilePath);
      }
    },
  };
}
