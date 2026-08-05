import React from 'react';
import { Info } from 'lucide-react';
import { cn } from '@/lib/utils';
import { trainingModeMeta } from '@/lib/training-modes';

export interface TrainingModeSummaryCardProps {
  mode: string;
  count?: number;
  reason?: string;
  onClick?: () => void;
  disabled?: boolean;
  className?: string;
}

export const TrainingModeSummaryCard: React.FC<TrainingModeSummaryCardProps> = ({
  mode,
  count,
  reason,
  onClick,
  disabled,
  className,
}) => {
  const meta = trainingModeMeta(mode);
  const Tag = onClick ? 'button' : 'div';

  return (
    <Tag
      {...(onClick
        ? {
            type: 'button' as const,
            onClick,
            disabled,
          }
        : {})}
      className={cn(
        'text-left rounded-2xl border border-border-subtle bg-surface/70 p-4 transition-colors',
        onClick ? 'hover:border-primary/40 disabled:opacity-60' : '',
        className
      )}
    >
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <div className="text-[10px] uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">{meta.label}</div>
          <div className="mt-2 text-lg font-semibold text-slate-900 dark:text-white">{meta.plainTitle}</div>
        </div>
        <span title={meta.tooltip} aria-label={meta.tooltip} className="mt-1 shrink-0">
          <Info size={16} className="text-slate-400 dark:text-white/35" />
        </span>
      </div>
      <div className="mt-3 text-sm leading-6 text-slate-600 dark:text-white/60">{meta.purpose}</div>
      <div className="mt-3 text-sm text-slate-500 dark:text-white/45">{meta.bestFor}</div>
      {count !== undefined ? (
        <div className="mt-4 text-sm font-bold text-primary">{count} 题</div>
      ) : null}
      {reason ? <div className="mt-3 text-sm text-slate-500 dark:text-white/45">{reason}</div> : null}
    </Tag>
  );
};
