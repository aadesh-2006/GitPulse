import { useState, useEffect, useCallback, useRef } from 'react';
import { fileApi } from '../api/files';
import { RepositoryFileResponse } from '../types/file';
import { Page } from '../types/api';
import { ApiError } from '../api/client';

export interface UseRepositoryRiskResult {
  hotspotsPage: Page<RepositoryFileResponse> | null;
  isLoadingHotspots: boolean;
  hotspotsError: string | null;

  page: number;
  setPage: (page: number) => void;

  selectedFilePath: string | null;
  selectedFileDetail: RepositoryFileResponse | null;
  isLoadingDetail: boolean;
  detailError: string | null;
  selectFile: (filePath: string | null) => void;

  refresh: () => Promise<void>;
}

export function useRepositoryRisk(repositoryId: number | null): UseRepositoryRiskResult {
  const [hotspotsPage, setHotspotsPage] = useState<Page<RepositoryFileResponse> | null>(null);
  const [isLoadingHotspots, setIsLoadingHotspots] = useState<boolean>(false);
  const [hotspotsError, setHotspotsError] = useState<string | null>(null);

  const [page, setPage] = useState<number>(0);

  const [selectedFilePath, setSelectedFilePath] = useState<string | null>(null);
  const [selectedFileDetail, setSelectedFileDetail] = useState<RepositoryFileResponse | null>(null);
  const [isLoadingDetail, setIsLoadingDetail] = useState<boolean>(false);
  const [detailError, setDetailError] = useState<string | null>(null);

  const hotspotsFetchIdRef = useRef<number>(0);
  const detailFetchIdRef = useRef<number>(0);

  // 1. Fetch Top Hotspots / Paginated Risk Files
  const fetchHotspots = useCallback(async () => {
    if (!repositoryId) {
      setHotspotsPage(null);
      return;
    }

    const currentFetchId = ++hotspotsFetchIdRef.current;
    setIsLoadingHotspots(true);
    setHotspotsError(null);

    try {
      const response = await fileApi.getRepositoryHotspots(repositoryId, {
        page,
        size: 20,
        sort: 'compositeScore,desc',
      });

      if (currentFetchId === hotspotsFetchIdRef.current) {
        setHotspotsPage(response);
        // Automatically select top file if none selected
        if (!selectedFilePath && response.content.length > 0) {
          setSelectedFilePath(response.content[0].filePath);
        }
      }
    } catch (err) {
      if (currentFetchId === hotspotsFetchIdRef.current) {
        const message =
          err instanceof ApiError
            ? err.message
            : err instanceof Error
            ? err.message
            : 'Failed to load repository risk & stability signal';
        setHotspotsError(message);
      }
    } finally {
      if (currentFetchId === hotspotsFetchIdRef.current) {
        setIsLoadingHotspots(false);
      }
    }
  }, [repositoryId, page, selectedFilePath]);

  // 2. Fetch Single File Detail for drilldown
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
  }, [repositoryId, fetchHotspots]);

  // Load detail when selected file changes
  useEffect(() => {
    if (selectedFilePath !== null) {
      fetchDetail(selectedFilePath);
    } else {
      setSelectedFileDetail(null);
      setDetailError(null);
    }
  }, [selectedFilePath, fetchDetail]);

  const selectFile = (filePath: string | null) => {
    setSelectedFilePath(filePath);
  };

  return {
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

    refresh: async () => {
      await fetchHotspots();
      if (selectedFilePath !== null) {
        await fetchDetail(selectedFilePath);
      }
    },
  };
}
