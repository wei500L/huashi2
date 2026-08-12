import type { AppChartOption } from '@/lib/echarts';
import type { ResearchDimensionStatisticVO, ResearchQuestionStatisticVO } from '@/lib/contracts';

export function dimensionChartOption(rows: ResearchDimensionStatisticVO[]): AppChartOption {
  return {
    tooltip: {
      trigger: 'axis',
      formatter: (params: unknown) => {
        const item = Array.isArray(params) ? params[0] as { name: string; data: number; dataIndex: number } : null;
        if (!item) return '';
        const row = rows[item.dataIndex];
        return `${item.name}<br/>正确率 ${item.data}%<br/>${row?.correctCount ?? 0} / ${row?.answeredCount ?? 0}`;
      },
    },
    grid: { left: 16, right: 16, top: 24, bottom: 32, containLabel: true },
    xAxis: { type: 'category', data: rows.map((row) => row.dimension), axisLabel: { interval: 0, rotate: rows.length > 5 ? 20 : 0 } },
    yAxis: { type: 'value', min: 0, max: 100, axisLabel: { formatter: '{value}%' } },
    series: [{
      type: 'bar',
      data: rows.map((row) => row.correctRate == null ? 0 : Math.round(row.correctRate * 1000) / 10),
      itemStyle: { color: '#2563eb', borderRadius: [8, 8, 0, 0] },
    }],
  };
}

export function difficultyChartOption(rows: ResearchQuestionStatisticVO[]): AppChartOption {
  const visible = rows.filter((row) => row.correctRate != null).slice(0, 12);
  return {
    tooltip: {
      trigger: 'axis',
      formatter: (params: unknown) => {
        const item = Array.isArray(params) ? params[0] as { name: string; data: number; dataIndex: number } : null;
        if (!item) return '';
        const row = visible[item.dataIndex];
        return `第 ${row?.questionOrder} 题<br/>正确率 ${item.data}%<br/>作答 ${row?.answeredCount ?? 0}，跳过 ${row?.skippedCount ?? 0}`;
      },
    },
    grid: { left: 16, right: 16, top: 24, bottom: 48, containLabel: true },
    xAxis: { type: 'category', data: visible.map((row) => `Q${row.questionOrder}`), axisLabel: { interval: 0 } },
    yAxis: { type: 'value', min: 0, max: 100, axisLabel: { formatter: '{value}%' } },
    series: [{
      type: 'bar',
      data: visible.map((row) => Math.round((row.correctRate ?? 0) * 1000) / 10),
      itemStyle: { color: '#059669', borderRadius: [8, 8, 0, 0] },
    }],
  };
}
