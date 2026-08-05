import React from 'react';
import { ArrowDown, ArrowUp, ArrowUpDown } from 'lucide-react';
import { cn } from '@/lib/utils';

export type DataTableColumn<T> = {
  id: string;
  header: React.ReactNode;
  accessor?: keyof T;
  render?: (row: T) => React.ReactNode;
  sortable?: boolean;
  sortValue?: (row: T) => string | number | null | undefined;
  className?: string;
};

type DataTableProps<T> = {
  rows: T[];
  columns: DataTableColumn<T>[];
  getRowId?: (row: T, index: number) => React.Key;
  caption?: string;
  empty?: React.ReactNode;
  density?: 'compact' | 'comfortable';
  className?: string;
};

export function DataTable<T>({ rows, columns, getRowId, caption, empty, density = 'comfortable', className }: DataTableProps<T>) {
  const [sort, setSort] = React.useState<{ id: string; direction: 'asc' | 'desc' } | null>(null);
  const activeColumn = columns.find((column) => column.id === sort?.id);
  const sortedRows = React.useMemo(() => {
    if (!sort || !activeColumn?.sortable) return rows;
    const getValue = (row: T) => activeColumn.sortValue?.(row) ?? (activeColumn.accessor ? row[activeColumn.accessor] : '');
    return [...rows].sort((a, b) => {
      const left = getValue(a);
      const right = getValue(b);
      if (left === right) return 0;
      if (left == null) return 1;
      if (right == null) return -1;
      const result = String(left).localeCompare(String(right), undefined, { numeric: true, sensitivity: 'base' });
      return sort.direction === 'asc' ? result : -result;
    });
  }, [activeColumn, rows, sort]);

  const toggleSort = (column: DataTableColumn<T>) => {
    if (!column.sortable) return;
    setSort((current) => current?.id !== column.id ? { id: column.id, direction: 'asc' } : { id: column.id, direction: current.direction === 'asc' ? 'desc' : 'asc' });
  };

  return (
    <div className={cn('overflow-x-auto rounded-lg border border-border-subtle', className)}>
      <table className={cn('w-full text-left', density === 'compact' ? 'data-table-compact' : 'data-table-comfortable')}>
        {caption ? <caption className="sr-only">{caption}</caption> : null}
        <thead>
          <tr>
            {columns.map((column) => {
              const active = sort?.id === column.id;
              return (
                <th key={column.id} scope="col" aria-sort={active ? (sort.direction === 'asc' ? 'ascending' : 'descending') : column.sortable ? 'none' : undefined} className={column.className}>
                  {column.sortable ? (
                    <button type="button" className="inline-flex items-center gap-1.5 text-left" onClick={() => toggleSort(column)}>
                      {column.header}
                      {active ? (sort.direction === 'asc' ? <ArrowUp size={13} aria-hidden="true" /> : <ArrowDown size={13} aria-hidden="true" />) : <ArrowUpDown size={13} aria-hidden="true" className="opacity-40" />}
                    </button>
                  ) : column.header}
                </th>
              );
            })}
          </tr>
        </thead>
        <tbody>
          {sortedRows.length ? sortedRows.map((row, index) => (
            <tr key={getRowId?.(row, index) ?? index}>
              {columns.map((column) => <td key={column.id} className={column.className}>{column.render ? column.render(row) : column.accessor ? String(row[column.accessor] ?? '—') : null}</td>)}
            </tr>
          )) : (
            <tr><td colSpan={columns.length} className="px-4 py-8 text-center text-sm text-slate-500">{empty ?? 'No records'}</td></tr>
          )}
        </tbody>
      </table>
    </div>
  );
}
