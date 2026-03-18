import React from 'react';
import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';
import { LucideIcon } from 'lucide-react';

// Utility for Tailwind classes merging
function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

// 1. 统计卡片 (StatCard)
interface StatCardProps {
  title: string;
  value: string | number;
  icon: LucideIcon;
  trend?: { value: number; isUp: boolean };
  className?: string;
  color?: string;
}

export const StatCard: React.FC<StatCardProps> = ({ title, value, icon: Icon, trend, className, color = "text-primary" }) => (
  <div className={cn("bg-card p-6 rounded-xl border border-border shadow-sm flex items-start justify-between", className)}>
    <div>
      <p className="text-sm font-medium text-muted-foreground">{title}</p>
      <h3 className="text-2xl font-bold mt-1 tracking-tight">{value}</h3>
      {trend && (
        <p className={cn("text-xs mt-1 font-medium", trend.isUp ? "text-emerald-600" : "text-rose-600")}>
          {trend.isUp ? '↑' : '↓'} {trend.value}% <span className="text-muted-foreground ml-1">vs last week</span>
        </p>
      )}
    </div>
    <div className={cn("p-3 rounded-lg bg-muted/50", color)}>
      <Icon size={24} />
    </div>
  </div>
);

// 2. 页面标题 (PageHeader)
interface PageHeaderProps {
  title: string;
  subtitle?: string;
  breadcrumbs?: string[];
  actions?: React.ReactNode;
}

export const PageHeader: React.FC<PageHeaderProps> = ({ title, subtitle, breadcrumbs, actions }) => (
  <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-8">
    <div>
      {breadcrumbs && (
        <div className="flex items-center gap-2 text-xs text-muted-foreground mb-2">
          {breadcrumbs.map((b, i) => (
            <React.Fragment key={i}>
              <span>{b}</span>
              {i < breadcrumbs.length - 1 && <span>/</span>}
            </React.Fragment>
          ))}
        </div>
      )}
      <h1 className="text-2xl font-bold tracking-tight">{title}</h1>
      {subtitle && <p className="text-sm text-muted-foreground mt-1">{subtitle}</p>}
    </div>
    {actions && <div className="flex items-center gap-3">{actions}</div>}
  </div>
);
