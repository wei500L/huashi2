import * as echarts from 'echarts/core';
import {
  BarChart,
  HeatmapChart,
  LineChart,
  PieChart,
  RadarChart,
  ScatterChart,
  type BarSeriesOption,
  type HeatmapSeriesOption,
  type LineSeriesOption,
  type PieSeriesOption,
  type RadarSeriesOption,
  type ScatterSeriesOption,
} from 'echarts/charts';
import {
  GridComponent,
  LegendComponent,
  RadarComponent,
  TooltipComponent,
  VisualMapComponent,
  type GridComponentOption,
  type LegendComponentOption,
  type RadarComponentOption,
  type TooltipComponentOption,
  type VisualMapComponentOption,
} from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import type { ComposeOption, EChartsType } from 'echarts/core';

echarts.use([
  BarChart,
  HeatmapChart,
  LineChart,
  PieChart,
  RadarChart,
  ScatterChart,
  GridComponent,
  LegendComponent,
  RadarComponent,
  TooltipComponent,
  VisualMapComponent,
  CanvasRenderer,
]);

const LIGHT_THEME = 'ef-transfer-light';
const DARK_THEME = 'ef-transfer-dark';

echarts.registerTheme(LIGHT_THEME, {
  backgroundColor: 'transparent',
  textStyle: {
    color: '#0f172a',
  },
});

echarts.registerTheme(DARK_THEME, {
  darkMode: true,
  color: ['#4992ff', '#7cffb2', '#fddd60', '#ff6e76', '#58d9f9', '#05c091', '#ff8a45'],
  backgroundColor: 'transparent',
  textStyle: {
    color: '#cbd5f5',
  },
  legend: {
    textStyle: {
      color: '#cbd5f5',
    },
  },
  tooltip: {
    backgroundColor: 'rgba(15,23,42,0.96)',
    borderColor: 'rgba(148,163,184,0.25)',
    textStyle: {
      color: '#e2e8f0',
    },
  },
  categoryAxis: {
    axisLine: {
      lineStyle: {
        color: '#64748b',
      },
    },
    axisLabel: {
      color: '#94a3b8',
    },
    splitLine: {
      lineStyle: {
        color: 'rgba(148,163,184,0.14)',
      },
    },
  },
  valueAxis: {
    axisLine: {
      lineStyle: {
        color: '#64748b',
      },
    },
    axisLabel: {
      color: '#94a3b8',
    },
    splitLine: {
      lineStyle: {
        color: 'rgba(148,163,184,0.14)',
      },
    },
  },
  radar: {
    axisName: {
      color: '#cbd5f5',
    },
    splitLine: {
      lineStyle: {
        color: 'rgba(148,163,184,0.16)',
      },
    },
    splitArea: {
      areaStyle: {
        color: ['rgba(255,255,255,0.02)', 'rgba(255,255,255,0.05)'],
      },
    },
  },
  visualMap: {
    textStyle: {
      color: '#94a3b8',
    },
  },
});

export type AppChartOption = ComposeOption<
  | BarSeriesOption
  | HeatmapSeriesOption
  | LineSeriesOption
  | PieSeriesOption
  | RadarSeriesOption
  | ScatterSeriesOption
  | GridComponentOption
  | LegendComponentOption
  | RadarComponentOption
  | TooltipComponentOption
  | VisualMapComponentOption
>;

export type AppChartInstance = EChartsType;

export function chartThemeName(isDarkMode: boolean): string {
  return isDarkMode ? DARK_THEME : LIGHT_THEME;
}

export { echarts };
