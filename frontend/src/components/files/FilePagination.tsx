import React from 'react';
import { Page } from '../../types/api';
import { Pagination } from '../common/Pagination';

interface FilePaginationProps {
  pageData: Page<unknown> | null;
  currentPage: number;
  onPageChange: (page: number) => void;
  disabled?: boolean;
}

export const FilePagination: React.FC<FilePaginationProps> = ({
  pageData,
  currentPage,
  onPageChange,
  disabled = false,
}) => {
  return (
    <Pagination
      pageData={pageData}
      currentPage={currentPage}
      onPageChange={onPageChange}
      itemLabel="files"
      disabled={disabled}
    />
  );
};
