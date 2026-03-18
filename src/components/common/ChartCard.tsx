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
    <div className={cn("bg-card border border-border rounded-xl shadow-sm overflow-hidden", className)}>
      <div className="px-6 py-4 border-b border-border flex items-center justify-between bg-muted/20">
        <h3 className="text-sm font-semibold tracking-tight">{title}</h3>
        <div className="flex items-center gap-4">
          {extra}
          {loading && <RefreshCcw className="animate-spin text-muted-foreground" size={14} />}
        </div>
      </div>
      
      <div className="p-6 relative" style={{ height }}>
        {loading ? (
          <div className="absolute inset-0 flex items-center justify-center bg-card/50 backdrop-blur-sm z-10">
            <div className="w-full h-full animate-pulse bg-muted/30 rounded-lg flex items-center justify-center">
              <span className="text-xs text-muted-foreground">分析数据中...</span>
            </div>
          </div>
        ) : isEmpty ? (
          <div className="flex flex-col items-center justify-center h-full text-muted-foreground">
            <p className="text-sm">暂无诊断数据，请先参与实验</p>
          </div>
        ) : (
          <ReactECharts 
            option={option} 
            style={{ height: '100%', width: '100%' }} 
            notMerge={true}
            lazyUpdate={true}
            theme={undefined} // 可根据明暗模式动态切换主题
          />
        )}
      </div>
    </div>
  );
};
