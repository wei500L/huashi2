import React from 'react';
import { AlertTriangle, Inbox, LoaderCircle, RefreshCcw, ShieldAlert } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import type { AsyncStateKind } from '@/lib/async-state';
import { StatusBadge } from '@/components/common';
import { cn } from '@/lib/utils';

export type FeedbackStateAction = {
  label: string;
  onClick: () => void;
  tone?: 'primary' | 'secondary';
};

export type FeedbackStateProps = {
  kind: AsyncStateKind;
  title: string;
  description: string;
  impact?: string;
  nextStep?: string;
  eyebrow?: string;
  className?: string;
  compact?: boolean;
  primaryAction?: FeedbackStateAction;
  secondaryAction?: FeedbackStateAction;
};

const KIND_META: Record<
  AsyncStateKind,
  {
    eyebrowKey: string;
    icon: React.ComponentType<{ className?: string; size?: number }>;
    shellClassName: string;
    badgeClassName: string;
  }
> = {
  loading: {
    eyebrowKey: 'ui.feedback.kind.loading',
    icon: LoaderCircle,
    shellClassName: 'border-sky-500/15 bg-sky-500/[0.05] text-slate-700 dark:text-white/80',
    badgeClassName: 'border-sky-500/20 bg-sky-500/10 text-sky-700 dark:text-sky-300',
  },
  empty: {
    eyebrowKey: 'ui.feedback.kind.empty',
    icon: Inbox,
    shellClassName: 'border-slate-200/80 bg-white/70 text-slate-700 dark:border-white/10 dark:bg-white/5 dark:text-white/80',
    badgeClassName: 'border-slate-200/80 bg-white/85 text-slate-500 dark:border-white/10 dark:bg-white/5 dark:text-white/45',
  },
  permission: {
    eyebrowKey: 'ui.feedback.kind.permission',
    icon: ShieldAlert,
    shellClassName: 'border-amber-500/20 bg-amber-500/[0.08] text-amber-900 dark:text-amber-100',
    badgeClassName: 'border-amber-500/20 bg-amber-500/10 text-amber-700 dark:text-amber-300',
  },
  error: {
    eyebrowKey: 'ui.feedback.kind.error',
    icon: AlertTriangle,
    shellClassName: 'border-rose-500/20 bg-rose-500/[0.07] text-rose-900 dark:text-rose-100',
    badgeClassName: 'border-rose-500/20 bg-rose-500/10 text-rose-700 dark:text-rose-300',
  },
  retry: {
    eyebrowKey: 'ui.feedback.kind.retry',
    icon: RefreshCcw,
    shellClassName: 'border-sky-500/20 bg-sky-500/[0.07] text-sky-900 dark:text-sky-100',
    badgeClassName: 'border-sky-500/20 bg-sky-500/10 text-sky-700 dark:text-sky-300',
  },
};

function ActionButton({ action }: { action: FeedbackStateAction }) {
  const toneClassName =
    action.tone === 'secondary'
      ? 'border border-slate-200/80 bg-white/80 text-slate-700 dark:border-white/10 dark:bg-slate-950/40 dark:text-white/75'
      : 'btn-liquid text-white';

  return (
    <button
      type="button"
      onClick={action.onClick}
      className={cn('inline-flex items-center justify-center rounded-2xl px-5 py-3 text-sm font-bold', toneClassName)}
    >
      {action.label}
    </button>
  );
}

export const FeedbackState: React.FC<FeedbackStateProps> = ({
  kind,
  title,
  description,
  impact,
  nextStep,
  eyebrow,
  className,
  compact = false,
  primaryAction,
  secondaryAction,
}) => {
  const { t } = useTranslation();
  const meta = KIND_META[kind];
  const Icon = meta.icon;
  const iconClassName = kind === 'loading' ? 'animate-spin' : undefined;
  const resolvedEyebrow = eyebrow ?? t(meta.eyebrowKey);

  return (
    <section
      className={cn(
        'rounded-[2rem] border p-6 md:p-7',
        meta.shellClassName,
        compact && 'h-full px-5 py-6 md:px-6 md:py-6',
        className
      )}
    >
      <div className={cn('flex h-full flex-col', compact ? 'justify-center gap-4 text-center' : 'gap-5')}>
        <StatusBadge
          label={resolvedEyebrow}
          icon={<Icon size={14} className={iconClassName} />}
          className={cn('gap-2 px-4 py-2 text-[10px] uppercase tracking-[0.24em]', meta.badgeClassName, compact && 'mx-auto')}
        />

        <div className={cn('space-y-3', compact && 'mx-auto max-w-md')}>
          <h3 className={cn('font-black tracking-tight', compact ? 'text-xl' : 'text-2xl text-slate-900 dark:text-white')}>
            {title}
          </h3>
          <p className={cn('leading-7 opacity-90', compact ? 'text-sm' : 'text-sm md:text-base')}>{description}</p>
        </div>

        {(impact || nextStep) && (
          <div className={cn('grid gap-3', compact ? 'mx-auto w-full max-w-md text-left' : 'md:grid-cols-2')}>
            {impact && (
              <div className="rounded-[1.4rem] border border-current/10 bg-white/45 px-4 py-4 dark:bg-black/10">
                <div className="text-[10px] font-black uppercase tracking-[0.24em] opacity-60">{t('ui.feedback.impactHeading')}</div>
                <p className="mt-2 text-sm leading-6 opacity-90">{impact}</p>
              </div>
            )}
            {nextStep && (
              <div className="rounded-[1.4rem] border border-current/10 bg-white/45 px-4 py-4 dark:bg-black/10">
                <div className="text-[10px] font-black uppercase tracking-[0.24em] opacity-60">{t('ui.feedback.nextStepHeading')}</div>
                <p className="mt-2 text-sm leading-6 opacity-90">{nextStep}</p>
              </div>
            )}
          </div>
        )}

        {(primaryAction || secondaryAction) && (
          <div className={cn('flex flex-col gap-3 sm:flex-row', compact && 'justify-center')}>
            {primaryAction ? <ActionButton action={primaryAction} /> : null}
            {secondaryAction ? <ActionButton action={{ ...secondaryAction, tone: secondaryAction.tone ?? 'secondary' }} /> : null}
          </div>
        )}
      </div>
    </section>
  );
};
