import React from 'react';
import ReactECharts from 'echarts-for-react';
import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';
import { RefreshCcw } from 'lucide-react';
import { useUIStore } from '@/store';
import { motion } from 'framer-motion';

function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

interface ChartCardProps {
  title: string;
  option: any;
  loading?: boolean;
  isEmpty?: boolean;
  height?: string | number;
  className?: string;
  extra?: React.ReactNode;
}

export const ChartCard: React.FC<ChartCardProps> = ({ 
  title, 
  option, 
  loading = false, 
  isEmpty = false, 
  height = 350, 
  className,
  extra 
}) => {
  const { isDarkMode } = useUIStore();

  return (
    <div className={cn(
      "liquid-glass-panel border-beam fluid-texture rounded-[2.5rem] overflow-hidden group transition-all duration-700 hover:shadow-[0_20px_60px_rgba(0,0,0,0.3)] dark:hover:shadow-[0_30px_80px_rgba(0,0,0,0.6)]", 
      className
    )}>
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
          <div className="absolute inset-0 flex flex-col items-center justify-center bg-black/[0.02] dark:bg-black/20 backdrop-blur-md z-20">
            <div className="w-full h-full p-8 space-y-6">
              <div className="h-4 w-1/3 bg-slate-200 dark:bg-white/5 rounded-full animate-pulse" />
              <div className="h-full w-full bg-slate-100 dark:bg-white/[0.02] rounded-3xl animate-pulse relative overflow-hidden">
                <div className="absolute inset-0 bg-gradient-to-r from-transparent via-primary/5 to-transparent -translate-x-full animate-[shimmer_2s_infinite]" />
              </div>
            </div>
          </div>
        ) : isEmpty ? (
          <div className="flex flex-col items-center justify-center h-full text-foreground/20">
            <p className="text-[10px] uppercase font-black tracking-[0.4em]">No spectral data detected</p>
          </div>
        ) : (
          <motion.div 
            initial={{ opacity: 0, scale: 0.98 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ duration: 1, ease: "easeOut" }}
            style={{ height: '100%', width: '100%' }}
          >
            <ReactECharts 
              option={option} 
              style={{ height: '100%', width: '100%' }} 
              notMerge={true}
              lazyUpdate={true}
              theme={isDarkMode ? 'dark' : 'light'} 
            />
          </motion.div>
        )}
      </div>
    </div>
  );
};
