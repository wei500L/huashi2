import React, { useEffect, useState } from 'react';
import { LucideIcon } from 'lucide-react';
import { motion, useReducedMotion } from 'framer-motion';
import { useTranslation } from 'react-i18next';
import { cn } from '@/lib/utils';

export const RouteSkeleton: React.FC = () => {
  const { t } = useTranslation();

  return (
  <div
    role="status"
    aria-live="polite"
    aria-busy="true"
    className="min-h-screen bg-background px-6 py-10"
  >
    <div className="mx-auto max-w-6xl space-y-8">
      <div className="rounded-xl border border-sky-500/20 bg-sky-500/[0.06] px-5 py-4 text-sky-900 dark:text-sky-100">
        <div className="text-sm font-black">{t('ui.skeleton.route.title')}</div>
        <div className="mt-2 grid gap-1 text-xs leading-5 opacity-75 md:grid-cols-3">
          <span>{t('ui.skeleton.route.happened')}</span>
          <span>{t('ui.skeleton.route.safety')}</span>
          <span>{t('ui.skeleton.route.nextStep')}</span>
        </div>
      </div>
      <div className="space-y-3">
        <div className="h-3 w-24 animate-pulse rounded-full bg-slate-200/80 dark:bg-white/10" />
        <div className="h-10 w-72 animate-pulse rounded-full bg-slate-200/80 dark:bg-white/10" />
        <div className="h-4 w-[28rem] animate-pulse rounded-full bg-slate-200/70 dark:bg-white/10" />
      </div>
      <div className="grid gap-6 xl:grid-cols-[1.2fr_0.8fr]">
        <div className="h-72 animate-pulse rounded-[2.5rem] border border-slate-200/80 bg-white/60 dark:border-white/10 dark:bg-white/5" />
        <div className="grid gap-6">
          <div className="h-32 animate-pulse rounded-[2.2rem] border border-slate-200/80 bg-white/60 dark:border-white/10 dark:bg-white/5" />
          <div className="h-32 animate-pulse rounded-[2.2rem] border border-slate-200/80 bg-white/60 dark:border-white/10 dark:bg-white/5" />
        </div>
      </div>
      <div className="grid gap-6 md:grid-cols-3">
        {Array.from({ length: 3 }).map((_, index) => (
          <div
            key={index}
            className="h-28 animate-pulse rounded-[2rem] border border-slate-200/80 bg-white/60 dark:border-white/10 dark:bg-white/5"
          />
        ))}
      </div>
    </div>
  </div>
  );
};

export const PanelSkeleton: React.FC<{ className?: string }> = ({ className }) => {
  const { t } = useTranslation();

  return (
  <div
    role="status"
    aria-live="polite"
    aria-busy="true"
    aria-label={t('ui.skeleton.panel.label')}
    className={cn('rounded-xl surface-panel p-8', className)}
  >
    <div className="space-y-5">
      <div className="rounded-lg border border-sky-500/15 bg-sky-500/[0.05] px-4 py-3 text-xs leading-5 text-sky-800 dark:text-sky-200">
        <div className="font-black">{t('ui.skeleton.panel.title')}</div>
        <div className="mt-1 opacity-75">{t('ui.skeleton.panel.summary')}</div>
      </div>
      <div className="h-3 w-24 animate-pulse rounded-full bg-slate-200/80 dark:bg-white/10" />
      <div className="grid gap-5 md:grid-cols-2">
        <div className="h-32 animate-pulse rounded-[2rem] border border-slate-200/80 bg-white/65 dark:border-white/10 dark:bg-white/5" />
        <div className="h-32 animate-pulse rounded-[2rem] border border-slate-200/80 bg-white/65 dark:border-white/10 dark:bg-white/5" />
      </div>
      <div className="h-20 animate-pulse rounded-[2rem] border border-slate-200/80 bg-white/55 dark:border-white/10 dark:bg-white/5" />
      <div className="space-y-3">
        {Array.from({ length: 4 }).map((_, index) => (
          <div
            key={index}
            className="h-16 animate-pulse rounded-[1.6rem] border border-slate-200/80 bg-white/65 dark:border-white/10 dark:bg-white/5"
          />
        ))}
      </div>
    </div>
  </div>
  );
};

type StatusTone = 'neutral' | 'info' | 'success' | 'warning' | 'danger';

const STATUS_TONE_CLASSNAME: Record<StatusTone, string> = {
  neutral: 'border-slate-200/80 bg-white/85 text-slate-500 dark:border-white/10 dark:bg-white/5 dark:text-white/45',
  info: 'border-sky-500/20 bg-sky-500/10 text-sky-700 dark:text-sky-300',
  success: 'border-emerald-500/20 bg-emerald-500/10 text-emerald-700 dark:text-emerald-300',
  warning: 'border-amber-500/20 bg-amber-500/10 text-amber-700 dark:text-amber-300',
  danger: 'border-rose-500/20 bg-rose-500/10 text-rose-700 dark:text-rose-300',
};

export const SectionEyebrow: React.FC<{ children: React.ReactNode; className?: string }> = ({ children, className }) => (
  <div
    className={cn(
      'type-metadata',
      className
    )}
  >
    {children}
  </div>
);

export const StatusBadge: React.FC<{ label: string; tone?: StatusTone; className?: string; icon?: React.ReactNode }> = ({
  label,
  tone = 'neutral',
  className,
  icon,
}) => (
  <span
    className={cn(
      'inline-flex items-center rounded-full border px-3 py-1 text-xs font-bold',
      STATUS_TONE_CLASSNAME[tone],
      className
    )}
  >
    {icon ? <span className="shrink-0">{icon}</span> : null}
    {label}
  </span>
);

// Number Animation Component
export const AnimatedNumber: React.FC<{ value: number; format?: (v: number) => string }> = ({ value, format = (v) => Math.round(v).toString() }) => {
  const reducedMotion = useReducedMotion();
  const [displayValue, setDisplayValue] = useState(() => (reducedMotion ? value : 0));

  useEffect(() => {
    if (reducedMotion) {
      setDisplayValue(value);
      return;
    }

    let startTimestamp: number;
    let frameId = 0;
    const duration = 560;

    const step = (timestamp: number) => {
      if (!startTimestamp) startTimestamp = timestamp;
      const progress = Math.min((timestamp - startTimestamp) / duration, 1);
      const easeProgress = progress === 1 ? 1 : 1 - Math.pow(2, -10 * progress);
      setDisplayValue(easeProgress * value);
      if (progress < 1) {
        frameId = window.requestAnimationFrame(step);
      }
    };
    frameId = window.requestAnimationFrame(step);
    return () => window.cancelAnimationFrame(frameId);
  }, [reducedMotion, value]);

  return <span>{format(displayValue)}</span>;
};

// 1. StatCard
interface StatCardProps {
  title: string;
  value: string | number;
  icon: LucideIcon;
  trend?: { value: number; isUp: boolean };
  trendLabel?: string;
  className?: string;
  color?: string;
}

export const StatCard: React.FC<StatCardProps> = ({
  title,
  value,
  icon: Icon,
  trend,
  trendLabel,
  className,
  color = 'text-primary',
}) => {
  const { t } = useTranslation();
  const iconGlowClass = (() => {
    switch (color) {
      case 'text-blue-500':
      case 'text-blue-600':
        return 'text-glow-blue';
      case 'text-rose-500':
      case 'text-rose-600':
        return 'text-glow-rose';
      case 'text-emerald-500':
      case 'text-emerald-600':
        return 'text-glow-emerald';
      case 'text-amber-500':
      case 'text-amber-600':
        return 'text-glow-amber';
      case 'text-primary':
        return 'text-glow-primary';
      default:
        return color;
    }
  })();

  const numericValue = typeof value === 'string' ? parseFloat(value.replace(/[^0-9.-]+/g, '')) : value;
  const suffix = typeof value === 'string' ? value.replace(/[0-9.-]+/g, '') : '';
  const resolvedTrendLabel = trendLabel ?? t('ui.statCard.recentPeriod');

  return (
    <div
      className={cn(
        'surface-card p-6 rounded-xl flex flex-col justify-between transition-shadow duration-200 hover:shadow-[var(--shadow-md)]',
        className
      )}
    >
      <div className="flex items-start justify-between relative z-10">
        <div>
          <SectionEyebrow className="mb-1">{title}</SectionEyebrow>
          <h3 className={cn('stat-card-value type-numeric text-3xl font-semibold tabular-nums', iconGlowClass)}>
            {!isNaN(numericValue) ? (
              <>
                <AnimatedNumber value={numericValue} />
                <span className="stat-card-value-suffix">{suffix}</span>
              </>
            ) : value}
          </h3>
        </div>
        <div
          className={cn(
            'p-2.5 rounded-lg bg-surface-sunken border border-border-subtle relative shadow-sm',
            color
          )}
        >
          <Icon size={22} />
        </div>
      </div>

      {trend && (
        <div className="mt-6 relative z-10">
          <div
            className={cn(
              'inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[10px] font-black border backdrop-blur-md transition-all duration-200',
              trend.isUp
                ? 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/20'
                : 'bg-rose-500/10 text-rose-600 dark:text-rose-400 border-rose-500/20'
            )}
          >
            <span className="text-xs">{trend.isUp ? '↑' : '↓'}</span>
            <span>
              <AnimatedNumber value={trend.value} />%
            </span>
            <span className="text-slate-400 dark:text-white/20 font-medium ml-1">{resolvedTrendLabel}</span>
          </div>
        </div>
      )}
    </div>
  );
};

// 2. PageHeader
interface PageHeaderProps {
  title: string;
  eyebrow?: string;
  subtitle?: string;
  breadcrumbs?: string[];
  actions?: React.ReactNode;
  compact?: boolean;
}

export const PageHeader: React.FC<PageHeaderProps> = ({ title, eyebrow, subtitle, breadcrumbs, actions, compact = false }) => {
  const reducedMotion = useReducedMotion();

  return (
  <div className={cn('flex min-w-0 flex-col justify-between sm:flex-row sm:items-start md:items-center', compact ? 'mb-5 gap-3 sm:mb-6 sm:gap-4' : 'mb-6 gap-4 sm:mb-8 sm:gap-6 md:mb-10')}>
    <motion.div className="min-w-0 flex-1" initial={reducedMotion ? false : { x: -20, opacity: 0 }} animate={{ x: 0, opacity: 1 }} transition={reducedMotion ? { duration: 0 } : { duration: 0.42, ease: 'easeOut' }}>
      {breadcrumbs && (
        <div className="mb-3 flex min-w-0 flex-wrap items-center gap-x-3 gap-y-1 text-[10px] font-bold uppercase tracking-[0.2em] text-slate-400 dark:text-white/30">
          {breadcrumbs.map((b, i) => (
            <React.Fragment key={i}>
              <span className="max-w-full truncate text-slate-400 dark:text-white/30">{b}</span>
              {i < breadcrumbs.length - 1 && <span className="text-slate-200 dark:text-white/10">/</span>}
            </React.Fragment>
          ))}
        </div>
      )}
      {eyebrow ? <SectionEyebrow className="mb-3">{eyebrow}</SectionEyebrow> : null}
      <h1 className="type-page-title break-words text-slate-900 dark:text-white" title={title}>{title}</h1>
      {subtitle && <p className="type-body-muted mt-2 max-w-xl sm:mt-3">{subtitle}</p>}
    </motion.div>
    {actions && (
      <motion.div initial={reducedMotion ? false : { x: 20, opacity: 0 }} animate={{ x: 0, opacity: 1 }} transition={reducedMotion ? { duration: 0 } : { duration: 0.42, ease: 'easeOut' }} className="page-actions w-full shrink-0 sm:w-auto sm:justify-end">
        {actions}
      </motion.div>
    )}
  </div>
  );
};

export { FeedbackState } from './FeedbackState';
export type { FeedbackStateAction, FeedbackStateProps } from './FeedbackState';
export { ConfirmationDialog } from './ConfirmationDialog';
export { Tabs } from './Tabs';
export type { TabItem, TabsProps } from './Tabs';
export { Pagination } from './Pagination';
export { DataTable } from './DataTable';
export type { DataTableColumn } from './DataTable';
export { WorkflowStepper } from './WorkflowStepper';
export type { WorkflowStage, WorkflowStageStatus, WorkflowStepperProps } from './WorkflowStepper';
export { DecisionCard } from './DecisionCard';
export { RegistrationQrCode } from './RegistrationQrCode';
