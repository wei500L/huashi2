import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { Download, FileText, Filter } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { flushSync } from 'react-dom';
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

      <div className="grid xl:grid-cols-[0.9fr_1.1fr] gap-8">
        <ChartCard
          title={t('ui.charts.comprehensiveRadar')}
          option={buildRadarOption(overviewQuery.data?.radar)}
          loading={overviewQuery.isLoading}
          isEmpty={!overviewQuery.data?.radar.length}
        />
        <ChartCard
          title={t('ui.charts.trendAnalysis')}
          option={buildTrendOption(trendsQuery.data)}
          loading={trendsQuery.isLoading}
          isEmpty={!trendsQuery.data?.series.length}
        />
      </div>

      <div className="grid xl:grid-cols-2 gap-8">
        <ChartCard
          title={t('ui.charts.transferHeatmap')}
          option={buildHeatmapOption(heatmapQuery.data)}
          loading={heatmapQuery.isLoading}
          isEmpty={!heatmapQuery.data?.cells.length}
        />
        <ChartCard
          title={t('ui.charts.contextPerformance')}
          option={contextOption}
          loading={overviewQuery.isLoading}
          isEmpty={!overviewQuery.data?.contextPerformance.length}
        />
      </div>

      <div className="grid xl:grid-cols-[1.1fr_0.9fr] gap-8">
        <ChartCard
          title={t('ui.charts.latencyAccuracyScatter')}
          option={buildScatterOption(scatterQuery.data)}
          loading={scatterQuery.isLoading}
          isEmpty={!scatterQuery.data?.points.length}
        />
        <ChartCard
          title={t('ui.sections.errorDistribution')}
          option={errorDistributionOption}
          loading={errorDistributionQuery.isLoading}
          isEmpty={!errorDistributionQuery.data?.length}
        />
      </div>

      <section className="rounded-[2.5rem] liquid-glass-panel p-8">
        <SectionEyebrow className="mb-6">{t('ui.sections.highRiskPairs')}</SectionEyebrow>
        <div className="grid md:grid-cols-2 xl:grid-cols-4 gap-4">
          {(highRiskPairsQuery.data || []).map((item) => (
            <div key={item.lexicalPairId} className="rounded-[1.6rem] border border-slate-200/70 dark:border-white/10 p-4 bg-white/60 dark:bg-white/5">
              <div className="font-black text-slate-900 dark:text-white">{item.englishWord} / {item.frenchWord}</div>
              <div className="mt-2 text-sm text-slate-500 dark:text-white/45">{lexicalPairTypeLabel(item.lexicalPairType)}</div>
              <div className="mt-4 flex items-center justify-between gap-4 text-sm">
                <span className="text-rose-500 font-bold">{formatMaybePercent(item.riskScore)}</span>
                <span className="text-slate-500 dark:text-white/45">{item.incorrectCount} / {item.attemptCount}</span>
              </div>
            </div>
          ))}
          {!highRiskPairsQuery.isLoading && !highRiskPairsQuery.data?.length && (
            <div className="text-sm text-slate-500 dark:text-white/45">{t('ui.labels.noHighRiskPairs')}</div>
          )}
        </div>
      </section>

      {overviewQuery.data?.latestSnapshot && (
        <section className="rounded-[2.5rem] liquid-glass-panel p-8">
          <SectionEyebrow className="mb-6">{t('ui.sections.latestSnapshot')}</SectionEyebrow>
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
