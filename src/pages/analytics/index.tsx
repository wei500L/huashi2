import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { ArrowRight, Download, FileText, Filter, Target } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { flushSync } from 'react-dom';
import { Link } from 'react-router-dom';
import { StudentAnalyticsPdfReport } from '@/components/analytics/AnalyticsPdfReport';
import { ChartCard } from '@/components/common/ChartCard';
import { PageHeader, SectionEyebrow } from '@/components/common';
import { saveBlob } from '@/lib/api';
import type { AppChartOption } from '@/lib/echarts';
import { exportReportPagesToPdf } from '@/lib/pdf-report';
import { studentService } from '@/lib/services';
import {
  buildHeatmapOption,
  buildRadarOption,
  buildScatterOption,
  buildTrendOption,
  formatMaybePercent,
  formatMs,
  lexicalPairTypeLabel,
  trainingModeLabel,
} from '@/lib/format';

const AnalyticsPage: React.FC = () => {
  const { t } = useTranslation();
  const [range, setRange] = React.useState<'7d' | '30d'>('30d');
  const [reportErrorMessage, setReportErrorMessage] = React.useState<string | null>(null);
  const [isPdfExporting, setIsPdfExporting] = React.useState(false);
  const [reportGeneratedAt, setReportGeneratedAt] = React.useState<string | null>(null);
  const reportRef = React.useRef<HTMLDivElement | null>(null);

  const overviewQuery = useQuery({
    queryKey: ['student-overview'],
    queryFn: ({ signal }) => studentService.getOverview({ signal }),
  });
  const trendsQuery = useQuery({
    queryKey: ['student-trends', range],
    queryFn: ({ signal }) => studentService.getTrends(range, 'day', { signal }),
  });
  const heatmapQuery = useQuery({
    queryKey: ['student-heatmap', range],
    queryFn: ({ signal }) => studentService.getHeatmap(range, { signal }),
  });
  const scatterQuery = useQuery({
    queryKey: ['student-scatter', range],
    queryFn: ({ signal }) => studentService.getScatter(range, { signal }),
  });
  const highRiskPairsQuery = useQuery({
    queryKey: ['student-high-risk-pairs', range, 8],
    queryFn: ({ signal }) => studentService.getHighRiskPairs(range, 8, { signal }),
  });
  const errorDistributionQuery = useQuery({
    queryKey: ['student-error-distribution', range],
    queryFn: ({ signal }) => studentService.getErrorDistribution(range, { signal }),
  });

  const contextOption: AppChartOption = {
    tooltip: { trigger: 'axis' },
    legend: { bottom: 0, textStyle: { color: '#94a3b8' } },
    grid: { left: '4%', right: '4%', top: '8%', bottom: '18%', containLabel: true },
    xAxis: {
      type: 'category',
      data: (overviewQuery.data?.contextPerformance || []).map((item) => item.contextSupportLevel),
      axisLabel: { color: '#94a3b8' },
    },
    yAxis: [
      { type: 'value', max: 1, name: '正确率 / Accuracy', axisLabel: { color: '#94a3b8', formatter: (value: number) => `${Math.round(value * 100)}%` } },
      { type: 'value', name: '反应时 / ms', axisLabel: { color: '#94a3b8', formatter: (value: number) => `${Math.round(value)} ms` } },
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

  const errorDistributionOption: AppChartOption = {
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
    try {
      setReportErrorMessage(null);
      const blob = await studentService.exportCsv(range);
      saveBlob(blob, `student-analytics-${range}.csv`);
    } catch (error) {
      setReportErrorMessage(error instanceof Error ? error.message : 'CSV 导出失败');
    }
  };

  const handlePdfExport = async () => {
    try {
      setIsPdfExporting(true);
      setReportErrorMessage(null);
      flushSync(() => {
        setReportGeneratedAt(new Date().toISOString());
      });
      await exportReportPagesToPdf(reportRef.current, `student-analytics-${range}-report.pdf`);
    } catch (error) {
      setReportErrorMessage(error instanceof Error ? error.message : 'PDF 报告导出失败');
    } finally {
      setIsPdfExporting(false);
      setReportGeneratedAt(null);
    }
  };

  const canExportPdf =
    Boolean(overviewQuery.data) &&
    Boolean(trendsQuery.data) &&
    Boolean(heatmapQuery.data) &&
    Boolean(scatterQuery.data) &&
    Boolean(highRiskPairsQuery.data) &&
    Boolean(errorDistributionQuery.data);

  return (
    <div className="space-y-10 pb-20">
      <PageHeader
        eyebrow={t('shell.nav.analytics')}
        title={t('analytics.title')}
        subtitle={t('analytics.subtitle')}
        actions={
          <div className="flex flex-wrap gap-3">
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
            <button
              type="button"
              onClick={() => void handlePdfExport()}
              disabled={!canExportPdf || isPdfExporting}
              className="btn-liquid flex items-center gap-2 px-5 py-3 text-white disabled:cursor-not-allowed disabled:opacity-60"
            >
              <FileText size={14} /> {isPdfExporting ? t('common.actions.exportingPdf') : t('common.actions.exportPdf')}
            </button>
            <button
              type="button"
              onClick={() => void handleExport()}
              className="inline-flex items-center gap-2 rounded-full border border-slate-200 px-4 py-3 text-sm font-semibold text-slate-700 transition hover:border-primary/40 hover:text-primary dark:border-white/10 dark:text-white/80"
            >
              <Download size={14} /> {t('common.actions.exportCsv')}
            </button>
          </div>
        }
      />

      {reportErrorMessage && (
        <div className="rounded-[2rem] border border-rose-500/20 bg-rose-500/5 p-6 text-rose-500">{reportErrorMessage}</div>
      )}

      {overviewQuery.error && (
        <div className="rounded-[2rem] border border-rose-500/20 bg-rose-500/5 p-6 text-rose-500">{overviewQuery.error.message}</div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-6">
        {(overviewQuery.data?.cards || []).slice(0, 4).map((card) => (
          <div key={card.key} className="rounded-[2rem] liquid-glass p-6">
            <SectionEyebrow>{card.label}</SectionEyebrow>
            <div className="mt-3 flex items-center justify-between gap-4">
              <div className="text-3xl font-black text-slate-900 dark:text-white">{`${card.value}${card.unit || ''}`}</div>
              <div className="rounded-2xl bg-primary/10 p-3 text-primary">
                <Filter size={18} />
              </div>
            </div>
          </div>
        ))}
      </div>

      <section className="grid gap-6 xl:grid-cols-[1.2fr_0.8fr]" aria-labelledby="analytics-conclusion">
        <div className="rounded-[2.4rem] bg-slate-950 p-8 text-white shadow-xl dark:bg-white/10">
          <div className="text-xs font-black uppercase tracking-[0.25em] text-white/55">结论 / Conclusion</div>
          <h2 id="analytics-conclusion" className="mt-3 text-3xl font-black leading-tight break-words">{overviewQuery.data?.latestSnapshot ? (overviewQuery.data.latestSnapshot.highRiskPairCount ? `当前优先处理 ${overviewQuery.data.latestSnapshot.highRiskPairCount} 个高风险词对` : '暂时没有足够记录生成高风险优先级') : '等待学习数据同步'}</h2>
          <p className="mt-4 max-w-2xl text-sm leading-7 text-white/65">先从风险最高的词对开始一次短训练，再用趋势和错误分布确认是否改善。风险排序用于决定先后，不是对能力的最终判定。</p>
          <Link to="/errors" className="mt-6 inline-flex items-center gap-2 rounded-full bg-white px-5 py-3 text-sm font-bold text-slate-900">打开错题与复习 <ArrowRight size={15} /></Link>
        </div>
        <div className="rounded-[2.4rem] liquid-glass-panel p-8"><div className="text-xs font-black uppercase tracking-[0.25em] text-slate-400">核心证据 / Snapshot</div><div className="mt-4 grid gap-4 sm:grid-cols-3 xl:grid-cols-1"><div><div className="text-sm text-slate-500 dark:text-white/45">近期开答正确率</div><div className="mt-1 text-3xl font-black">{overviewQuery.data?.latestSnapshot ? formatMaybePercent(overviewQuery.data.latestSnapshot.recentAccuracy) : '—'}</div></div><div><div className="text-sm text-slate-500 dark:text-white/45">负迁移风险</div><div className="mt-1 text-3xl font-black text-rose-500">{overviewQuery.data?.latestSnapshot ? formatMaybePercent(overviewQuery.data.latestSnapshot.recentNegativeTransferRisk) : '—'}</div></div><div><div className="text-sm text-slate-500 dark:text-white/45">平均反应时</div><div className="mt-1 text-3xl font-black">{overviewQuery.data?.latestSnapshot ? formatMs(overviewQuery.data.latestSnapshot.recentAvgReactionTimeMs) : '—'}</div></div></div></div>
      </section>

      <SectionEyebrow>证据 / Evidence</SectionEyebrow>

      <div className="grid xl:grid-cols-[0.9fr_1.1fr] gap-8">
        <ChartCard
          title={t('ui.charts.comprehensiveRadar')}
          description="各能力维度使用 0–100% 归一化分数；悬停可查看维度名称。"
          option={buildRadarOption(overviewQuery.data?.radar)}
          loading={overviewQuery.isLoading}
          isEmpty={!overviewQuery.data?.radar.length}
          error={overviewQuery.error}
          onRetry={() => void overviewQuery.refetch()}
        />
        <ChartCard title={t('ui.charts.contextPerformance')} description="柱状图为正确率（%），折线为平均反应时（ms），双轴单位已标注。" option={contextOption} loading={overviewQuery.isLoading} isEmpty={!overviewQuery.data?.contextPerformance.length} error={overviewQuery.error} onRetry={() => void overviewQuery.refetch()} />
      </div>

      <SectionEyebrow>薄弱点 / Weak points</SectionEyebrow>

      <div className="grid xl:grid-cols-2 gap-8">
        <ChartCard
          title={t('ui.charts.transferHeatmap')}
          description="颜色深浅代表样本量；悬停可查看正确率和平均反应时。"
          option={buildHeatmapOption(heatmapQuery.data)}
          loading={heatmapQuery.isLoading}
          isEmpty={!heatmapQuery.data?.cells.length}
          error={heatmapQuery.error}
          onRetry={() => void heatmapQuery.refetch()}
        />
        <ChartCard title={t('ui.sections.errorDistribution')} description="错误数量按类型分布；比例与总样本量在悬停提示中显示。" option={errorDistributionOption} loading={errorDistributionQuery.isLoading} isEmpty={!errorDistributionQuery.data?.length} error={errorDistributionQuery.error} onRetry={() => void errorDistributionQuery.refetch()} />
      </div>

      <section className="rounded-[2.5rem] liquid-glass-panel p-8" aria-labelledby="analytics-weakness-list"><div className="flex items-center justify-between gap-4"><div><SectionEyebrow>优先薄弱点</SectionEyebrow><h2 id="analytics-weakness-list" className="mt-2 text-2xl font-black">先处理风险最高的词对</h2></div><Link to="/errors" className="text-sm font-bold text-primary">查看全部</Link></div>{highRiskPairsQuery.error ? <div className="mt-5 rounded-[1.5rem] border border-rose-500/20 bg-rose-500/5 p-4 text-sm text-rose-600"><div>{highRiskPairsQuery.error.message}</div><button type="button" onClick={() => void highRiskPairsQuery.refetch()} className="mt-3 rounded-full border border-rose-500/30 px-4 py-2 font-bold">重新加载</button></div> : <div className="mt-6 grid gap-4 md:grid-cols-2 xl:grid-cols-4">{(highRiskPairsQuery.data || []).slice(0, 4).map((item) => <div key={item.lexicalPairId} className="rounded-[1.6rem] border border-rose-500/15 bg-rose-500/5 p-4"><div className="flex items-start gap-3"><Target size={17} className="mt-1 shrink-0 text-rose-500" /><div className="min-w-0"><div className="font-black break-words">{item.englishWord} / {item.frenchWord}</div><div className="mt-2 text-sm text-slate-500 dark:text-white/45 break-words">{lexicalPairTypeLabel(item.lexicalPairType)} · 风险 {formatMaybePercent(item.riskScore)}</div><div className="mt-2 text-xs text-slate-500 dark:text-white/45">{item.incorrectCount} / {item.attemptCount} 次错误</div></div></div></div>)}</div>}{!highRiskPairsQuery.isLoading && !highRiskPairsQuery.error && !highRiskPairsQuery.data?.length ? <div className="mt-5 text-sm text-slate-500 dark:text-white/45">{t('ui.labels.noHighRiskPairs')}</div> : null}</section>

      <SectionEyebrow>趋势 / Trend</SectionEyebrow>

      <div className="grid xl:grid-cols-[1.1fr_0.9fr] gap-8">
        <ChartCard title={t('ui.charts.trendAnalysis')} description="横轴为日期，纵轴显示接口返回的趋势指标；图例和单位保留在图表内。" option={buildTrendOption(trendsQuery.data)} loading={trendsQuery.isLoading} isEmpty={!trendsQuery.data?.series.length} error={trendsQuery.error} onRetry={() => void trendsQuery.refetch()} />
        <ChartCard
          title={t('ui.charts.latencyAccuracyScatter')}
          description="横轴为平均反应时（ms），纵轴为正确率（%）；点大小代表尝试次数。"
          option={buildScatterOption(scatterQuery.data)}
          loading={scatterQuery.isLoading}
          isEmpty={!scatterQuery.data?.points.length}
          error={scatterQuery.error}
          onRetry={() => void scatterQuery.refetch()}
        />
      </div>

      <section className="rounded-[2.5rem] border border-primary/20 bg-primary/5 p-8" aria-labelledby="analytics-recommendations"><SectionEyebrow>推荐行动 / Recommended actions</SectionEyebrow><h2 id="analytics-recommendations" className="mt-2 text-2xl font-black">下一次练习做什么</h2><p className="mt-3 max-w-3xl text-sm leading-7 text-slate-600 dark:text-white/65">系统根据最近风险与快照给出训练入口；你可以先完成一组短练习，再返回趋势确认变化。</p><div className="mt-5 flex flex-wrap items-center gap-4"><div className="rounded-full bg-white/80 px-4 py-3 text-sm font-bold dark:bg-white/10">{overviewQuery.data ? trainingModeLabel(overviewQuery.data.recommendedTrainingMode) : '推荐训练模式待同步'}</div><Link to="/training" className="btn-liquid inline-flex items-center gap-2 px-5 py-3 text-white">开始推荐训练 <ArrowRight size={15} /></Link></div></section>

      {overviewQuery.data?.latestSnapshot && (
        <section className="rounded-[2.5rem] liquid-glass-panel p-8">
          <SectionEyebrow className="mb-6">{t('ui.sections.latestSnapshot')} · Snapshot detail</SectionEyebrow>
          <div className="grid md:grid-cols-3 gap-4">
            <div className="rounded-[1.6rem] border border-slate-200/70 dark:border-white/10 p-4 bg-white/60 dark:bg-white/5">
              <div className="text-sm text-slate-500 dark:text-white/45">{t('ui.meta.correctRate')}</div>
              <div className="mt-2 text-2xl font-black text-slate-900 dark:text-white">
                {formatMaybePercent(overviewQuery.data.latestSnapshot.recentAccuracy)}
              </div>
            </div>
            <div className="rounded-[1.6rem] border border-slate-200/70 dark:border-white/10 p-4 bg-white/60 dark:bg-white/5">
              <div className="text-sm text-slate-500 dark:text-white/45">{t('ui.meta.risk')}</div>
              <div className="mt-2 text-2xl font-black text-rose-500">
                {formatMaybePercent(overviewQuery.data.latestSnapshot.recentNegativeTransferRisk)}
              </div>
            </div>
            <div className="rounded-[1.6rem] border border-slate-200/70 dark:border-white/10 p-4 bg-white/60 dark:bg-white/5">
              <div className="text-sm text-slate-500 dark:text-white/45">{t('ui.fields.averageReactionTime')}</div>
              <div className="mt-2 text-2xl font-black text-slate-900 dark:text-white">
                {formatMs(overviewQuery.data.latestSnapshot.recentAvgReactionTimeMs)}
              </div>
            </div>
          </div>
        </section>
      )}

      {reportGeneratedAt ? (
        <StudentAnalyticsPdfReport
          reportRef={reportRef}
          range={range}
          generatedAt={reportGeneratedAt}
          overview={overviewQuery.data}
          trend={trendsQuery.data}
          heatmap={heatmapQuery.data}
          scatter={scatterQuery.data}
          highRiskPairs={highRiskPairsQuery.data}
          errorDistribution={errorDistributionQuery.data}
        />
      ) : null}
    </div>
  );
};

export default AnalyticsPage;
