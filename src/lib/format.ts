import type { AnalyticsHeatmapVO, AnalyticsRadarMetricVO, AnalyticsScatterVO, AnalyticsTrendVO, Role } from './contracts';

export function formatPercent(value: number, digits = 0): string {
  return `${(value * 100).toFixed(digits)}%`;
}

export function formatMaybePercent(value?: number | null, digits = 0): string {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return '--';
  }
  return formatPercent(value, digits);
}

export function formatMs(value?: number | null): string {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return '--';
  }
  return `${Math.round(value)}ms`;
}

export function formatDateTime(value?: string | null): string {
  if (!value) {
    return '--';
  }
  return new Date(value).toLocaleString('zh-CN', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export function formatDate(value?: string | null): string {
  if (!value) {
    return '--';
  }
  return new Date(value).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
}

export function roleHomePath(role?: Role | null): string {
  if (role === 'TEACHER') {
    return '/teacher/classes';
  }
  if (role === 'ADMIN') {
    return '/admin/users';
  }
  return '/dashboard';
}

export function lexicalPairTypeLabel(type?: string | null): string {
  switch (type) {
    case 'COGNATE':
      return '同源词';
    case 'FALSE_FRIEND':
      return '同形异义';
    case 'PARTIAL_COGNATE':
      return '部分同源';
    case 'ORTHOGRAPHIC_SIMILAR':
      return '近形词';
    default:
      return type || '--';
  }
}

export function contextLevelLabel(level?: string | null): string {
  switch (level) {
    case 'LOW':
      return '低语境';
    case 'MEDIUM':
      return '中语境';
    case 'HIGH':
      return '高语境';
    default:
      return level || '--';
  }
}

export function riskTone(level?: string | null): string {
  switch (level) {
    case 'HIGH':
    case 'URGENT':
      return 'text-rose-500';
    case 'MEDIUM':
    case 'NORMAL':
      return 'text-amber-500';
    default:
      return 'text-emerald-500';
  }
}

export function buildTrendOption(trend?: AnalyticsTrendVO | null) {
  if (!trend) {
    return {};
  }
  return {
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis' },
    legend: { textStyle: { color: '#94a3b8' }, bottom: 0 },
    grid: { left: '4%', right: '4%', top: '8%', bottom: '18%', containLabel: true },
    xAxis: {
      type: 'category',
      data: trend.xAxis,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#94a3b8' },
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: 'rgba(148,163,184,0.12)' } },
      axisLabel: { color: '#94a3b8' },
    },
    series: trend.series.map((series, index) => ({
      name: series.label,
      type: index === 0 ? 'line' : 'bar',
      smooth: true,
      symbol: 'none',
      data: series.values,
    })),
  };
}

export function buildRadarOption(radar?: AnalyticsRadarMetricVO[] | null) {
  if (!radar?.length) {
    return {};
  }
  return {
    backgroundColor: 'transparent',
    radar: {
      indicator: radar.map((item) => ({ name: item.label, max: item.max })),
      axisName: { color: '#94a3b8' },
      splitArea: { areaStyle: { color: ['rgba(255,255,255,0.03)', 'rgba(255,255,255,0.06)'] } },
      splitLine: { lineStyle: { color: 'rgba(148,163,184,0.15)' } },
    },
    series: [
      {
        type: 'radar',
        data: [
          {
            value: radar.map((item) => item.value),
            areaStyle: { color: 'rgba(59,130,246,0.18)' },
            lineStyle: { color: '#3b82f6', width: 2 },
            symbol: 'none',
          },
        ],
      },
    ],
  };
}

export function buildHeatmapOption(heatmap?: AnalyticsHeatmapVO | null) {
  if (!heatmap) {
    return {};
  }
  return {
    backgroundColor: 'transparent',
    tooltip: {
      formatter: (params: { data: [number, number, number, number, number] }) => {
        const [xIndex, yIndex, value, accuracy, avgRt] = params.data;
        return `${heatmap.xAxis[xIndex]} / ${heatmap.yAxis[yIndex]}<br/>样本数: ${value}<br/>正确率: ${formatPercent(accuracy, 0)}<br/>平均反应时: ${formatMs(avgRt)}`;
      },
    },
    grid: { left: '6%', right: '6%', top: '8%', bottom: '12%', containLabel: true },
    xAxis: { type: 'category', data: heatmap.xAxis, axisLabel: { color: '#94a3b8' } },
    yAxis: { type: 'category', data: heatmap.yAxis, axisLabel: { color: '#94a3b8' } },
    visualMap: {
      min: 0,
      max: Math.max(1, ...heatmap.cells.map((item) => item.value)),
      calculable: true,
      orient: 'horizontal',
      left: 'center',
      bottom: 0,
      textStyle: { color: '#94a3b8' },
    },
    series: [
      {
        type: 'heatmap',
        data: heatmap.cells.map((item) => [
          heatmap.xAxis.indexOf(item.xKey),
          heatmap.yAxis.indexOf(item.yKey),
          item.value,
          item.accuracy,
          item.avgReactionTimeMs,
        ]),
        label: { show: true, color: '#fff' },
      },
    ],
  };
}

export function buildScatterOption(scatter?: AnalyticsScatterVO | null) {
  if (!scatter) {
    return {};
  }
  return {
    backgroundColor: 'transparent',
    tooltip: {
      formatter: (params: { data: [number, number, number, number, string] }) => {
        const [x, y, attempts, risk, label] = params.data;
        return `${label}<br/>${scatter.x}: ${formatMs(x)}<br/>${scatter.y}: ${formatPercent(y, 0)}<br/>尝试次数: ${attempts}<br/>风险: ${formatPercent(risk, 0)}`;
      },
    },
    xAxis: { name: scatter.x, axisLabel: { color: '#94a3b8' } },
    yAxis: { name: scatter.y, axisLabel: { color: '#94a3b8' }, max: 1 },
    series: [
      {
        type: 'scatter',
        data: scatter.points.map((point) => [
          point.avgReactionTimeMs,
          point.accuracy,
          point.attemptCount,
          point.riskScore,
          point.label,
        ]),
        symbolSize: (data: [number, number, number]) => Math.max(10, Math.min(30, data[2] * 2)),
      },
    ],
  };
}
