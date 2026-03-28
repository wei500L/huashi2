import type { AnalyticsHeatmapVO, AnalyticsRadarMetricVO, AnalyticsScatterVO, AnalyticsTrendVO, Capability, CurrentUserVO } from './contracts';
import type { AppChartOption } from './echarts';

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

export function hasCapability(capabilities: Capability[] | null | undefined, capability: Capability): boolean {
  return Array.isArray(capabilities) && capabilities.includes(capability);
}

export function userHasCapability(user: Pick<CurrentUserVO, 'capabilities'> | null | undefined, capability: Capability): boolean {
  return hasCapability(user?.capabilities, capability);
}

export function homePathForCapabilities(capabilities?: Capability[] | null): string {
  if (hasCapability(capabilities, 'ADMIN_CONSOLE')) {
    return '/admin/users';
  }
  if (hasCapability(capabilities, 'TEACHING_WORKSPACE')) {
    return '/teacher/workspace';
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

export function roleLabel(role?: string | null): string {
  if (role === 'TEACHER') {
    return 'Teacher';
  }
  if (role === 'ADMIN') {
    return 'Administrator';
  }
  return 'Student';
}

export function workspaceLabels(capabilities?: Capability[] | null): string[] {
  const labels: string[] = [];
  if (hasCapability(capabilities, 'ADMIN_CONSOLE')) {
    labels.push('Admin');
  }
  if (hasCapability(capabilities, 'TEACHING_WORKSPACE')) {
    labels.push('Teaching');
  }
  if (hasCapability(capabilities, 'STUDENT_WORKSPACE')) {
    labels.push('Student');
  }
  return labels;
}

export function buildTrendOption(trend?: AnalyticsTrendVO | null): AppChartOption {
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
    series: trend.series.map((series, index) => {
      if (index === 0) {
        return {
          name: series.label,
          type: 'line',
          smooth: true,
          symbol: 'none',
          data: series.values,
        };
      }
      return {
        name: series.label,
        type: 'bar',
        data: series.values,
      };
    }),
  };
}

export function buildRadarOption(radar?: AnalyticsRadarMetricVO[] | null): AppChartOption {
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

export function buildHeatmapOption(heatmap?: AnalyticsHeatmapVO | null): AppChartOption {
  if (!heatmap) {
    return {};
  }
  return {
    backgroundColor: 'transparent',
    tooltip: {
      formatter: (params) => {
        const dataCarrier = (Array.isArray(params) ? params[0] : params) as { data?: unknown };
        const data = Array.isArray(dataCarrier.data)
          ? (dataCarrier.data as [number, number, number, number, number])
          : [0, 0, 0, 0, 0];
        const [xIndex, yIndex, value, accuracy, avgRt] = data;
        return `${heatmap.xAxis[xIndex]} / ${heatmap.yAxis[yIndex]}<br/>样本数: ${value}<br/>正确率: ${formatPercent(Number(accuracy), 0)}<br/>平均反应时: ${formatMs(Number(avgRt))}`;
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

export function buildScatterOption(scatter?: AnalyticsScatterVO | null): AppChartOption {
  if (!scatter) {
    return {};
  }
  return {
    backgroundColor: 'transparent',
    tooltip: {
      formatter: (params) => {
        const dataCarrier = (Array.isArray(params) ? params[0] : params) as { data?: unknown };
        const data = Array.isArray(dataCarrier.data)
          ? (dataCarrier.data as [number, number, number, number, string])
          : [0, 0, 0, 0, ''];
        const [x, y, attempts, risk, label] = data;
        return `${label}<br/>${scatter.x}: ${formatMs(Number(x))}<br/>${scatter.y}: ${formatPercent(Number(y), 0)}<br/>尝试次数: ${Number(attempts)}<br/>风险: ${formatPercent(Number(risk), 0)}`;
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
