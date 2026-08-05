import React from 'react';
import { Link } from 'react-router-dom';
import { ArrowRight } from 'lucide-react';
import { cn } from '@/lib/utils';

export const WorkspaceHero: React.FC<{
  eyebrow: string;
  title: string;
  subtitle: string;
  meta?: string | null;
  actions?: React.ReactNode;
}> = ({ eyebrow, title, subtitle, meta, actions }) => (
  <section className="border-b border-slate-200/80 pb-6 dark:border-white/10">
    <div className="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
      <div className="min-w-0">
        <div className="type-metadata">{eyebrow}</div>
        <h1 className="mt-2 truncate text-2xl font-black text-slate-900 sm:text-3xl dark:text-white" title={title}>
          {title}
        </h1>
        <div className="mt-2 max-w-3xl text-sm leading-6 text-slate-500 dark:text-white/50">{subtitle}</div>
      </div>
      <div className="flex shrink-0 flex-col items-start gap-3 lg:items-end">
        {meta ? (
          <div className="max-w-xs truncate text-xs font-semibold text-slate-500 dark:text-white/45" title={meta}>
            {meta}
          </div>
        ) : null}
        {actions}
      </div>
    </div>
  </section>
);

export const MetricGrid: React.FC<{
  items: Array<{ id: string; label: string; value: number | string; hint: string }>;
}> = ({ items }) => (
  <div className="grid overflow-hidden rounded-2xl border border-slate-200/80 bg-white/70 sm:grid-cols-2 xl:grid-cols-5 dark:border-white/10 dark:bg-white/[0.03]">
    {items.map((item, index) => (
      <div
        key={item.id}
        className={cn(
          'min-w-0 px-4 py-3.5',
          index > 0 && 'border-t border-slate-200/70 sm:border-l sm:border-t-0 dark:border-white/10',
          index === 2 && 'sm:border-l-0 xl:border-l',
          index === 4 && 'sm:border-l-0 xl:border-l'
        )}
        title={item.hint}
      >
        <div className="truncate text-[11px] font-semibold text-slate-500 dark:text-white/45">{item.label}</div>
        <div className="mt-1 text-xl font-black tabular-nums text-slate-900 dark:text-white">{item.value}</div>
      </div>
    ))}
  </div>
);

export const ActionGrid: React.FC<{
  title: string;
  description: string;
  actions: Array<{ id: string; label: string; description: string; to: string }>;
}> = ({ title, description, actions }) => (
  <section className="rounded-2xl border border-slate-200/80 bg-white/70 p-4 dark:border-white/10 dark:bg-white/[0.03]">
    <div className="flex flex-col gap-3 lg:flex-row lg:items-center">
      <div className="shrink-0 lg:w-44">
        <div className="text-xs font-black text-slate-900 dark:text-white">{title}</div>
        <div className="mt-1 text-xs leading-5 text-slate-500 dark:text-white/45">{description}</div>
      </div>
      <div className="grid flex-1 gap-2 sm:grid-cols-2 xl:grid-cols-3">
        {actions.map((action) => (
          <Link
            key={action.id}
            to={action.to}
            title={action.description}
            className="group flex min-w-0 items-center justify-between gap-3 rounded-xl border border-slate-200/70 bg-white/70 px-3.5 py-3 text-sm font-bold text-slate-700 transition-colors hover:border-primary/40 hover:text-primary dark:border-white/10 dark:bg-white/[0.04] dark:text-white/75"
          >
            <span className="truncate">{action.label}</span>
            <ArrowRight size={14} className="shrink-0 opacity-45 transition-transform group-hover:translate-x-0.5 group-hover:opacity-100" />
          </Link>
        ))}
      </div>
    </div>
  </section>
);

export const StatusBanner: React.FC<{
  title: string;
  description: string;
  actionLabel: string;
  to: string;
  tone?: 'action' | 'attention' | 'stable';
  index?: number;
}> = ({ title, description, actionLabel, to, tone = 'action', index = 0 }) => {
  const toneClassName = {
    action: 'border-l-sky-500 bg-sky-500/[0.035]',
    attention: 'border-l-amber-500 bg-amber-500/[0.055]',
    stable: 'border-l-emerald-500 bg-emerald-500/[0.035]',
  }[tone];

  return (
    <div
      className={cn(
        'workspace-todo-enter border-b border-l-2 border-b-slate-200/70 px-4 py-3 last:border-b-0 dark:border-b-white/10',
        toneClassName
      )}
      style={{ animationDelay: `${Math.min(index, 5) * 45}ms` }}
    >
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div className="min-w-0">
          <div className="text-sm font-black text-slate-900 dark:text-white">{title}</div>
          <div className="mt-0.5 line-clamp-2 text-xs leading-5 text-slate-500 dark:text-white/50">{description}</div>
        </div>
        <Link to={to} className="inline-flex shrink-0 items-center gap-1.5 text-xs font-black text-primary">
          {actionLabel}
          <ArrowRight size={13} />
        </Link>
      </div>
    </div>
  );
};

export const WorkspaceEmptyState: React.FC<{
  title: string;
  description: string;
  actionLabel: string;
  to: string;
}> = ({ title, description, actionLabel, to }) => (
  <div className="rounded-xl border border-dashed border-slate-300 bg-white/40 px-4 py-5 text-sm text-slate-500 dark:border-white/15 dark:bg-white/[0.02] dark:text-white/45">
    <div className="font-black text-slate-900 dark:text-white">{title}</div>
    <div className="mt-1.5 max-w-xl text-xs leading-5">{description}</div>
    <Link to={to} className="mt-3 inline-flex items-center gap-1.5 text-xs font-black text-primary">
      {actionLabel}
      <ArrowRight size={13} />
    </Link>
  </div>
);

export const WorkspaceSectionHeader: React.FC<{
  eyebrow: string;
  title: string;
  action?: React.ReactNode;
}> = ({ eyebrow, title, action }) => (
  <div className="flex items-end justify-between gap-4">
    <div className="min-w-0">
      <div className="text-[10px] font-bold uppercase tracking-[0.22em] text-slate-400 dark:text-white/30">{eyebrow}</div>
      <h2 className="mt-1 truncate text-base font-black text-slate-900 dark:text-white">{title}</h2>
    </div>
    {action ? <div className="shrink-0">{action}</div> : null}
  </div>
);
