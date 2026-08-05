import React from 'react';
import { motion, useReducedMotion } from 'framer-motion';
import { useTranslation } from 'react-i18next';
import { EChart } from '@/components/common/EChart';
import { FeedbackState, type FeedbackStateProps } from '@/components/common/FeedbackState';
import { getProductizedErrorState } from '@/lib/async-state';
import type { AppChartOption } from '@/lib/echarts';
import { cn } from '@/lib/utils';

interface ChartCardProps {
  title: string;
  description?: string;
  anomalyNote?: string;
  option: AppChartOption;
  loading?: boolean;
  isEmpty?: boolean;
  error?: unknown;
  onRetry?: () => void;
  height?: string | number;
  className?: string;
  extra?: React.ReactNode;
  embedded?: boolean;
  emptyState?: Partial<Pick<FeedbackStateProps, 'title' | 'description' | 'impact' | 'nextStep'>>;
}

export const ChartCard: React.FC<ChartCardProps> = ({
  title,
  description,
  anomalyNote,
  option,
  loading = false,
  isEmpty = false,
  error,
  onRetry,
  height = 350,
  className,
  extra,
  embedded = false,
  emptyState,
}) => {
  const { t } = useTranslation();
  const reducedMotion = useReducedMotion();
  const emptyTitle = emptyState?.title ?? t('ui.chart.emptyTitle', { title });
  const emptyDescription = emptyState?.description ?? t('ui.chart.emptyDescription');
  const emptyImpact = emptyState?.impact;
  const emptyNextStep = emptyState?.nextStep;
  const shouldShowDetails = !embedded && Boolean(emptyImpact || emptyNextStep);
  const errorState = error
    ? getProductizedErrorState(error, {
        resourceLabel: t('ui.chart.resourceLabel', { title }),
        taskLabel: t('ui.chart.taskLabel', { title }),
        retryActionLabel: t('ui.chart.retryAction'),
      })
    : null;
  const containerStyle = errorState || isEmpty ? { minHeight: height } : { height };

  return (
    <div
      className={cn(
        embedded
          ? 'relative overflow-hidden rounded-lg'
          : 'surface-panel rounded-xl overflow-hidden group transition-shadow duration-200 hover:shadow-[var(--shadow-lg)]',
        className
      )}
    >
      {!embedded && <div className="flex flex-wrap items-start justify-between gap-3 border-b border-border-subtle bg-surface-sunken px-4 py-4 relative z-10 sm:px-6">
        <div className="flex min-w-0 items-start gap-3">
          <div className="h-1.5 w-1.5 rounded-full bg-primary" aria-hidden="true" />
          <div>
            <h3 className="type-section-title text-slate-800 dark:text-white/85">{title}</h3>
            {description ? <p className="type-body-muted mt-1 max-w-2xl">{description}</p> : null}
          </div>
        </div>
        <div className="flex max-w-full flex-wrap items-center justify-end gap-2">
          {extra}
        </div>
      </div>}
      
      <div className={cn('relative z-10', embedded ? 'p-0' : 'p-4 sm:p-8')} style={containerStyle}>
        {anomalyNote && !loading && !errorState && !isEmpty ? (
          <div className="anomaly-note mb-4" role="note">
            <span aria-hidden="true">!</span>
            <span>{anomalyNote}</span>
          </div>
        ) : null}
        {loading ? (
          <div className="absolute inset-0 z-20 bg-surface/90 dark:bg-surface/90">
            <FeedbackState
              kind="loading"
              compact
              className="h-full border-0 bg-transparent p-0 shadow-none"
              title={t('ui.chart.loadingTitle')}
              description={t('ui.chart.loadingDescription')}
              impact={t('ui.chart.loadingImpact')}
              nextStep={t('ui.chart.loadingNextStep')}
            />
          </div>
        ) : errorState ? (
          <FeedbackState
            kind={errorState.kind}
            compact
            className={cn('h-full', embedded && 'border-0 bg-transparent px-4 py-6 shadow-none')}
            title={errorState.title}
            description={errorState.description}
            impact={shouldShowDetails ? errorState.impact : undefined}
            nextStep={shouldShowDetails ? errorState.nextStep : undefined}
            primaryAction={
              onRetry
                ? {
                    label: t('ui.chart.retryAction'),
                    onClick: onRetry,
                  }
                : undefined
            }
          />
        ) : isEmpty ? (
          <FeedbackState
            kind="empty"
            compact
            className={cn('min-h-full', embedded && 'border-0 bg-transparent px-4 py-6 shadow-none')}
            title={emptyTitle}
            description={emptyDescription}
            impact={shouldShowDetails ? emptyImpact : undefined}
            nextStep={shouldShowDetails ? emptyNextStep : undefined}
          />
        ) : (
          <motion.div
            initial={reducedMotion ? false : { opacity: 0, scale: 0.98 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={reducedMotion ? { duration: 0 } : { duration: 0.42, ease: 'easeOut' }}
            style={{ height: '100%', width: '100%' }}
          >
            <EChart option={option} ariaLabel={description ? `${title}. ${description}` : title} style={{ height: '100%', width: '100%' }} />
          </motion.div>
        )}
      </div>
    </div>
  );
};
