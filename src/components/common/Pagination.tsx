import React from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { cn } from '@/lib/utils';

type PaginationProps = {
  page: number;
  pageCount: number;
  onPageChange: (page: number) => void;
  label?: string;
  previousLabel?: string;
  nextLabel?: string;
  className?: string;
  disabled?: boolean;
};

export const Pagination: React.FC<PaginationProps> = ({ page, pageCount, onPageChange, label, previousLabel = 'Previous page', nextLabel = 'Next page', className, disabled }) => {
  const safePageCount = Math.max(1, pageCount);
  return (
    <nav aria-label={label ?? 'Pagination'} className={cn('flex items-center justify-between gap-3 text-sm', className)}>
      <span className="type-body-muted tabular-nums">{page} / {safePageCount}</span>
      <div className="flex items-center gap-1">
        <button
          type="button"
          aria-label={previousLabel}
          onClick={() => onPageChange(Math.max(1, page - 1))}
          disabled={disabled || page <= 1}
          className="icon-button h-9 w-9 disabled:cursor-not-allowed disabled:opacity-40"
        >
          <ChevronLeft size={16} aria-hidden="true" />
        </button>
        <button
          type="button"
          aria-label={nextLabel}
          onClick={() => onPageChange(Math.min(safePageCount, page + 1))}
          disabled={disabled || page >= safePageCount}
          className="icon-button h-9 w-9 disabled:cursor-not-allowed disabled:opacity-40"
        >
          <ChevronRight size={16} aria-hidden="true" />
        </button>
      </div>
    </nav>
  );
};
