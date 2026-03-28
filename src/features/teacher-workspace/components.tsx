import React from 'react';
import { Link } from 'react-router-dom';
import { ArrowRight } from 'lucide-react';
import { cn } from '@/lib/utils';

export const WorkspaceHero: React.FC<{
  eyebrow: string;
  title: string;
  subtitle: string;
  meta?: string | null;
}> = ({ eyebrow, title, subtitle, meta }) => (
  <section className="rounded-[3rem] liquid-glass-panel edge-light p-8 md:p-10">
    <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">{eyebrow}</div>
    <div className="mt-4 text-4xl font-black tracking-tight text-slate-900 dark:text-white">{title}</div>
    <div className="mt-4 max-w-3xl text-base leading-7 text-slate-500 dark:text-white/45">{subtitle}</div>
    {meta && (
      <div className="mt-6 inline-flex rounded-full border border-slate-200/80 bg-white/70 px-4 py-2 text-xs font-bold text-slate-500 dark:border-white/10 dark:bg-white/5 dark:text-white/45">
        {meta}
      </div>
    )}
  </section>
);

export const MetricGrid: React.FC<{
  items: Array<{ id: string; label: string; value: number | string; hint: string }>;
}> = ({ items }) => (
  <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
    {items.map((item) => (
      <div key={item.id} className="rounded-[2rem] liquid-glass p-6">
        <div className="text-[10px] uppercase tracking-[0.26em] text-slate-400 dark:text-white/30">{item.label}</div>
        <div className="mt-3 text-3xl font-black text-slate-900 dark:text-white">{item.value}</div>
        <div className="mt-3 text-sm leading-6 text-slate-500 dark:text-white/45">{item.hint}</div>
      </div>
    ))}
  </div>
);

export const ActionGrid: React.FC<{
  title: string;
  description: string;
  actions: Array<{ id: string; label: string; description: string; to: string }>;
}> = ({ title, description, actions }) => (
  <section className="rounded-[2.5rem] liquid-glass-panel p-8">
    <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">{title}</div>
    <div className="mt-3 text-2xl font-black text-slate-900 dark:text-white">{description}</div>
    <div className="mt-6 grid gap-4 md:grid-cols-2 xl:grid-cols-3">
      {actions.map((action) => (
        <Link
          key={action.id}
          to={action.to}
          className="rounded-[1.8rem] border border-slate-200/80 bg-white/65 p-5 transition-all hover:border-primary/40 dark:border-white/10 dark:bg-white/5"
        >
          <div className="flex items-start justify-between gap-4">
            <div>
              <div className="font-black text-slate-900 dark:text-white">{action.label}</div>
              <div className="mt-2 text-sm leading-6 text-slate-500 dark:text-white/45">{action.description}</div>
            </div>
            <ArrowRight size={16} className="mt-1 shrink-0 text-primary" />
          </div>
        </Link>
      ))}
    </div>
  </section>
);

export const StatusBanner: React.FC<{
  title: string;
  description: string;
  actionLabel: string;
  to: string;
  tone?: 'action' | 'attention' | 'stable';
}> = ({ title, description, actionLabel, to, tone = 'action' }) => {
  const toneClassName = {
    action: 'border-sky-500/20 bg-sky-500/5 text-sky-700 dark:text-sky-300',
    attention: 'border-amber-500/20 bg-amber-500/10 text-amber-700 dark:text-amber-300',
    stable: 'border-emerald-500/20 bg-emerald-500/5 text-emerald-700 dark:text-emerald-300',
  }[tone];

  return (
    <div className={cn('rounded-[1.8rem] border px-5 py-5', toneClassName)}>
      <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <div className="font-black">{title}</div>
          <div className="mt-2 text-sm leading-6 opacity-90">{description}</div>
        </div>
        <Link to={to} className="inline-flex items-center gap-2 text-sm font-black">
          {actionLabel}
          <ArrowRight size={14} />
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
  <div className="rounded-[1.8rem] border border-dashed border-slate-300 bg-white/55 px-5 py-8 text-sm text-slate-500 dark:border-white/15 dark:bg-white/[0.02] dark:text-white/45">
    <div className="text-lg font-black text-slate-900 dark:text-white">{title}</div>
    <div className="mt-3 max-w-xl leading-7">{description}</div>
    <Link to={to} className="mt-5 inline-flex items-center gap-2 text-sm font-black text-primary">
      {actionLabel}
      <ArrowRight size={14} />
    </Link>
  </div>
);
