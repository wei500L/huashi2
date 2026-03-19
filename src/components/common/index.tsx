import React, { useEffect, useState } from 'react';
import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';
import { LucideIcon } from 'lucide-react';
import { motion, useMotionValue, useSpring, useTransform } from 'framer-motion';

function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

// Number Animation Component
export const AnimatedNumber: React.FC<{ value: number; format?: (v: number) => string }> = ({ value, format = (v) => Math.round(v).toString() }) => {
  const [displayValue, setDisplayValue] = useState(0);

  useEffect(() => {
    let startTimestamp: number;
    const duration = 1500;

    const step = (timestamp: number) => {
      if (!startTimestamp) startTimestamp = timestamp;
      const progress = Math.min((timestamp - startTimestamp) / duration, 1);
      const easeProgress = progress === 1 ? 1 : 1 - Math.pow(2, -10 * progress);
      setDisplayValue(easeProgress * value);
      if (progress < 1) {
        window.requestAnimationFrame(step);
      }
    };
    window.requestAnimationFrame(step);
  }, [value]);

  return <span>{format(displayValue)}</span>;
};

// 1. StatCard with Advanced Glass Effects, Framer Motion & 3D Tilt
interface StatCardProps {
  title: string;
  value: string | number;
  icon: LucideIcon;
  trend?: { value: number; isUp: boolean };
  className?: string;
  color?: string;
}

export const StatCard: React.FC<StatCardProps> = ({ title, value, icon: Icon, trend, className, color = "text-primary" }) => {
  const glowColorMap: Record<string, string> = {
    'text-blue-500': 'text-glow-blue',
    'text-blue-600': 'text-glow-blue',
    'text-rose-500': 'text-glow-rose',
    'text-rose-600': 'text-glow-rose',
    'text-emerald-500': 'text-glow-emerald',
    'text-emerald-600': 'text-glow-emerald',
    'text-amber-500': 'text-glow-amber',
    'text-amber-600': 'text-glow-amber',
    'text-primary': 'text-glow-primary',
  };
  
  const iconGlowClass = glowColorMap[color] || color;

  const numericValue = typeof value === 'string' ? parseFloat(value.replace(/[^0-9.-]+/g,"")) : value;
  const suffix = typeof value === 'string' ? value.replace(/[0-9.-]+/g,"") : "";

  const x = useMotionValue(0);
  const y = useMotionValue(0);
  const mouseXSpring = useSpring(x);
  const mouseYSpring = useSpring(y);
  const rotateX = useTransform(mouseYSpring, [-0.5, 0.5], ["7deg", "-7deg"]);
  const rotateY = useTransform(mouseXSpring, [-0.5, 0.5], ["-7deg", "7deg"]);

  const handleMouseMove = (e: React.MouseEvent<HTMLDivElement, MouseEvent>) => {
    const rect = e.currentTarget.getBoundingClientRect();
    const width = rect.width;
    const height = rect.height;
    const mouseX = e.clientX - rect.left;
    const mouseY = e.clientY - rect.top;
    const xPct = mouseX / width - 0.5;
    const yPct = mouseY / height - 0.5;
    x.set(xPct);
    y.set(yPct);
  };

  const handleMouseLeave = () => {
    x.set(0);
    y.set(0);
  };

  return (
    <motion.div 
      style={{ rotateX, rotateY, transformStyle: "preserve-3d" }}
      onMouseMove={handleMouseMove}
      onMouseLeave={handleMouseLeave}
      whileHover={{ scale: 1.02 }}
      transition={{ type: "spring", stiffness: 400, damping: 30 }}
      className={cn("liquid-glass p-7 rounded-[2.5rem] flex flex-col justify-between edge-light group fluid-texture transition-shadow duration-500 hover:shadow-[0_20px_50px_rgba(0,0,0,0.1)] dark:hover:shadow-[0_20px_50px_rgba(0,0,0,0.5)] cursor-default", className)}
    >
      <div className="flex items-start justify-between relative z-10" style={{ transform: "translateZ(30px)" }}>
        <div>
          <p className="text-[10px] font-black text-slate-400 dark:text-white/30 uppercase tracking-[0.2em] mb-1">{title}</p>
          <h3 className={cn("text-4xl font-black tracking-tighter drop-shadow-none dark:drop-shadow-[0_0_15px_rgba(255,255,255,0.1)]", iconGlowClass)}>
            {!isNaN(numericValue) ? (
              <>
                <AnimatedNumber value={numericValue} />
                <span className="text-xl opacity-80">{suffix}</span>
              </>
            ) : value}
          </h3>
        </div>
        <div className={cn("p-4 rounded-2xl bg-black/5 dark:bg-white/5 border border-slate-200 dark:border-white/10 backdrop-blur-xl relative group-hover:scale-110 transition-transform duration-500 shadow-sm dark:shadow-[inset_0_0_15px_rgba(255,255,255,0.05)]", color)}>
          <Icon size={28} className={cn("dark:drop-shadow-[0_0_12px_currentColor]")} />
        </div>
      </div>
      
      {trend && (
        <div className="mt-6 relative z-10" style={{ transform: "translateZ(20px)" }}>
          <div className={cn(
            "inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[10px] font-black border backdrop-blur-md transition-all duration-500",
            trend.isUp 
              ? "bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/20" 
              : "bg-rose-500/10 text-rose-600 dark:text-rose-400 border-rose-500/20"
          )}>
            <span className="text-xs">{trend.isUp ? '↑' : '↓'}</span>
            <span><AnimatedNumber value={trend.value} />%</span>
            <span className="text-slate-400 dark:text-white/20 font-medium ml-1">PAST WEEK</span>
          </div>
        </div>
      )}
    </motion.div>
  );
};

// 2. PageHeader with Refined Typography
interface PageHeaderProps {
  title: string;
  subtitle?: string;
  breadcrumbs?: string[];
  actions?: React.ReactNode;
}

export const PageHeader: React.FC<PageHeaderProps> = ({ title, subtitle, breadcrumbs, actions }) => (
  <div className="flex flex-col md:flex-row md:items-center justify-between gap-6 mb-10">
    <motion.div
      initial={{ x: -20, opacity: 0 }}
      animate={{ x: 0, opacity: 1 }}
      transition={{ duration: 0.6, ease: "easeOut" }}
    >
      {breadcrumbs && (
        <div className="flex items-center gap-3 text-[10px] font-bold text-slate-400 dark:text-white/30 uppercase tracking-[0.2em] mb-3">
          {breadcrumbs.map((b, i) => (
            <React.Fragment key={i}>
              <span className="hover:text-primary transition-colors cursor-pointer">{b}</span>
              {i < breadcrumbs.length - 1 && <span className="text-slate-200 dark:text-white/10">/</span>}
            </React.Fragment>
          ))}
        </div>
      )}
      <h1 className="text-4xl font-black tracking-tight text-slate-900 dark:text-white drop-shadow-none dark:drop-shadow-[0_0_20px_rgba(255,255,255,0.15)]">{title}</h1>
      {subtitle && <p className="text-sm text-slate-500 dark:text-white/40 mt-3 font-medium max-w-xl leading-relaxed">{subtitle}</p>}
    </motion.div>
    {actions && (
      <motion.div 
        initial={{ x: 20, opacity: 0 }}
        animate={{ x: 0, opacity: 1 }}
        transition={{ duration: 0.6, ease: "easeOut" }}
        className="flex items-center gap-4"
      >
        {actions}
      </motion.div>
    )}
  </div>
);
