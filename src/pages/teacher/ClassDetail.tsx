import React from 'react';
import { Link, useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import type { EChartsOption } from 'echarts';
import { Download, Users } from 'lucide-react';
import { ChartCard } from '@/components/common/ChartCard';
import { PageHeader, StatCard } from '@/components/common';
import { saveBlob } from '@/lib/api';
import { teacherAnalyticsService } from '@/lib/services';
import { buildHeatmapOption, buildRadarOption, buildTrendOption, formatMaybePercent, formatMs } from '@/lib/format';

const TeacherClassDetailPage: React.FC = () => {
  const params = useParams();
  const classId = Number(params.classId);

  const overviewQuery = useQuery({
    queryKey: ['teacher-class-overview', classId],
    queryFn: () => teacherAnalyticsService.getClassOverview(classId),
    enabled: Number.isFinite(classId),
  });
  const riskDistributionQuery = useQuery({
    queryKey: ['teacher-class-risk-distribution', classId],
    queryFn: () => teacherAnalyticsService.getRiskDistribution(classId),
    enabled: Number.isFinite(classId),
  });
  const heatmapQuery = useQuery({
    queryKey: ['teacher-class-heatmap', classId],
    queryFn: () => teacherAnalyticsService.getHeatmap(classId),
    enabled: Number.isFinite(classId),
  });
  const errorDistributionQuery = useQuery({
    queryKey: ['teacher-class-error-distribution', classId],
    queryFn: () => teacherAnalyticsService.getErrorDistribution(classId),
    enabled: Number.isFinite(classId),
  });
  const completionRateQuery = useQuery({
    queryKey: ['teacher-class-completion-rate', classId],
    queryFn: () => teacherAnalyticsService.getCompletionRate(classId),
    enabled: Number.isFinite(classId),
  });
  const studentsQuery = useQuery({
    queryKey: ['teacher-class-students', classId],
    queryFn: () => teacherAnalyticsService.listStudents(classId),
    enabled: Number.isFinite(classId),
  });

  const riskDistributionOption = {
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
  } as EChartsOption;

  const errorDistributionOption = {
    tooltip: { trigger: 'item' },
    series: [
      {
        type: 'pie',
        radius: ['52%', '78%'],
        data: (errorDistributionQuery.data || []).map((item) => ({ name: item.label, value: item.count })),
      },
    ],
  } as EChartsOption;

  const handleExport = async () => {
    const blob = await teacherAnalyticsService.exportClassCsv(classId);
    saveBlob(blob, `class-${classId}-analytics.csv`);
  };

  return (
    <div className="space-y-10 pb-20">
      <PageHeader
        title={overviewQuery.data?.className || '班级详情'}
        subtitle={`班级编码 ${overviewQuery.data?.classCode || '--'} · 主风险 ${overviewQuery.data?.primaryRiskLevel || '--'}`}
        actions={
          <button type="button" onClick={() => void handleExport()} className="btn-liquid px-5 py-3 text-white flex items-center gap-2">
            <Download size={14} /> 导出 CSV
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
          title="班级雷达"
          option={buildRadarOption(overviewQuery.data?.radar)}
          loading={overviewQuery.isLoading}
          isEmpty={!overviewQuery.data?.radar.length}
        />
        <ChartCard
          title="班级热力图"
          option={buildHeatmapOption(heatmapQuery.data)}
          loading={heatmapQuery.isLoading}
          isEmpty={!heatmapQuery.data?.cells.length}
        />
      </div>

      <div className="grid xl:grid-cols-3 gap-8">
        <ChartCard
          title="风险分桶"
          option={riskDistributionOption}
          loading={riskDistributionQuery.isLoading}
          isEmpty={!riskDistributionQuery.data?.length}
        />
        <ChartCard
          title="错误分布"
          option={errorDistributionOption}
          loading={errorDistributionQuery.isLoading}
          isEmpty={!errorDistributionQuery.data?.length}
        />
        <ChartCard
          title="完成率趋势"
          option={buildTrendOption(completionRateQuery.data?.trend)}
          loading={completionRateQuery.isLoading}
          isEmpty={!completionRateQuery.data?.trend.series.length}
        />
      </div>

      <section className="rounded-[2.5rem] liquid-glass-panel p-8">
        <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30 mb-6">students</div>
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
                    {item.gradeName} · 推荐 {item.recommendedTrainingMode}
                  </div>
                </div>
                <div className="text-right text-sm text-slate-500 dark:text-white/45">
                  <div>准确率 {formatMaybePercent(item.recentAccuracy)}</div>
                  <div>风险 {formatMaybePercent(item.recentNegativeTransferRisk)}</div>
                  <div>{formatMs(item.recentAvgReactionTimeMs)}</div>
                </div>
              </div>
            </Link>
          ))}
          {!studentsQuery.isLoading && !studentsQuery.data?.length && (
            <div className="text-sm text-slate-500 dark:text-white/45">班级下暂无学生。</div>
          )}
        </div>
      </section>
    </div>
  );
};

export default TeacherClassDetailPage;
