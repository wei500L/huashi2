import React from 'react';
import { RefreshCcw } from 'lucide-react';
import { motion } from 'framer-motion';
import { useTranslation } from 'react-i18next';
import { EChart } from '@/components/common/EChart';
import { FeedbackState, type FeedbackStateProps } from '@/components/common/FeedbackState';
import { getProductizedErrorState } from '@/lib/async-state';
import type { AppChartOption } from '@/lib/echarts';
import { cn } from '@/lib/utils';

interface ChartCardProps {
  title: string;
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
          ? 'relative overflow-hidden rounded-[2rem]'
          : 'liquid-glass-panel border-beam fluid-texture rounded-[2.5rem] overflow-hidden group transition-all duration-700 hover:shadow-[0_20px_60px_rgba(0,0,0,0.3)] dark:hover:shadow-[0_30px_80px_rgba(0,0,0,0.6)]',
        className
      )}
    >
      {!embedded && <div className="px-8 py-6 border-b border-white/10 flex items-center justify-between bg-white/5 backdrop-blur-md relative z-10">
        <div className="flex items-center gap-3">
          <div className="w-1.5 h-1.5 rounded-full bg-primary animate-pulse shadow-[0_0_8px_rgba(139,92,246,0.8)]" />
          <h3 className="text-base font-black tracking-tight text-slate-800 dark:text-white/85">{title}</h3>
        </div>
        <div className="flex items-center gap-4">
          {extra}
          {loading && <RefreshCcw className="animate-spin text-primary drop-shadow-[0_0_8px_rgba(139,92,246,0.8)]" size={16} />}
        </div>
      </div>}
      
      <div className={cn('relative z-10', embedded ? 'p-0' : 'p-8')} style={containerStyle}>
        {loading ? (
          <div className="absolute inset-0 z-20 bg-black/[0.02] backdrop-blur-md dark:bg-black/20">
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
            initial={{ opacity: 0, scale: 0.98 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ duration: 1, ease: 'easeOut' }}
            style={{ height: '100%', width: '100%' }}
          >
            <EChart option={option} style={{ height: '100%', width: '100%' }} />
          </motion.div>
        )}
      </div>
    </div>
  );
};
