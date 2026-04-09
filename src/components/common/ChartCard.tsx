import React from 'react';
import { RefreshCcw } from 'lucide-react';
import { motion } from 'framer-motion';
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
  emptyState,
}) => {
  const emptyTitle = emptyState?.title ?? `当前暂无可展示的${title}`;
  const emptyDescription = emptyState?.description ?? '系统已经完成本次查询，但还没有足够的数据生成图表。';
  const emptyImpact = emptyState?.impact ?? '这不会影响当前页面其他内容，你仍然可以继续查看文字信息或切换筛选条件。';
  const emptyNextStep = emptyState?.nextStep ?? '请稍后再回来查看，或先去完成相关学习任务以生成新的数据。';
  const errorState = error
    ? getProductizedErrorState(error, {
        resourceLabel: `${title}图表`,
        taskLabel: `查看${title}`,
        retryActionLabel: '重新加载图表',
      })
    : null;

  return (
    <div
      className={cn(
        'liquid-glass-panel border-beam fluid-texture rounded-[2.5rem] overflow-hidden group transition-all duration-700 hover:shadow-[0_20px_60px_rgba(0,0,0,0.3)] dark:hover:shadow-[0_30px_80px_rgba(0,0,0,0.6)]',
        className
      )}
    >
      <div className="px-8 py-6 border-b border-white/10 flex items-center justify-between bg-white/5 backdrop-blur-md relative z-10">
        <div className="flex items-center gap-3">
          <div className="w-1.5 h-1.5 rounded-full bg-primary animate-pulse shadow-[0_0_8px_rgba(139,92,246,0.8)]" />
          <h3 className="text-[10px] font-black dark:font-black tracking-[0.3em] uppercase text-slate-500 dark:text-white/40">{title}</h3>
        </div>
        <div className="flex items-center gap-4">
          {extra}
          {loading && <RefreshCcw className="animate-spin text-primary drop-shadow-[0_0_8px_rgba(139,92,246,0.8)]" size={16} />}
        </div>
      </div>
      
      <div className="p-8 relative z-10" style={{ height }}>
        {loading ? (
          <div className="absolute inset-0 z-20 bg-black/[0.02] backdrop-blur-md dark:bg-black/20">
            <FeedbackState
              kind="loading"
              compact
              className="h-full border-0 bg-transparent p-0 shadow-none"
              title="正在整理图表数据"
              description="系统正在准备可视化结果，当前页面其他内容不受影响。"
              impact="图表暂时无法展示，但不会影响你继续浏览当前页面。"
              nextStep="请稍等片刻；如果长时间没有结果，可稍后刷新页面。"
            />
          </div>
        ) : errorState ? (
          <FeedbackState
            kind={errorState.kind}
            compact
            className="h-full"
            title={errorState.title}
            description={errorState.description}
            impact={errorState.impact}
            nextStep={errorState.nextStep}
            primaryAction={
              onRetry
                ? {
                    label: '重新加载图表',
                    onClick: onRetry,
                  }
                : undefined
            }
          />
        ) : isEmpty ? (
          <FeedbackState
            kind="empty"
            compact
            className="h-full"
            title={emptyTitle}
            description={emptyDescription}
            impact={emptyImpact}
            nextStep={emptyNextStep}
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
