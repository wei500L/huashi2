import React from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Brain, Wand2 } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useParams } from 'react-router-dom';
import { ChartCard } from '@/components/common/ChartCard';
import { PageHeader, PanelSkeleton, SectionEyebrow, StatusBadge } from '@/components/common';
import type { TeacherInterventionSummaryVO } from '@/lib/contracts';
import { aiService, teacherAnalyticsService, teacherInterventionService } from '@/lib/services';
import {
  buildHeatmapOption,
  buildRadarOption,
  buildScatterOption,
  buildTrendOption,
  formatDateTime,
  formatMaybePercent,
  formatMs,
  interventionPriorityLabel,
  interventionStatusLabel,
  interventionStatusTone,
  lexicalPairTypeLabel,
} from '@/lib/format';
import { getApiErrorMessage } from '@/lib/api';

type InterventionFormState = {
  priority: string;
  status: string;
  plannedAt: string;
  teacherNote: string;
};

const PRIORITY_OPTIONS = ['LOW', 'NORMAL', 'URGENT'] as const;
const STATUS_OPTIONS = ['PENDING', 'IN_PROGRESS', 'COMPLETED'] as const;

function toDateTimeLocalValue(value?: string | null): string {
  if (!value) {
    return '';
  }
  return value.slice(0, 16);
}

function buildInterventionForm(item?: TeacherInterventionSummaryVO | null): InterventionFormState {
  return {
    priority: item?.priority || 'NORMAL',
    status: item?.status || 'PENDING',
    plannedAt: toDateTimeLocalValue(item?.plannedAt || item?.createdAt || new Date().toISOString()),
    teacherNote: item?.teacherNote || item?.suggestedAction || '',
  };
}

const TeacherStudentDetailPage: React.FC = () => {
  const { t } = useTranslation();
  const params = useParams();
  const classId = Number(params.classId);
  const studentUserId = Number(params.studentUserId);
  const queryClient = useQueryClient();
  const [selectedInterventionId, setSelectedInterventionId] = React.useState<number | null>(null);
  const [form, setForm] = React.useState<InterventionFormState>(buildInterventionForm());

  const detailQuery = useQuery({
    queryKey: ['teacher-student-detail', classId, studentUserId],
    queryFn: ({ signal }) => teacherAnalyticsService.getStudentDetail(classId, studentUserId, { signal }),
    enabled: Number.isFinite(classId) && Number.isFinite(studentUserId),
  });

  const interventionsQuery = useQuery({
    queryKey: ['teacher-interventions', classId, studentUserId],
    queryFn: ({ signal }) =>
      teacherInterventionService.list(
        { classId, studentUserId, pageNo: 1, pageSize: 20 },
        { signal }
      ),
    enabled: Number.isFinite(classId) && Number.isFinite(studentUserId),
  });

  const suggestMutation = useMutation({
    mutationFn: () =>
      aiService.suggestTeacherIntervention({
        classId,
        studentUserId,
        diagnosisSummaryId: detailQuery.data?.analysis.overview.latestSnapshot.lastDiagnosisSummaryId,
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['teacher-interventions', classId, studentUserId] });
    },
  });

  const saveInterventionMutation = useMutation({
    mutationFn: () => {
      if (!selectedInterventionId) {
        throw new Error('No intervention selected');
      }
      return teacherInterventionService.update(selectedInterventionId, {
        priority: form.priority,
        status: form.status,
        plannedAt: form.plannedAt || null,
        teacherNote: form.teacherNote,
      });
    },
    onSuccess: async (updated) => {
      setForm(buildInterventionForm(updated));
      await queryClient.invalidateQueries({ queryKey: ['teacher-interventions', classId, studentUserId] });
    },
  });

  const completeInterventionMutation = useMutation({
    mutationFn: () => {
      if (!selectedInterventionId) {
        throw new Error('No intervention selected');
      }
      return teacherInterventionService.update(selectedInterventionId, {
        priority: form.priority,
        status: 'COMPLETED',
        plannedAt: form.plannedAt || null,
        teacherNote: form.teacherNote,
      });
    },
    onSuccess: async (updated) => {
      setForm(buildInterventionForm(updated));
      await queryClient.invalidateQueries({ queryKey: ['teacher-interventions', classId, studentUserId] });
    },
  });

  const detail = detailQuery.data;
  const analysis = detail?.analysis;
  const studentInterventions = React.useMemo(() => interventionsQuery.data?.records || [], [interventionsQuery.data?.records]);
  const selectedIntervention =
    studentInterventions.find((item) => item.id === selectedInterventionId) || studentInterventions[0] || null;

  React.useEffect(() => {
    if (!studentInterventions.length) {
      setSelectedInterventionId(null);
      setForm(buildInterventionForm());
      return;
    }
    if (!selectedInterventionId || !studentInterventions.some((item) => item.id === selectedInterventionId)) {
      setSelectedInterventionId(studentInterventions[0].id);
    }
  }, [selectedInterventionId, studentInterventions]);

  React.useEffect(() => {
    if (!selectedIntervention) {
      return;
    }
    setForm(buildInterventionForm(selectedIntervention));
  }, [selectedIntervention]);

  return (
    <div className="space-y-10 pb-20">
      <PageHeader
        eyebrow={t('ui.sections.studentMetrics')}
        title={detail?.studentName || t('ui.pages.studentDetail.fallbackTitle')}
        subtitle={
          detail
            ? t('ui.meta.classRankPercentile', {
                rank: detail.classRank,
                percent: (detail.classPercentile * 100).toFixed(0),
              })
            : t('ui.labels.loadingStudentContext')
        }
        actions={
          <button
            type="button"
            onClick={() => suggestMutation.mutate()}
            disabled={suggestMutation.isPending}
            className="btn-liquid px-5 py-3 text-white flex items-center gap-2 disabled:opacity-60"
          >
            <Wand2 size={14} /> {t('ui.actions.generateIntervention')}
          </button>
        }
      />

      {detailQuery.error && (
        <div className="rounded-[2rem] border border-rose-500/20 bg-rose-500/5 p-6 text-rose-500">
          {detailQuery.error.message}
        </div>
      )}

      {analysis && (
        <>
          <div className="grid md:grid-cols-3 gap-6">
            <div className="rounded-[2rem] liquid-glass p-6">
              <SectionEyebrow className="text-xs">{t('ui.meta.correctRate')}</SectionEyebrow>
              <div className="mt-3 text-3xl font-black text-slate-900 dark:text-white">
                {formatMaybePercent(analysis.overview.latestSnapshot.recentAccuracy)}
              </div>
            </div>
            <div className="rounded-[2rem] liquid-glass p-6">
              <SectionEyebrow className="text-xs">{t('ui.meta.risk')}</SectionEyebrow>
              <div className="mt-3 text-3xl font-black text-rose-500">
                {formatMaybePercent(analysis.overview.latestSnapshot.recentNegativeTransferRisk)}
              </div>
            </div>
            <div className="rounded-[2rem] liquid-glass p-6">
              <SectionEyebrow className="text-xs">{t('ui.fields.averageReactionTime')}</SectionEyebrow>
              <div className="mt-3 text-3xl font-black text-slate-900 dark:text-white">
                {formatMs(analysis.overview.latestSnapshot.recentAvgReactionTimeMs)}
              </div>
            </div>
          </div>

          <div className="grid xl:grid-cols-[0.95fr_1.05fr] gap-8">
            <ChartCard title={t('ui.charts.studentRadar')} option={buildRadarOption(analysis.overview.radar)} loading={detailQuery.isLoading} isEmpty={!analysis.overview.radar.length} />
            <ChartCard title={t('ui.charts.trend7d')} option={buildTrendOption(analysis.trend7d)} loading={detailQuery.isLoading} isEmpty={!analysis.trend7d.series.length} />
          </div>

          <div className="grid xl:grid-cols-2 gap-8">
            <ChartCard title={t('ui.charts.transferHeatmap')} option={buildHeatmapOption(analysis.transferHeatmap)} loading={detailQuery.isLoading} isEmpty={!analysis.transferHeatmap.cells.length} />
            <ChartCard title={t('ui.charts.latencyAccuracyScatter')} option={buildScatterOption(analysis.scatter)} loading={detailQuery.isLoading} isEmpty={!analysis.scatter.points.length} />
          </div>

          <div className="grid xl:grid-cols-[1fr_1fr_0.9fr] gap-8">
            <section className="rounded-[2.5rem] liquid-glass-panel p-8">
              <SectionEyebrow className="mb-6">{t('ui.sections.highRiskPairs')}</SectionEyebrow>
              <div className="space-y-4">
                {analysis.highRiskPairs.map((item) => (
                  <div key={item.lexicalPairId} className="rounded-[1.6rem] border border-slate-200/70 dark:border-white/10 p-4 bg-white/60 dark:bg-white/5">
                    <div className="font-black text-slate-900 dark:text-white">{item.englishWord} / {item.frenchWord}</div>
                    <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                      {lexicalPairTypeLabel(item.lexicalPairType)} · {t('ui.meta.risk')} {formatMaybePercent(item.riskScore)}
                    </div>
                  </div>
                ))}
              </div>
            </section>

            <section className="rounded-[2.5rem] liquid-glass-panel p-8">
              <SectionEyebrow className="mb-6">{t('ui.sections.errorDistribution')}</SectionEyebrow>
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
                <SectionEyebrow>AI</SectionEyebrow>
              </div>
              {suggestMutation.isPending ? (
                <PanelSkeleton className="min-h-[220px] p-0" />
              ) : suggestMutation.data ? (
                <div className="space-y-4">
                  <p className="text-sm leading-7 text-slate-800 dark:text-white/85">{suggestMutation.data.explanation}</p>
                  <div className="rounded-[1.6rem] border border-slate-200/70 dark:border-white/10 p-4 bg-white/60 dark:bg-white/5">
                    <div className="font-bold text-slate-900 dark:text-white">建议备注</div>
                    <div className="mt-2 text-sm text-slate-500 dark:text-white/45">{suggestMutation.data.teacherNote}</div>
                  </div>
                </div>
              ) : suggestMutation.error ? (
                <div className="text-sm text-rose-500">{getApiErrorMessage(suggestMutation.error)}</div>
              ) : (
                <div className="text-sm text-slate-500 dark:text-white/45">点击右上角按钮生成一条新的 AI 干预建议。</div>
              )}
            </section>
          </div>

          <div className="grid gap-8 xl:grid-cols-[0.95fr_1.05fr]">
            <section className="rounded-[2.5rem] liquid-glass-panel p-8">
              <div className="flex items-center justify-between gap-4 mb-6">
                <SectionEyebrow>{t('ui.sections.interventionRecords')}</SectionEyebrow>
                <div className="text-sm text-slate-500 dark:text-white/45">{studentInterventions.length} 条</div>
              </div>

              {interventionsQuery.isLoading ? (
                <PanelSkeleton />
              ) : interventionsQuery.error ? (
                <div className="rounded-2xl border border-rose-500/20 bg-rose-500/5 px-4 py-3 text-sm text-rose-500">
                  {getApiErrorMessage(interventionsQuery.error)}
                </div>
              ) : !studentInterventions.length ? (
                <div className="rounded-2xl border border-slate-200 bg-white/70 px-4 py-8 text-sm text-slate-500 dark:border-white/10 dark:bg-white/5 dark:text-white/45">
                  当前没有该学生的干预记录。
                </div>
              ) : (
                <div className="space-y-4">
                  {studentInterventions.map((item) => (
                    <button
                      key={item.id}
                      type="button"
                      onClick={() => setSelectedInterventionId(item.id)}
                      className={`w-full rounded-[1.6rem] border p-4 text-left transition-all ${
                        selectedIntervention?.id === item.id
                          ? 'border-primary/30 bg-primary/5'
                          : 'border-slate-200/70 bg-white/60 dark:border-white/10 dark:bg-white/5'
                      }`}
                    >
                      <div className="flex items-start justify-between gap-4">
                        <div>
                          <div className="flex flex-wrap gap-2">
                            <StatusBadge label={interventionPriorityLabel(item.priority)} tone="warning" />
                            <StatusBadge label={interventionStatusLabel(item.status)} tone={interventionStatusTone(item.status)} />
                          </div>
                          <div className="mt-2 font-black text-slate-900 dark:text-white">{item.patternDetected}</div>
                          <div className="mt-2 text-sm text-slate-500 dark:text-white/45">{item.suggestedAction}</div>
                        </div>
                        <div className="text-right text-xs text-slate-500 dark:text-white/45">
                          <div>{formatDateTime(item.plannedAt)}</div>
                          <div className="mt-2">{formatDateTime(item.completedAt)}</div>
                        </div>
                      </div>
                    </button>
                  ))}
                </div>
              )}
            </section>

            <section className="rounded-[2.5rem] liquid-glass-panel p-8">
              <div className="flex items-center justify-between gap-4 mb-6">
                <SectionEyebrow>{t('ui.sections.interventionRecords')}</SectionEyebrow>
                {selectedIntervention && (
                  <div className="text-sm text-slate-500 dark:text-white/45">{t('ui.meta.lastUpdated', { time: formatDateTime(selectedIntervention.updatedAt) })}</div>
                )}
              </div>

              {!selectedIntervention ? (
                <div className="rounded-2xl border border-slate-200 bg-white/70 px-4 py-8 text-sm text-slate-500 dark:border-white/10 dark:bg-white/5 dark:text-white/45">
                  生成或选择一条干预记录后，即可在这里编辑排期、优先级和教师备注。
                </div>
              ) : (
                <div className="space-y-6">
                  <div className="rounded-[1.8rem] border border-slate-200/70 bg-white/60 p-5 dark:border-white/10 dark:bg-white/5">
                    <div className="text-sm font-bold text-slate-900 dark:text-white">{selectedIntervention.patternDetected}</div>
                    <div className="mt-2 text-sm leading-6 text-slate-500 dark:text-white/45">
                      {selectedIntervention.suggestedAction}
                    </div>
                  </div>

                  <div className="grid gap-4 md:grid-cols-3">
                    <label className="space-y-2 text-sm">
                      <span className="text-slate-500 dark:text-white/45">优先级</span>
                      <select
                        value={form.priority}
                        onChange={(event) => setForm((current) => ({ ...current, priority: event.target.value }))}
                        className="native-select w-full rounded-2xl border border-slate-200 bg-white/70 px-4 py-3 dark:border-white/10 dark:bg-white/5"
                      >
                        {PRIORITY_OPTIONS.map((item) => (
                          <option key={item} value={item}>
                            {item}
                          </option>
                        ))}
                      </select>
                    </label>

                    <label className="space-y-2 text-sm">
                      <span className="text-slate-500 dark:text-white/45">状态</span>
                      <select
                        value={form.status}
                        onChange={(event) => setForm((current) => ({ ...current, status: event.target.value }))}
                        className="native-select w-full rounded-2xl border border-slate-200 bg-white/70 px-4 py-3 dark:border-white/10 dark:bg-white/5"
                      >
                        {STATUS_OPTIONS.map((item) => (
                          <option key={item} value={item}>
                            {item}
                          </option>
                        ))}
                      </select>
                    </label>

                    <label className="space-y-2 text-sm">
                      <span className="text-slate-500 dark:text-white/45">计划时间</span>
                      <input
                        type="datetime-local"
                        value={form.plannedAt}
                        onChange={(event) => setForm((current) => ({ ...current, plannedAt: event.target.value }))}
                        className="w-full rounded-2xl border border-slate-200 bg-white/70 px-4 py-3 dark:border-white/10 dark:bg-white/5"
                      />
                    </label>
                  </div>

                  <label className="block space-y-2 text-sm">
                    <span className="text-slate-500 dark:text-white/45">教师备注</span>
                    <textarea
                      value={form.teacherNote}
                      onChange={(event) => setForm((current) => ({ ...current, teacherNote: event.target.value }))}
                      rows={6}
                      className="w-full rounded-[1.8rem] border border-slate-200 bg-white/70 px-4 py-3 dark:border-white/10 dark:bg-white/5"
                      placeholder="补充执行方式、课堂安排、跟进节点和完成标准。"
                    />
                  </label>

                  <div className="flex flex-wrap gap-3">
                    <button
                      type="button"
                      onClick={() => saveInterventionMutation.mutate()}
                      disabled={saveInterventionMutation.isPending}
                      className="btn-liquid px-5 py-3 text-white disabled:opacity-60"
                    >
                      保存计划
                    </button>
                    <button
                      type="button"
                      onClick={() => completeInterventionMutation.mutate()}
                      disabled={completeInterventionMutation.isPending}
                      className="rounded-full border border-slate-200 px-5 py-3 text-sm font-bold text-primary disabled:opacity-60 dark:border-white/10"
                    >
                      标记完成
                    </button>
                  </div>

                  {(saveInterventionMutation.error || completeInterventionMutation.error) && (
                    <div className="rounded-2xl border border-rose-500/20 bg-rose-500/5 px-4 py-3 text-sm text-rose-500">
                      {getApiErrorMessage(saveInterventionMutation.error || completeInterventionMutation.error)}
                    </div>
                  )}
                </div>
              )}
            </section>
          </div>
        </>
      )}
    </div>
  );
};

export default TeacherStudentDetailPage;
