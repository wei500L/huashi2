import React from 'react';
import ReactECharts from 'echarts-for-react';
import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';
import { RefreshCcw } from 'lucide-react';

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
  return (
    <div className={cn("liquid-glass-panel edge-light fluid-texture rounded-3xl overflow-hidden group transition-all duration-500 hover:shadow-[0_15px_50px_rgba(0,0,0,0.6)]", className)}>
      <div className="px-8 py-5 border-b border-white/5 flex items-center justify-between bg-white/5 backdrop-blur-md relative z-10">
        <h3 className="text-sm font-bold tracking-widest uppercase text-white/90 drop-shadow-[0_0_8px_rgba(255,255,255,0.3)]">{title}</h3>
        <div className="flex items-center gap-4">
          {extra}
          {loading && <RefreshCcw className="animate-spin text-primary drop-shadow-[0_0_8px_rgba(139,92,246,0.8)]" size={16} />}
        </div>
      </div>
      
      <div className="p-8 relative z-10" style={{ height }}>
        {loading ? (
          <div className="absolute inset-0 flex items-center justify-center bg-black/20 backdrop-blur-sm z-20">
            <div className="w-full h-full animate-pulse bg-white/5 rounded-2xl flex items-center justify-center border border-white/10 shadow-[inset_0_0_20px_rgba(255,255,255,0.05)]">
              <span className="text-sm font-medium tracking-widest text-primary animate-pulse drop-shadow-[0_0_10px_rgba(139,92,246,0.8)]">SYNCHRONIZING DATA...</span>
            </div>
          </div>
        ) : isEmpty ? (
          <div className="flex flex-col items-center justify-center h-full text-white/40">
            <p className="text-sm uppercase tracking-widest">No diagnostic data available</p>
          </div>
        ) : (
          <ReactECharts 
            option={option} 
            style={{ height: '100%', width: '100%' }} 
            notMerge={true}
            lazyUpdate={true}
            theme="dark" // 强制为暗色以适应全局背景
          />
        )}
      </div>
    </div>
  );
};
