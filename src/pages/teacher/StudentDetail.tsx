import React from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Brain, Wand2 } from 'lucide-react';
import { ChartCard } from '@/components/common/ChartCard';
import { PageHeader } from '@/components/common';
import { aiService, teacherAnalyticsService, teacherInterventionService } from '@/lib/services';
import { buildHeatmapOption, buildRadarOption, buildScatterOption, buildTrendOption, formatMaybePercent, formatMs, lexicalPairTypeLabel } from '@/lib/format';
import { useParams } from 'react-router-dom';

const TeacherStudentDetailPage: React.FC = () => {
  const params = useParams();
  const classId = Number(params.classId);
  const studentUserId = Number(params.studentUserId);
  const queryClient = useQueryClient();

  const detailQuery = useQuery({
    queryKey: ['teacher-student-detail', classId, studentUserId],
    queryFn: ({ signal }) => teacherAnalyticsService.getStudentDetail(classId, studentUserId, { signal }),
    enabled: Number.isFinite(classId) && Number.isFinite(studentUserId),
  });

  const interventionsQuery = useQuery({
    queryKey: ['teacher-interventions', classId],
    queryFn: ({ signal }) => teacherInterventionService.list({ classId, pageNo: 1, pageSize: 20 }, { signal }),
    enabled: Number.isFinite(classId),
  });

  const suggestMutation = useMutation({
    mutationFn: () =>
      aiService.suggestTeacherIntervention({
        classId,
        studentUserId,
        diagnosisSummaryId: detailQuery.data?.analysis.overview.latestSnapshot.lastDiagnosisSummaryId,
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['teacher-interventions', classId] });
    },
  });

  const detail = detailQuery.data;
  const analysis = detail?.analysis;
  const studentInterventions = (interventionsQuery.data?.records || []).filter((item) => item.studentUserId === studentUserId);

  return (
    <div className="space-y-10 pb-20">
      <PageHeader
        title={detail?.studentName || '学生详情'}
        subtitle={
          detail
            ? `班级排名 ${detail.classRank} · Percentile ${(detail.classPercentile * 100).toFixed(0)}%`
            : '正在加载学生分析'
        }
        actions={
          <button
            type="button"
            onClick={() => suggestMutation.mutate()}
            disabled={suggestMutation.isPending}
            className="btn-liquid px-5 py-3 text-white flex items-center gap-2 disabled:opacity-60"
          >
            <Wand2 size={14} /> 生成干预建议
          </button>
        }
      />

      {detailQuery.error && (
        <div className="rounded-[2rem] border border-rose-500/20 bg-rose-500/5 p-6 text-rose-500">{detailQuery.error.message}</div>
      )}

      {analysis && (
        <>
          <div className="grid md:grid-cols-3 gap-6">
            <div className="rounded-[2rem] liquid-glass p-6">
              <div className="text-xs uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">最近准确率</div>
              <div className="mt-3 text-3xl font-black text-slate-900 dark:text-white">
                {formatMaybePercent(analysis.overview.latestSnapshot.recentAccuracy)}
              </div>
            </div>
            <div className="rounded-[2rem] liquid-glass p-6">
              <div className="text-xs uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">最近负迁移风险</div>
              <div className="mt-3 text-3xl font-black text-rose-500">
                {formatMaybePercent(analysis.overview.latestSnapshot.recentNegativeTransferRisk)}
              </div>
            </div>
            <div className="rounded-[2rem] liquid-glass p-6">
              <div className="text-xs uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">最近平均反应时</div>
              <div className="mt-3 text-3xl font-black text-slate-900 dark:text-white">
                {formatMs(analysis.overview.latestSnapshot.recentAvgReactionTimeMs)}
              </div>
            </div>
          </div>

          <div className="grid xl:grid-cols-[0.95fr_1.05fr] gap-8">
            <ChartCard title="学生雷达" option={buildRadarOption(analysis.overview.radar)} loading={detailQuery.isLoading} isEmpty={!analysis.overview.radar.length} />
            <ChartCard title="近 7 天趋势" option={buildTrendOption(analysis.trend7d)} loading={detailQuery.isLoading} isEmpty={!analysis.trend7d.series.length} />
          </div>

          <div className="grid xl:grid-cols-2 gap-8">
            <ChartCard title="迁移热力图" option={buildHeatmapOption(analysis.transferHeatmap)} loading={detailQuery.isLoading} isEmpty={!analysis.transferHeatmap.cells.length} />
            <ChartCard title="散点图" option={buildScatterOption(analysis.scatter)} loading={detailQuery.isLoading} isEmpty={!analysis.scatter.points.length} />
          </div>

          <div className="grid xl:grid-cols-[1fr_1fr_0.9fr] gap-8">
            <section className="rounded-[2.5rem] liquid-glass-panel p-8">
              <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30 mb-6">high risk pairs</div>
              <div className="space-y-4">
                {analysis.highRiskPairs.map((item) => (
                  <div key={item.lexicalPairId} className="rounded-[1.6rem] border border-slate-200/70 dark:border-white/10 p-4 bg-white/60 dark:bg-white/5">
                    <div className="font-black text-slate-900 dark:text-white">{item.englishWord} / {item.frenchWord}</div>
                    <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                      {lexicalPairTypeLabel(item.lexicalPairType)} · 风险 {formatMaybePercent(item.riskScore)}
                    </div>
                  </div>
                ))}
              </div>
            </section>

            <section className="rounded-[2.5rem] liquid-glass-panel p-8">
              <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30 mb-6">error distribution</div>
              <div className="space-y-4">
                {analysis.errorDistribution.map((item) => (
                  <div key={item.key} className="rounded-[1.6rem] border border-slate-200/70 dark:border-white/10 p-4 bg-white/60 dark:bg-white/5">
                    <div className="flex items-center justify-between gap-4">
                      <span className="font-bold text-slate-900 dark:text-white">{item.label}</span>
                      <span className="text-sm text-slate-500 dark:text-white/45">
                        {item.count} / {formatMaybePercent(item.ratio)}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            </section>

            <section className="rounded-[2.5rem] liquid-glass-panel p-8">
              <div className="flex items-center gap-3 mb-6">
                <Brain size={16} className="text-primary" />
                <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">AI intervention</div>
              </div>
              {suggestMutation.data ? (
                <div className="space-y-4">
                  <p className="text-sm leading-7 text-slate-800 dark:text-white/85">{suggestMutation.data.explanation}</p>
                  <div className="rounded-[1.6rem] border border-slate-200/70 dark:border-white/10 p-4 bg-white/60 dark:bg-white/5">
                    <div className="font-bold text-slate-900 dark:text-white">教师备注</div>
                    <div className="mt-2 text-sm text-slate-500 dark:text-white/45">{suggestMutation.data.teacherNote}</div>
                  </div>
                </div>
              ) : suggestMutation.error ? (
                <div className="text-sm text-rose-500">{suggestMutation.error.message}</div>
              ) : (
                <div className="text-sm text-slate-500 dark:text-white/45">点击右上角按钮生成一条新的干预建议。</div>
              )}
            </section>
          </div>

          <section className="rounded-[2.5rem] liquid-glass-panel p-8">
            <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30 mb-6">intervention history</div>
            <div className="space-y-4">
              {studentInterventions.map((item) => (
                <div key={item.id} className="rounded-[1.6rem] border border-slate-200/70 dark:border-white/10 p-4 bg-white/60 dark:bg-white/5">
                  <div className="flex items-center justify-between gap-4">
                    <div>
                      <div className="font-black text-slate-900 dark:text-white">{item.patternDetected}</div>
                      <div className="mt-2 text-sm text-slate-500 dark:text-white/45">{item.suggestedAction}</div>
                    </div>
                    <div className="text-right text-sm text-slate-500 dark:text-white/45">
                      <div>{item.priority}</div>
                      <div>{item.status}</div>
                    </div>
                  </div>
                </div>
              ))}
              {!studentInterventions.length && (
                <div className="text-sm text-slate-500 dark:text-white/45">当前没有该学生的干预记录。</div>
              )}
            </div>
          </section>
        </>
      )}
    </div>
  );
};

export default TeacherStudentDetailPage;
