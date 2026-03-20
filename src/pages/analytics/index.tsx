import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { Download, Filter } from 'lucide-react';
import { ChartCard } from '@/components/common/ChartCard';
import { PageHeader, StatCard } from '@/components/common';
import { saveBlob } from '@/lib/api';
import { studentService } from '@/lib/services';
import {
  buildHeatmapOption,
  buildRadarOption,
  buildScatterOption,
  buildTrendOption,
  formatMaybePercent,
  formatMs,
  lexicalPairTypeLabel,
} from '@/lib/format';

const AnalyticsPage: React.FC = () => {
  const [range, setRange] = React.useState<'7d' | '30d'>('30d');

  const overviewQuery = useQuery({
    queryKey: ['student-overview'],
    queryFn: () => studentService.getOverview(),
  });
  const trendsQuery = useQuery({
    queryKey: ['student-trends', range],
    queryFn: () => studentService.getTrends(range),
  });
  const heatmapQuery = useQuery({
    queryKey: ['student-heatmap', range],
    queryFn: () => studentService.getHeatmap(range),
  });
  const scatterQuery = useQuery({
    queryKey: ['student-scatter', range],
    queryFn: () => studentService.getScatter(range),
  });
  const highRiskPairsQuery = useQuery({
    queryKey: ['student-high-risk-pairs', range, 8],
    queryFn: () => studentService.getHighRiskPairs(range, 8),
  });
  const errorDistributionQuery = useQuery({
    queryKey: ['student-error-distribution', range],
    queryFn: () => studentService.getErrorDistribution(range),
  });

  const contextOption = {
    tooltip: { trigger: 'axis' },
    legend: { bottom: 0, textStyle: { color: '#94a3b8' } },
    grid: { left: '4%', right: '4%', top: '8%', bottom: '18%', containLabel: true },
    xAxis: {
      type: 'category',
      data: (overviewQuery.data?.contextPerformance || []).map((item) => item.contextSupportLevel),
      axisLabel: { color: '#94a3b8' },
    },
    yAxis: [
      { type: 'value', max: 1, axisLabel: { color: '#94a3b8' } },
      { type: 'value', axisLabel: { color: '#94a3b8' } },
    ],
    series: [
      {
        name: '正确率',
        type: 'bar',
        data: (overviewQuery.data?.contextPerformance || []).map((item) => item.accuracy),
      },
      {
        name: '平均反应时',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        data: (overviewQuery.data?.contextPerformance || []).map((item) => item.avgReactionTimeMs),
      },
    ],
  };

  const errorDistributionOption = {
    tooltip: { trigger: 'item' },
    series: [
      {
        type: 'pie',
        radius: ['52%', '78%'],
        data: (errorDistributionQuery.data || []).map((item) => ({ name: item.label, value: item.count })),
      },
    ],
  };

  const handleExport = async () => {
    const blob = await studentService.exportCsv(range);
    saveBlob(blob, `student-analytics-${range}.csv`);
  };

  return (
    <div className="space-y-10 pb-20">
      <PageHeader
        title="学情分析"
        subtitle="真实聚合后的趋势、热力图、散点图和高风险词对。"
        actions={
          <div className="flex gap-3">
            <div className="flex gap-2 rounded-full border border-slate-200 dark:border-white/10 p-1">
              {(['7d', '30d'] as const).map((value) => (
                <button
                  key={value}
                  type="button"
                  onClick={() => setRange(value)}
                  className={`px-4 py-2 rounded-full text-xs font-black uppercase tracking-[0.24em] ${
                    range === value ? 'bg-primary text-white' : 'text-slate-500 dark:text-white/45'
                  }`}
                >
                  {value}
                </button>
              ))}
            </div>
            <button type="button" onClick={() => void handleExport()} className="btn-liquid px-5 py-3 text-white flex items-center gap-2">
              <Download size={14} /> 导出 CSV
            </button>
          </div>
        }
      />

      {overviewQuery.error && (
        <div className="rounded-[2rem] border border-rose-500/20 bg-rose-500/5 p-6 text-rose-500">{overviewQuery.error.message}</div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-6">
        {(overviewQuery.data?.cards || []).slice(0, 4).map((card) => (
          <StatCard key={card.key} title={card.label} value={`${card.value}${card.unit || ''}`} icon={Filter} />
        ))}
      </div>

      <div className="grid xl:grid-cols-[0.9fr_1.1fr] gap-8">
        <ChartCard
          title="综合能力雷达"
          option={buildRadarOption(overviewQuery.data?.radar)}
          loading={overviewQuery.isLoading}
          isEmpty={!overviewQuery.data?.radar.length}
        />
        <ChartCard
          title="趋势分析"
          option={buildTrendOption(trendsQuery.data)}
          loading={trendsQuery.isLoading}
          isEmpty={!trendsQuery.data?.series.length}
        />
      </div>

      <div className="grid xl:grid-cols-2 gap-8">
        <ChartCard
          title="迁移热力图"
          option={buildHeatmapOption(heatmapQuery.data)}
          loading={heatmapQuery.isLoading}
          isEmpty={!heatmapQuery.data?.cells.length}
        />
        <ChartCard
          title="语境表现"
          option={contextOption}
          loading={overviewQuery.isLoading}
          isEmpty={!overviewQuery.data?.contextPerformance.length}
        />
      </div>

      <div className="grid xl:grid-cols-[1.1fr_0.9fr] gap-8">
        <ChartCard
          title="延迟-准确率散点"
          option={buildScatterOption(scatterQuery.data)}
          loading={scatterQuery.isLoading}
          isEmpty={!scatterQuery.data?.points.length}
        />
        <ChartCard
          title="错误分布"
          option={errorDistributionOption}
          loading={errorDistributionQuery.isLoading}
          isEmpty={!errorDistributionQuery.data?.length}
        />
      </div>

      <section className="rounded-[2.5rem] liquid-glass-panel p-8">
        <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30 mb-6">high risk pairs</div>
        <div className="grid md:grid-cols-2 xl:grid-cols-4 gap-4">
          {(highRiskPairsQuery.data || []).map((item) => (
            <div key={item.lexicalPairId} className="rounded-[1.6rem] border border-slate-200/70 dark:border-white/10 p-4 bg-white/60 dark:bg-white/5">
              <div className="font-black text-slate-900 dark:text-white">{item.englishWord} / {item.frenchWord}</div>
              <div className="mt-2 text-sm text-slate-500 dark:text-white/45">{lexicalPairTypeLabel(item.lexicalPairType)}</div>
              <div className="mt-4 flex items-center justify-between gap-4 text-sm">
                <span className="text-rose-500 font-bold">{formatMaybePercent(item.riskScore)}</span>
                <span className="text-slate-500 dark:text-white/45">错 {item.incorrectCount} / {item.attemptCount}</span>
              </div>
            </div>
          ))}
          {!highRiskPairsQuery.isLoading && !highRiskPairsQuery.data?.length && (
            <div className="text-sm text-slate-500 dark:text-white/45">暂无高风险词对。</div>
          )}
        </div>
      </section>

      {overviewQuery.data?.latestSnapshot && (
        <section className="rounded-[2.5rem] liquid-glass-panel p-8">
          <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30 mb-6">latest snapshot</div>
          <div className="grid md:grid-cols-3 gap-4">
            <div className="rounded-[1.6rem] border border-slate-200/70 dark:border-white/10 p-4 bg-white/60 dark:bg-white/5">
              <div className="text-sm text-slate-500 dark:text-white/45">最近准确率</div>
              <div className="mt-2 text-2xl font-black text-slate-900 dark:text-white">
                {formatMaybePercent(overviewQuery.data.latestSnapshot.recentAccuracy)}
              </div>
            </div>
            <div className="rounded-[1.6rem] border border-slate-200/70 dark:border-white/10 p-4 bg-white/60 dark:bg-white/5">
              <div className="text-sm text-slate-500 dark:text-white/45">最近负迁移风险</div>
              <div className="mt-2 text-2xl font-black text-rose-500">
                {formatMaybePercent(overviewQuery.data.latestSnapshot.recentNegativeTransferRisk)}
              </div>
            </div>
            <div className="rounded-[1.6rem] border border-slate-200/70 dark:border-white/10 p-4 bg-white/60 dark:bg-white/5">
              <div className="text-sm text-slate-500 dark:text-white/45">最近平均反应时</div>
              <div className="mt-2 text-2xl font-black text-slate-900 dark:text-white">
                {formatMs(overviewQuery.data.latestSnapshot.recentAvgReactionTimeMs)}
              </div>
            </div>
          </div>
        </section>
      )}
    </div>
  );
};

export default AnalyticsPage;
