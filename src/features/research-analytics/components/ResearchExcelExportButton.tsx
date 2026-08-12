import React from 'react';
import { Download } from 'lucide-react';

export const ResearchExcelExportButton: React.FC<{
  onClick: () => void;
  exporting?: boolean;
  disabled?: boolean;
  variant?: 'primary' | 'secondary';
  children?: React.ReactNode;
}> = ({ onClick, exporting = false, disabled = false, variant = 'secondary', children }) => {
  const className = variant === 'primary'
    ? 'btn-liquid inline-flex min-h-11 items-center justify-center gap-2 px-5 py-2.5 text-sm text-white disabled:opacity-40'
    : 'inline-flex min-h-11 items-center justify-center gap-2 rounded-2xl border border-slate-200 px-4 py-2.5 text-sm font-bold disabled:opacity-40 dark:border-white/10';
  return (
    <button type="button" disabled={disabled || exporting} onClick={onClick} className={className}>
      <Download size={16} />
      {exporting ? '正在生成 Excel…' : children || '导出 Excel'}
    </button>
  );
};
