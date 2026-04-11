import React from 'react';
import { Link, useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Download, Users } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { ChartCard } from '@/components/common/ChartCard';
import { PageHeader, SectionEyebrow, StatCard } from '@/components/common';
import { saveBlob } from '@/lib/api';
import type { AppChartOption } from '@/lib/echarts';
import { teacherAnalyticsService } from '@/lib/services';
import { buildHeatmapOption, buildRadarOption, buildTrendOption, formatMaybePercent, formatMs, trainingModeLabel } from '@/lib/format';

const TeacherClassDetailPage: React.FC = () => {
  const { t } = useTranslation();
  const params = useParams();
  const classId = Number(params.classId);

  const overviewQuery = useQuery({
    queryKey: ['teacher-class-overview', classId],
    queryFn: ({ signal }) => teacherAnalyticsService.getClassOverview(classId, '30d', { signal }),
    enabled: Number.isFinite(classId),
  });
  const riskDistributionQuery = useQuery({
    queryKey: ['teacher-class-risk-distribution', classId],
    queryFn: ({ signal }) => teacherAnalyticsService.getRiskDistribution(classId, { signal }),
    enabled: Number.isFinite(classId),
  });
  const heatmapQuery = useQuery({
    queryKey: ['teacher-class-heatmap', classId],
    queryFn: ({ signal }) => teacherAnalyticsService.getHeatmap(classId, '30d', { signal }),
    enabled: Number.isFinite(classId),
  });
  const errorDistributionQuery = useQuery({
    queryKey: ['teacher-class-error-distribution', classId],
    queryFn: ({ signal }) => teacherAnalyticsService.getErrorDistribution(classId, '30d', { signal }),
    enabled: Number.isFinite(classId),
  });
  const completionRateQuery = useQuery({
    queryKey: ['teacher-class-completion-rate', classId],
    queryFn: ({ signal }) => teacherAnalyticsService.getCompletionRate(classId, '30d', 'day', { signal }),
    enabled: Number.isFinite(classId),
  });
  const studentsQuery = useQuery({
    queryKey: ['teacher-class-students', classId],
    queryFn: ({ signal }) => teacherAnalyticsService.listStudents(classId, { signal }),
    enabled: Number.isFinite(classId),
  });

  const riskDistributionOption: AppChartOption = {
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: (riskDistributionQuery.data || []).map((item) => `${item.bucketStart.toFixed(1)}-${item.bucketEnd.toFixed(1)}`),
      axisLabel: { color: '#94a3b8' },
    },
    yAxis: { type: 'value', axisLabel: { color: '#94a3b8' } },
    series: [
      {
        type: 'bar',
        data: (riskDistributionQuery.data || []).map((item) => item.studentCount),
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
    const blob = await teacherAnalyticsService.exportClassCsv(classId);
    saveBlob(blob, `class-${classId}-analytics.csv`);
  };

  return (
    <div className="space-y-10 pb-20">
      <PageHeader
        eyebrow={t('ui.sections.students')}
        title={overviewQuery.data?.className || t('ui.pages.classDetail.fallbackTitle')}
        subtitle={`${t('ui.meta.classCode', { code: overviewQuery.data?.classCode || '--' })} · ${t('ui.meta.primaryRisk', {
          risk: overviewQuery.data?.primaryRiskLevel || '--',
        })} · ${t('ui.meta.nextClassStep')}`}
        actions={
          <button type="button" onClick={() => void handleExport()} className="btn-liquid px-5 py-3 text-white flex items-center gap-2">
            <Download size={14} /> {t('common.actions.exportCsv')}
          </button>
        }
      />

      {overviewQuery.error && (
        <div className="rounded-[2rem] border border-rose-500/20 bg-rose-500/5 p-6 text-rose-500">{overviewQuery.error.message}</div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-6">
        {(overviewQuery.data?.cards || []).slice(0, 4).map((card) => (
          <StatCard key={card.key} title={card.label} value={`${card.value}${card.unit || ''}`} icon={Users} />
        ))}
      </div>

      <div className="grid xl:grid-cols-[0.95fr_1.05fr] gap-8">
        <ChartCard
          title={t('ui.charts.classRadar')}
          option={buildRadarOption(overviewQuery.data?.radar)}
          loading={overviewQuery.isLoading}
          isEmpty={!overviewQuery.data?.radar.length}
        />
        <ChartCard
          title={t('ui.charts.classHeatmap')}
          option={buildHeatmapOption(heatmapQuery.data)}
          loading={heatmapQuery.isLoading}
          isEmpty={!heatmapQuery.data?.cells.length}
        />
      </div>

      <div className="grid xl:grid-cols-3 gap-8">
        <ChartCard
          title={t('ui.charts.riskBuckets')}
          option={riskDistributionOption}
          loading={riskDistributionQuery.isLoading}
          isEmpty={!riskDistributionQuery.data?.length}
        />
        <ChartCard
          title={t('ui.sections.errorDistribution')}
          option={errorDistributionOption}
          loading={errorDistributionQuery.isLoading}
          isEmpty={!errorDistributionQuery.data?.length}
        />
        <ChartCard
          title={t('ui.charts.completionTrend')}
          option={buildTrendOption(completionRateQuery.data?.trend)}
          loading={completionRateQuery.isLoading}
          isEmpty={!completionRateQuery.data?.trend.series.length}
        />
      </div>

      <section className="rounded-[2.5rem] liquid-glass-panel p-8">
        <SectionEyebrow className="mb-6">{t('ui.sections.students')}</SectionEyebrow>
        <div className="space-y-4">
          {(studentsQuery.data || []).map((item) => (
            <Link
              key={item.studentUserId}
              to={`/teacher/classes/${classId}/students/${item.studentUserId}`}
              className="block rounded-[1.6rem] border border-slate-200/70 dark:border-white/10 p-5 bg-white/60 dark:bg-white/5 hover:border-primary/40 transition-all"
            >
              <div className="flex items-center justify-between gap-4">
                <div>
                  <div className="font-black text-slate-900 dark:text-white">{item.studentName}</div>
                  <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                    {item.gradeName} · {trainingModeLabel(item.recommendedTrainingMode)}
                  </div>
                </div>
                <div className="text-right text-sm text-slate-500 dark:text-white/45">
                  <div>{t('ui.meta.correctRate')} {formatMaybePercent(item.recentAccuracy)}</div>
                  <div>{t('ui.meta.risk')} {formatMaybePercent(item.recentNegativeTransferRisk)}</div>
                  <div>{formatMs(item.recentAvgReactionTimeMs)}</div>
                </div>
              </div>
            </Link>
          ))}
          {!studentsQuery.isLoading && !studentsQuery.data?.length && (
            <div className="text-sm text-slate-500 dark:text-white/45">{t('ui.labels.noStudents')}</div>
          )}
        </div>
      </section>
    </div>
  );
};

export default TeacherClassDetailPage;
