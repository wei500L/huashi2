import React from 'react';
import { chartThemeName, echarts, type AppChartOption } from '@/lib/echarts';
import { cn } from '@/lib/utils';
import { useUIStore } from '@/store';

type EChartProps = {
  option: AppChartOption;
  className?: string;
  style?: React.CSSProperties;
  theme?: 'light' | 'dark';
  notMerge?: boolean;
  lazyUpdate?: boolean;
};

export const EChart: React.FC<EChartProps> = ({
  option,
  className,
  style,
  theme,
  notMerge = true,
  lazyUpdate = true,
}) => {
  const { isDarkMode } = useUIStore();
  const containerRef = React.useRef<HTMLDivElement | null>(null);
  const chartRef = React.useRef<ReturnType<typeof echarts.init> | null>(null);
  const optionRef = React.useRef(option);
  const notMergeRef = React.useRef(notMerge);
  const lazyUpdateRef = React.useRef(lazyUpdate);
  const resolvedTheme = chartThemeName(theme ? theme === 'dark' : isDarkMode);

  React.useEffect(() => {
    optionRef.current = option;
    notMergeRef.current = notMerge;
    lazyUpdateRef.current = lazyUpdate;
  }, [lazyUpdate, notMerge, option]);

  React.useEffect(() => {
    const element = containerRef.current;
    if (!element) {
      return;
    }

    const chart = echarts.init(element, resolvedTheme);
    chartRef.current = chart;
    chart.setOption(optionRef.current, {
      notMerge: notMergeRef.current,
      lazyUpdate: lazyUpdateRef.current,
    });

    const resizeObserver = new ResizeObserver(() => {
      chart.resize();
    });
    resizeObserver.observe(element);

    return () => {
      resizeObserver.disconnect();
      chart.dispose();
      chartRef.current = null;
    };
  }, [resolvedTheme]);

  React.useEffect(() => {
    chartRef.current?.setOption(option, { notMerge, lazyUpdate });
  }, [lazyUpdate, notMerge, option]);

  return <div ref={containerRef} className={cn('h-full w-full', className)} style={style} />;
};
