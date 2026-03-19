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

export const StatCard: React.FC<StatCardProps> = ({ title, value, icon: Icon, trend, className, color = "text-primary" }) => {
  // Map standard tailwind text colors to glow text classes
  const glowColorMap: Record<string, string> = {
    'text-blue-500': 'text-glow-blue',
    'text-rose-500': 'text-glow-rose',
    'text-emerald-500': 'text-glow-emerald',
    'text-amber-500': 'text-glow-amber',
    'text-primary': 'text-glow-primary',
  };
  
  const iconGlowClass = glowColorMap[color] || color;

  return (
    <div className={cn("liquid-glass p-6 rounded-3xl flex items-start justify-between edge-light group fluid-texture transition-all duration-500 hover:-translate-y-1", className)}>
      <div className="relative z-10">
        <p className="text-sm font-medium text-muted-foreground uppercase tracking-widest">{title}</p>
        <h3 className={cn("text-3xl font-black mt-2 tracking-tight drop-shadow-[0_0_10px_rgba(255,255,255,0.2)]", iconGlowClass)}>{value}</h3>
        {trend && (
          <p className={cn("text-xs mt-3 font-bold px-2 py-1 rounded-full inline-flex items-center gap-1 backdrop-blur-sm", trend.isUp ? "bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 shadow-[0_0_10px_rgba(16,185,129,0.2)]" : "bg-rose-500/10 text-rose-400 border border-rose-500/20 shadow-[0_0_10px_rgba(244,63,94,0.2)]")}>
            {trend.isUp ? '↑' : '↓'} {trend.value}% <span className="text-muted-foreground ml-1 font-normal opacity-70">vs last week</span>
          </p>
        )}
      </div>
      <div className={cn("p-4 rounded-2xl bg-white/5 border border-white/10 backdrop-blur-md relative z-10 group-hover:scale-110 transition-transform duration-500 shadow-[inset_0_0_20px_rgba(255,255,255,0.05)]", color)}>
        <Icon size={28} className={cn("drop-shadow-[0_0_10px_currentColor]")} />
      </div>
    </div>
  );
};

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
              <span className="hover:text-primary transition-colors cursor-pointer">{b}</span>
              {i < breadcrumbs.length - 1 && <span className="opacity-50">/</span>}
            </React.Fragment>
          ))}
        </div>
      )}
      <h1 className="text-3xl font-black tracking-tight bg-gradient-to-r from-white to-white/70 bg-clip-text text-transparent drop-shadow-[0_0_15px_rgba(255,255,255,0.1)]">{title}</h1>
      {subtitle && <p className="text-sm text-muted-foreground mt-2">{subtitle}</p>}
    </div>
    {actions && <div className="flex items-center gap-3">{actions}</div>}
  </div>
);
