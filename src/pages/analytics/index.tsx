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
import { motion } from 'framer-motion';

const Analytics: React.FC = () => {
  const { report, loading, error, getHeatmapOption } = useAnalytics();
  const [timeRange, setTimeRange] = useState('30d');

  if (error) return (
    <div className="flex flex-col items-center justify-center min-h-[60vh] text-rose-500 liquid-glass p-12 rounded-[3rem] max-w-lg mx-auto mt-20 shadow-[0_0_100px_rgba(244,63,94,0.15)] edge-light">
      <AlertTriangle size={80} className="drop-shadow-[0_0_25px_rgba(244,63,94,0.8)] animate-pulse mb-8" />
      <h2 className="font-black text-2xl tracking-tighter uppercase mb-2 text-slate-900 dark:text-rose-500">Analytics Offline</h2>
      <p className="text-center text-slate-500 dark:text-rose-400/60 font-medium mb-10 max-w-xs">{error}</p>
      <button className="btn-liquid text-white px-10 py-4" onClick={() => window.location.reload()}>RE-SYNC ENGINE</button>
    </div>
  );

  // 1. Scatter Option
  const scatterOption = report ? {
    backgroundColor: 'transparent',
    tooltip: {
      backgroundColor: 'rgba(10, 5, 30, 0.9)',
      borderColor: 'rgba(139, 92, 246, 0.4)',
      textStyle: { color: '#fff', fontFamily: 'Inter' },
      formatter: (params: any) => `词汇: ${params.data[3]}<br/>RT: ${params.data[0]}ms<br/>正确率: ${params.data[1] * 100}%`
    },
    xAxis: { name: '反应时', splitLine: { lineStyle: { color: 'rgba(255,255,255,0.05)' } }, axisLabel: { color: '#94a3b8' } },
    yAxis: { name: '正确率', min: 0, max: 1, splitLine: { lineStyle: { color: 'rgba(255,255,255,0.05)' } }, axisLabel: { color: '#94a3b8' } },
    series: [{
      type: 'scatter',
      data: report.scatterData.map(d => [d.rt, d.accuracy, d.frequency, d.word]),
      symbolSize: (data: any) => data[2] * 3,
      itemStyle: {
        color: (params: any) => params.data[0] > 1200 && params.data[1] < 0.6 ? '#f43f5e' : '#8b5cf6',
        opacity: 0.8,
        shadowBlur: 10,
        shadowColor: 'rgba(0,0,0,0.3)'
      }
    }]
  } : {};

  // 2. Context Option
  const contextOption = report ? {
    backgroundColor: 'transparent',
    legend: { textStyle: { color: '#94a3b8' }, bottom: 0 },
    xAxis: { type: 'category', data: ['低语境', '中语境', '高语境'], axisLabel: { color: '#94a3b8' } },
    yAxis: [{ type: 'value', max: 1, axisLabel: { color: '#94a3b8' }, splitLine: { show: false } }, { type: 'value', name: 'ms', position: 'right', axisLabel: { color: '#94a3b8' }, splitLine: { show: false } }],
    series: [
      { name: '正确率', type: 'bar', data: report.contextPerformances.map(p => p.accuracy), itemStyle: { borderRadius: [4, 4, 0, 0], color: '#8b5cf6' } },
      { name: '反应时 (ms)', type: 'line', smooth: true, yAxisIndex: 1, data: report.contextPerformances.map(p => p.avgRT), itemStyle: { color: '#f59e0b' }, lineStyle: { width: 3 } }
    ]
  } : {};

  // 3. Radar Option
  const radarOption = report ? {
    backgroundColor: 'transparent',
    radar: {
      indicator: [
        { name: '英语水平', max: 100 },
        { name: '法语水平', max: 100 },
        { name: '语义辨析', max: 1 },
        { name: '语境利用', max: 1 },
        { name: '效率平衡', max: 1 }
      ],
      axisName: { color: '#94a3b8', fontWeight: 'bold' },
      splitArea: { show: false },
      splitLine: { lineStyle: { color: 'rgba(255,255,255,0.05)' } }
    },
    series: [{
      type: 'radar',
      data: [{
        value: [
          report.metrics?.enLevel || 0,
          report.metrics?.frLevel || 0,
          report.metrics?.semanticDiscrimination || 0,
          report.metrics?.contextUsage || 0,
          report.metrics?.speedAccuracyBalance || 0
        ],
        areaStyle: { color: 'rgba(139, 92, 246, 0.3)' },
        lineStyle: { color: '#8b5cf6', width: 2 },
        symbol: 'none'
      }]
    }]
  } : {};

  if (loading) return (
    <div className="flex items-center justify-center min-h-[60vh]">
      <div className="text-primary font-black animate-pulse tracking-[0.5em] uppercase text-xl">Initializing Analytics Engine...</div>
    </div>
  );

  return (
    <div className="space-y-10 pb-12">
      <PageHeader 
        title="深度学情分析" 
        subtitle="基于迁移机制的行为数据解码与认知路径分析"
        breadcrumbs={['Dashboard', 'Analytics']}
        actions={
          <div className="flex gap-3">
            <button className="flex items-center gap-2 px-5 py-2.5 bg-white/50 dark:bg-white/5 hover:bg-white dark:hover:bg-white/10 rounded-2xl text-xs font-black transition-all border border-slate-200 dark:border-white/10 text-slate-600 dark:text-white/70">
              <Download size={14} /> EXPORT PDF
            </button>
            <button className="btn-liquid flex items-center gap-2 px-5 py-2.5">
              <FileSpreadsheet size={14} /> EXPORT CSV
            </button>
          </div>
        }
      />

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-10">
        <div className="lg:col-span-1 space-y-8">
          <div className="liquid-glass-panel border-beam p-8 rounded-[2.5rem] flex flex-col h-full">
            <h3 className="text-[10px] font-black uppercase tracking-[0.3em] text-slate-400 dark:text-white/30 mb-8">Overall Competence</h3>
            <div className="flex-1 flex items-center justify-center min-h-[250px]">
              <ChartCard title="" option={radarOption} height={250} className="border-none shadow-none bg-transparent" loading={loading} />
            </div>
            <div className="mt-8 p-6 bg-slate-50 dark:bg-white/5 rounded-3xl border border-slate-100 dark:border-white/5">
              <div className="flex justify-between items-center mb-3">
                <span className="text-[10px] font-black uppercase tracking-widest text-slate-400 dark:text-white/30">Negative Risk Index</span>
                <span className="text-xs font-black text-rose-500">{(report?.metrics?.negativeTransferRisk || 0) * 100}%</span>
              </div>
              <div className="w-full h-2 bg-slate-200 dark:bg-black/40 rounded-full overflow-hidden">
                <div className="h-full bg-gradient-to-r from-rose-600 to-rose-400" style={{ width: `${(report?.metrics?.negativeTransferRisk || 0) * 100}%` }} />
              </div>
            </div>
          </div>
        </div>

        <div className="lg:col-span-3 grid grid-cols-1 md:grid-cols-3 gap-8">
          <StatCard title="Stability Score" value="0.82" icon={Activity} trend={{ value: 5, isUp: true }} color="text-blue-600 dark:text-blue-500" />
          <StatCard title="Processing Latency" value="480ms" icon={Clock} trend={{ value: 12, isUp: false }} color="text-amber-600 dark:text-amber-500" />
          <StatCard title="Contextual Gain" value="+24%" icon={Brain} color="text-emerald-600 dark:text-emerald-500" />
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-10">
        <ChartCard 
          title="Cognitive Heatmap: Interference Patterns" 
          option={getHeatmapOption(report?.heatmap || [])} 
          loading={loading}
          extra={<div className="text-[10px] font-bold text-slate-400 dark:text-white/30 tracking-wider">SPECTRAL DENSITY ANALYSIS</div>}
        />
        <ChartCard 
          title="Condition-Based Performance Comparison" 
          option={contextOption} 
          loading={loading}
        />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-10">
        <ChartCard 
          title="Processing Pathways (Latency vs Accuracy)" 
          option={scatterOption} 
          className="lg:col-span-2"
          loading={loading}
        />
        <div className="liquid-glass-panel p-8 rounded-[2.5rem] flex flex-col">
          <div className="flex items-center justify-between mb-8">
            <h3 className="text-[10px] font-black uppercase tracking-[0.3em] text-slate-400 dark:text-white/30 flex items-center gap-2">
              <AlertTriangle size={14} className="text-rose-500" /> Critical Interference
            </h3>
            <span className="text-[9px] font-black text-slate-400 dark:text-white/20 uppercase tracking-widest">Priority 1-4</span>
          </div>
          <div className="flex-1 space-y-5">
            {report?.topRiskPairs?.map((pair, i) => (
              <div key={i} className="flex items-center justify-between p-4 rounded-3xl bg-slate-50/50 dark:bg-white/[0.03] border border-slate-100 dark:border-white/5 hover:bg-white dark:hover:bg-white/[0.08] transition-all group cursor-pointer shadow-sm">
                <div className="flex items-center gap-4">
                  <div className="w-10 h-10 rounded-2xl bg-rose-500/10 text-rose-600 dark:text-rose-400 flex items-center justify-center text-xs font-black border border-rose-500/20 group-hover:scale-110 transition-transform">
                    {i+1}
                  </div>
                  <div>
                    <div className="text-sm font-black text-slate-800 dark:text-white/90 uppercase tracking-tight">{pair.en} / {pair.fr}</div>
                    <div className="text-[10px] text-slate-400 dark:text-white/30 font-bold mt-0.5">{pair.count} INCIDENCES RECORDED</div>
                  </div>
                </div>
                <div className="text-right">
                  <div className="text-sm font-black text-rose-600 dark:text-rose-400">{(pair.riskScore * 100).toFixed(0)}%</div>
                  <div className="text-[8px] text-slate-400 dark:text-white/20 uppercase font-black tracking-widest">Risk</div>
                </div>
              </div>
            ))}
          </div>
          <button className="mt-10 w-full py-4 bg-slate-100 dark:bg-white/5 rounded-2xl text-[10px] font-black uppercase tracking-[0.2em] text-slate-500 dark:text-white/40 hover:bg-slate-200 dark:hover:bg-white/10 hover:text-slate-900 dark:hover:text-white transition-all border border-slate-200 dark:border-white/5">
            ACCESS FULL REPOSITORY
          </button>
        </div>
      </div>
    </div>
  );
};

export default Analytics;
