import { useState, useEffect, useCallback, useRef } from 'react';
import { fileApi } from '../api/files';
import { RepositoryFileResponse } from '../types/file';
import { Page } from '../types/api';
import { ApiError } from '../api/client';

export type DeletedFilterOption = 'all' | 'active' | 'deleted';

export interface FileFiltersState {
  extension: string;
  deletedFilter: DeletedFilterOption;
  sortField: string;
  sortDirection: 'asc' | 'desc';
  pageSize: number;
}

export interface UseFileIntelligenceResult {
  hotspots: RepositoryFileResponse[];
  isLoadingHotspots: boolean;
  hotspotsError: string | null;

  filePage: Page<RepositoryFileResponse> | null;
  isLoadingFiles: boolean;
  filesError: string | null;

  filters: FileFiltersState;
  page: number;
  setPage: (page: number) => void;
  setExtension: (ext: string) => void;
  setDeletedFilter: (filter: DeletedFilterOption) => void;
  setSort: (field: string, direction: 'asc' | 'desc') => void;
  setPageSize: (size: number) => void;
  resetFilters: () => void;

  selectedFilePath: string | null;
  selectedFileDetail: RepositoryFileResponse | null;
  isLoadingDetail: boolean;
  detailError: string | null;
  selectFile: (filePath: string | null) => void;

  refresh: () => Promise<void>;
}

const DEFAULT_FILTERS: FileFiltersState = {
  extension: '',
  deletedFilter: 'all',
  sortField: 'compositeScore',
  sortDirection: 'desc',
  pageSize: 20,
};

export function useFileIntelligence(repositoryId: number | null): UseFileIntelligenceResult {
  const [hotspots, setHotspots] = useState<RepositoryFileResponse[]>([]);
  const [isLoadingHotspots, setIsLoadingHotspots] = useState<boolean>(false);
  const [hotspotsError, setHotspotsError] = useState<string | null>(null);

  const [filePage, setFilePage] = useState<Page<RepositoryFileResponse> | null>(null);
  const [isLoadingFiles, setIsLoadingFiles] = useState<boolean>(false);
  const [filesError, setFilesError] = useState<string | null>(null);

  const [filters, setFilters] = useState<FileFiltersState>(DEFAULT_FILTERS);
  const [page, setPage] = useState<number>(0);

  const [selectedFilePath, setSelectedFilePath] = useState<string | null>(null);
  const [selectedFileDetail, setSelectedFileDetail] = useState<RepositoryFileResponse | null>(null);
  const [isLoadingDetail, setIsLoadingDetail] = useState<boolean>(false);
  const [detailError, setDetailError] = useState<string | null>(null);

  const hotspotFetchIdRef = useRef<number>(0);
  const filesFetchIdRef = useRef<number>(0);
  const detailFetchIdRef = useRef<number>(0);

  // Fetch top 10 Hotspots
  const fetchHotspots = useCallback(async () => {
    if (!repositoryId) {
      setHotspots([]);
      return;
    }

    const currentFetchId = ++hotspotFetchIdRef.current;
    setIsLoadingHotspots(true);
    setHotspotsError(null);

    try {
      const response = await fileApi.getRepositoryHotspots(repositoryId, {
        page: 0,
        size: 10,
        sort: 'compositeScore,desc',
      });

      if (currentFetchId === hotspotFetchIdRef.current) {
        setHotspots(response.content);
      }
    } catch (err) {
      if (currentFetchId === hotspotFetchIdRef.current) {
        const message =
          err instanceof ApiError
            ? err.message
            : err instanceof Error
            ? err.message
            : 'Failed to load repository hotspots';
        setHotspotsError(message);
      }
    } finally {
      if (currentFetchId === hotspotFetchIdRef.current) {
        setIsLoadingHotspots(false);
      }
    }
  }, [repositoryId]);

  // Fetch Full Filtered & Paginated File List
  const fetchFiles = useCallback(async () => {
    if (!repositoryId) {
      setFilePage(null);
      return;
    }

    const currentFetchId = ++filesFetchIdRef.current;
    setIsLoadingFiles(true);
    setFilesError(null);

    const isDeletedParam =
      filters.deletedFilter === 'active'
        ? false
        : filters.deletedFilter === 'deleted'
        ? true
        : undefined;

    const extensionParam = filters.extension.trim() ? filters.extension.trim() : undefined;
    const sortParam = `${filters.sortField},${filters.sortDirection}`;

    try {
      const response = await fileApi.getRepositoryFiles(repositoryId, {
        page,
        size: filters.pageSize,
        sort: sortParam,
        extension: extensionParam,
        isDeleted: isDeletedParam,
      });

      if (currentFetchId === filesFetchIdRef.current) {
        setFilePage(response);
      }
    } catch (err) {
      if (currentFetchId === filesFetchIdRef.current) {
        const message =
          err instanceof ApiError
            ? err.message
            : err instanceof Error
            ? err.message
            : 'Failed to load repository files';
        setFilesError(message);
      }
    } finally {
      if (currentFetchId === filesFetchIdRef.current) {
        setIsLoadingFiles(false);
      }
    }
  }, [repositoryId, page, filters]);

  // Fetch Single File Detail
  const fetchDetail = useCallback(async (filePath: string) => {
    if (!repositoryId || !filePath) return;

    const currentFetchId = ++detailFetchIdRef.current;
    setIsLoadingDetail(true);
    setDetailError(null);

    try {
      const response = await fileApi.getRepositoryFileByPath(repositoryId, filePath);
      if (currentFetchId === detailFetchIdRef.current) {
        setSelectedFileDetail(response);
      }
    } catch (err) {
      if (currentFetchId === detailFetchIdRef.current) {
        const message =
          err instanceof ApiError
            ? err.message
            : err instanceof Error
            ? err.message
            : 'Failed to load file details';
        setDetailError(message);
      }
    } finally {
      if (currentFetchId === detailFetchIdRef.current) {
        setIsLoadingDetail(false);
      }
    }
  }, [repositoryId]);

  // Initial load when repository changes
  useEffect(() => {
    setPage(0);
    setSelectedFilePath(null);
    setSelectedFileDetail(null);
    fetchHotspots();
    fetchFiles();
  }, [repositoryId, fetchHotspots, fetchFiles]);

  // Filter modifiers (reset to page 0)
  const setExtension = (ext: string) => {
    setFilters((prev) => ({ ...prev, extension: ext }));
    setPage(0);
  };

  const setDeletedFilter = (filter: DeletedFilterOption) => {
    setFilters((prev) => ({ ...prev, deletedFilter: filter }));
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

  const selectFile = (filePath: string | null) => {
    setSelectedFilePath(filePath);
    if (filePath) {
      fetchDetail(filePath);
    } else {
      setSelectedFileDetail(null);
      setDetailError(null);
    }
  };

  return {
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

    refresh: async () => {
      await Promise.all([fetchHotspots(), fetchFiles()]);
      if (selectedFilePath) {
        await fetchDetail(selectedFilePath);
      }
    },
  };
}
