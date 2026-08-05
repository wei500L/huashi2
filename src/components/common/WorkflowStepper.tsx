import React from 'react';
import { AlertTriangle, Check, Circle, Clock3, LockKeyhole, Save } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { cn } from '@/lib/utils';

export type WorkflowStageStatus = 'complete' | 'current' | 'warning' | 'blocked' | 'pending';

export type WorkflowStage = {
  key: string;
  label: string;
  status: WorkflowStageStatus;
  statusLabel: string;
  reason: string;
  fallback: string;
  saveState: string;
  nextAction: string;
  onSelect?: () => void;
  disabled?: boolean;
};

export type WorkflowStepperProps = {
  stages: WorkflowStage[];
  title?: string;
  description?: string;
  className?: string;
};

const STATUS_META: Record<WorkflowStageStatus, {
  icon: React.ComponentType<{ size?: number; className?: string }>;
  shell: string;
  badge: string;
  marker: string;
}> = {
  complete: {
    icon: Check,
    shell: 'border-emerald-500/20 bg-emerald-500/[0.06]',
    badge: 'border-emerald-500/20 bg-emerald-500/10 text-emerald-700 dark:text-emerald-300',
    marker: 'bg-emerald-500 text-white',
  },
  current: {
    icon: Clock3,
    shell: 'border-primary/35 bg-primary/[0.07]',
    badge: 'border-primary/25 bg-primary/10 text-primary',
    marker: 'bg-primary text-white',
  },
  warning: {
    icon: AlertTriangle,
    shell: 'border-amber-500/25 bg-amber-500/[0.07]',
    badge: 'border-amber-500/25 bg-amber-500/10 text-amber-700 dark:text-amber-300',
    marker: 'bg-amber-500 text-white',
  },
  blocked: {
    icon: LockKeyhole,
    shell: 'border-rose-500/25 bg-rose-500/[0.07]',
    badge: 'border-rose-500/25 bg-rose-500/10 text-rose-700 dark:text-rose-300',
    marker: 'bg-rose-500 text-white',
  },
  pending: {
    icon: Circle,
    shell: 'border-slate-200/80 bg-white/55 dark:border-white/10 dark:bg-white/[0.03]',
    badge: 'border-slate-200/80 bg-white/75 text-slate-500 dark:border-white/10 dark:bg-white/5 dark:text-white/45',
    marker: 'bg-slate-200 text-slate-500 dark:bg-white/10 dark:text-white/45',
  },
};

export const WorkflowStepper: React.FC<WorkflowStepperProps> = ({ stages, title, description, className }) => {
  const { t } = useTranslation();

  return (
    <section className={cn('rounded-[2.2rem] border border-slate-200/75 bg-white/55 p-5 dark:border-white/10 dark:bg-white/[0.025] md:p-6', className)}>
    {(title || description) && (
      <div className="mb-5 flex flex-wrap items-end justify-between gap-3">
        <div>
          {title && <h2 className="text-lg font-black text-slate-900 dark:text-white">{title}</h2>}
          {description && <p className="mt-2 max-w-3xl text-sm leading-6 text-slate-500 dark:text-white/45">{description}</p>}
        </div>
        <div className="text-xs font-bold text-slate-400 dark:text-white/30">{t('ui.workflow.stagesCount', { count: stages.length })}</div>
      </div>
    )}

    <ol className="grid gap-3 md:grid-cols-2 xl:grid-cols-3" aria-label={title || t('ui.workflow.defaultTitle')}>
      {stages.map((stage, index) => {
        const meta = STATUS_META[stage.status];
        const Icon = meta.icon;
        const content = (
          <>
            <div className="flex items-start justify-between gap-3">
              <div className="flex min-w-0 items-center gap-3">
                <span className={cn('flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-xs font-black', meta.marker)}>
                  {stage.status === 'complete' ? <Check size={15} /> : index + 1}
                </span>
                <div className="min-w-0 text-sm font-black text-slate-900 dark:text-white">{stage.label}</div>
              </div>
              <span className={cn('inline-flex shrink-0 items-center gap-1.5 rounded-full border px-2.5 py-1 text-[10px] font-bold', meta.badge)}>
                <Icon size={12} />
                {stage.statusLabel}
              </span>
            </div>

            <dl className="mt-4 grid gap-2 text-xs leading-5 text-slate-600 dark:text-white/55">
              <div><dt className="inline font-black text-slate-800 dark:text-white/80">{t('ui.workflow.reasonLabel')}: </dt><dd className="inline">{stage.reason}</dd></div>
              <div><dt className="inline font-black text-slate-800 dark:text-white/80">{t('ui.workflow.fallbackLabel')}: </dt><dd className="inline">{stage.fallback}</dd></div>
              <div className="flex items-start gap-1.5"><Save size={12} className="mt-1 shrink-0" /><div><dt className="inline font-black text-slate-800 dark:text-white/80">{t('ui.workflow.saveLabel')}: </dt><dd className="inline">{stage.saveState}</dd></div></div>
              <div><dt className="inline font-black text-slate-800 dark:text-white/80">{t('ui.workflow.nextLabel')}: </dt><dd className="inline">{stage.nextAction}</dd></div>
            </dl>
          </>
        );

        return (
          <li key={stage.key} className="min-w-0">
            {stage.onSelect ? (
              <button
                type="button"
                onClick={stage.onSelect}
                disabled={stage.disabled}
                aria-current={stage.status === 'current' ? 'step' : undefined}
                className={cn('h-full w-full rounded-[1.5rem] border p-4 text-left transition hover:-translate-y-0.5 hover:border-primary/30 disabled:cursor-not-allowed disabled:opacity-60', meta.shell)}
              >
                {content}
              </button>
            ) : (
              <div aria-current={stage.status === 'current' ? 'step' : undefined} className={cn('h-full rounded-[1.5rem] border p-4', meta.shell)}>
                {content}
              </div>
            )}
          </li>
        );
      })}
    </ol>
    </section>
  );
};
