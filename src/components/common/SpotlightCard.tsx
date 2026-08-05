import React from 'react';
import { cn } from '@/lib/utils';

/**
 * A single, deliberately quiet emphasis surface for teacher decisions.
 * It has no motion or ambient glow; the border and label carry the hierarchy.
 */
export const SpotlightCard: React.FC<{
  eyebrow?: string;
  title: string;
  children: React.ReactNode;
  className?: string;
}> = ({ eyebrow, title, children, className }) => (
  <section
    aria-labelledby="teacher-spotlight-title"
    className={cn(
      'rounded-[2rem] border border-primary/30 bg-primary/[0.045] p-6 shadow-sm dark:bg-primary/[0.08]',
      className
    )}
  >
    {eyebrow ? <div className="text-[11px] font-black uppercase tracking-[0.24em] text-primary/80">{eyebrow}</div> : null}
    <h2 id="teacher-spotlight-title" className="mt-2 text-xl font-black text-slate-900 dark:text-white">
      {title}
    </h2>
    <div className="mt-4">{children}</div>
  </section>
);

