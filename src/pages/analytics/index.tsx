import React, { useState } from 'react';
import { 
  BarChart3, 
  Download, 
  Calendar, 
  Filter, 
  Activity, 
  AlertTriangle, 
  Brain, 
  Clock, 
  Target,
  FileJson,
  FileSpreadsheet
} from 'lucide-react';
import { useAnalytics } from '@/hooks/useAnalytics';
import { StatCard, PageHeader } from '@/components/common';
import { ChartCard } from '@/components/common/ChartCard';

const Analytics: React.FC = () => {
  const { report, loading, error, getHeatmapOption } = useAnalytics();
  const [timeRange, setTimeRange] = useState('30d');

  if (error) return <div className="p-8 text-rose-500 font-bold">{error}</div>;

  // 1. 散点图配置：反应时 vs 准确率
  const scatterOption = report ? {
    tooltip: {
      formatter: (params: any) => `词汇: ${params.data[3]}<br/>RT: ${params.data[0]}ms<br/>正确率: ${params.data[1] * 100}%`
    },
    xAxis: { name: '反应时 (ms)', splitLine: { lineStyle: { type: 'dashed' } } },
    yAxis: { name: '正确率', min: 0, max: 1, splitLine: { lineStyle: { type: 'dashed' } } },
    series: [{
      type: 'scatter',
      data: report.scatterData.map(d => [d.rt, d.accuracy, d.frequency, d.word]),
      symbolSize: (data: any) => data[2] * 2,
      itemStyle: {
        color: (params: any) => {
          if (params.data[0] > 1200 && params.data[1] < 0.6) return '#ef4444'; // 高风险红色
          return '#3b82f6';
        },
        opacity: 0.6
      }
    }]
  } : {};

  // 2. 语境对比柱状图
  const contextOption = report ? {
    legend: { data: ['正确率', '反应时 (ms)'], bottom: 0 },
    xAxis: { type: 'category', data: ['低语境', '中语境', '高语境'] },
    yAxis: [{ type: 'value', max: 1 }, { type: 'value', name: 'ms', position: 'right' }],
    series: [
      { name: '正确率', type: 'bar', data: report.contextPerformances.map(p => p.accuracy), itemStyle: { color: '#6366f1' } },
      { name: '反应时 (ms)', type: 'line', yAxisIndex: 1, data: report.contextPerformances.map(p => p.avgRT), itemStyle: { color: '#f59e0b' } }
    ]
  } : {};

  // 3. 总体画像雷达图
  const radarOption = report ? {
    radar: {
      indicator: [
        { name: '英语水平', max: 100 },
        { name: '法语水平', max: 100 },
        { name: '语义辨析', max: 1 },
        { name: '语境利用', max: 1 },
        { name: '效率平衡', max: 1 }
      ],
      axisName: { color: '#64748b' }
    },
    series: [{
      type: 'radar',
      data: [{
        value: [
          report.metrics.enLevel,
          report.metrics.frLevel,
          report.metrics.semanticDiscrimination,
          report.metrics.contextUsage,
          report.metrics.speedAccuracyBalance
        ],
        areaStyle: { color: 'rgba(99, 102, 241, 0.2)' },
        lineStyle: { color: '#6366f1' }
      }]
    }]
  } : {};

  return (
    <div className="space-y-8 pb-12">
      <PageHeader 
        title="深度学情分析" 
        subtitle="基于迁移机制的行为数据解码与认知路径分析"
        breadcrumbs={['Dashboard', 'Analytics']}
        actions={
          <div className="flex gap-2">
            <button className="flex items-center gap-2 px-4 py-2 bg-muted hover:bg-muted/80 rounded-xl text-xs font-bold transition-all">
              <Download size={14} /> 导出 PDF
            </button>
            <button className="flex items-center gap-2 px-4 py-2 bg-primary text-white rounded-xl text-xs font-bold transition-all shadow-lg shadow-primary/20">
              <FileSpreadsheet size={14} /> 原始数据导出 (CSV)
            </button>
          </div>
        }
      />

      {/* 指标与概览 */}
      <div className="grid grid-cols-1 lg:grid-cols-4 gap-8">
        <div className="lg:col-span-1 space-y-6">
          <div className="bg-card border border-border p-6 rounded-3xl shadow-sm h-full">
            <h3 className="text-xs font-black uppercase tracking-widest text-muted-foreground mb-6">总体能力画像</h3>
            <div className="h-[250px]">
              <ChartCard title="" option={radarOption} height={250} className="border-none shadow-none" loading={loading} />
            </div>
            <div className="mt-6 p-4 bg-muted/50 rounded-2xl">
              <div className="flex justify-between items-center mb-2">
                <span className="text-xs font-medium text-muted-foreground">负迁移风险指数</span>
                <span className="text-xs font-black text-rose-500">{(report?.metrics.negativeTransferRisk || 0) * 100}%</span>
              </div>
              <div className="w-full h-1.5 bg-muted rounded-full overflow-hidden">
                <div className="h-full bg-rose-500" style={{ width: `${(report?.metrics.negativeTransferRisk || 0) * 100}%` }} />
              </div>
            </div>
          </div>
        </div>

        <div className="lg:col-span-3 grid grid-cols-1 md:grid-cols-3 gap-6">
          <StatCard title="正迁移稳定性" value="0.82" icon={Activity} trend={{ value: 5, isUp: true }} color="text-blue-500" />
          <StatCard title="语义加工时耗" value="480ms" icon={Clock} trend={{ value: 12, isUp: false }} color="text-amber-500" />
          <StatCard title="语境依赖增益" value="+24%" icon={Brain} color="text-emerald-500" />
        </div>
      </div>

      {/* 核心分析图表 */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        <ChartCard 
          title="迁移热力图：词对类型 × 错误模式" 
          option={getHeatmapOption(report?.heatmap || [])} 
          loading={loading}
          extra={<div className="text-[10px] text-muted-foreground">颜色越深代表认知干扰越强</div>}
        />
        <ChartCard 
          title="语境支持条件下的认知表现对比" 
          option={contextOption} 
          loading={loading}
        />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <ChartCard 
          title="加工路径分布 (反应时 vs 正确率)" 
          option={scatterOption} 
          className="lg:col-span-2"
          loading={loading}
        />
        <div className="bg-card border border-border rounded-3xl p-6 shadow-sm overflow-hidden flex flex-col">
          <div className="flex items-center justify-between mb-6">
            <h3 className="text-sm font-bold flex items-center gap-2">
              <AlertTriangle size={16} className="text-rose-500" /> 高风险负迁移词对
            </h3>
            <span className="text-[10px] font-bold text-muted-foreground uppercase">Top 4</span>
          </div>
          <div className="flex-1 space-y-4">
            {report?.topRiskPairs.map((pair, i) => (
              <div key={i} className="flex items-center justify-between p-3 rounded-2xl border border-border hover:bg-muted/30 transition-all">
                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 rounded-lg bg-rose-100 text-rose-600 flex items-center justify-center text-[10px] font-black">
                    {i+1}
                  </div>
                  <div>
                    <div className="text-xs font-bold">{pair.en} / {pair.fr}</div>
                    <div className="text-[10px] text-muted-foreground">错误出现 {pair.count} 次</div>
                  </div>
                </div>
                <div className="text-right">
                  <div className="text-xs font-black text-rose-500">{(pair.riskScore * 100).toFixed(0)}%</div>
                  <div className="text-[8px] text-muted-foreground uppercase font-medium">Risk</div>
                </div>
              </div>
            ))}
          </div>
          <button className="mt-6 w-full py-3 bg-muted rounded-2xl text-[10px] font-black uppercase tracking-widest text-muted-foreground hover:text-foreground transition-all">
            查看完整错题库
          </button>
        </div>
      </div>
    </div>
  );
};

export default Analytics;
