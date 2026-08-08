import React from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Brain, Eye, EyeOff, RefreshCw, ShieldCheck, Wand2 } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useParams } from 'react-router-dom';
import { ChartCard } from '@/components/common/ChartCard';
import { DecisionCard, PageHeader, PanelSkeleton, SectionEyebrow, StatusBadge } from '@/components/common';
import InterventionEffectPanel from '@/features/interventions/InterventionEffectPanel';
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

function maskStudentName(name?: string | null): string {
  if (!name) return '学生';
  const chars = Array.from(name);
  return chars.length <= 1 ? '•' : `${chars[0]}${'•'.repeat(Math.min(chars.length - 1, 3))}`;
}

function errorStatus(error: unknown): number | null {
  return typeof error === 'object' && error !== null && 'status' in error && typeof error.status === 'number'
    ? error.status
    : null;
}

const TeacherStudentDetailPage: React.FC = () => {
  const { t } = useTranslation();
  const params = useParams();
  const classId = Number(params.classId);
  const studentUserId = Number(params.studentUserId);
  const queryClient = useQueryClient();
  const [selectedInterventionId, setSelectedInterventionId] = React.useState<number | null>(null);
  const [form, setForm] = React.useState<InterventionFormState>(buildInterventionForm());
  const [showIdentity, setShowIdentity] = React.useState(false);

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
    <div className="page-stack pb-16">
      <PageHeader
        eyebrow={t('ui.sections.studentMetrics')}
        title={detail ? (showIdentity ? detail.studentName : maskStudentName(detail.studentName)) : t('ui.pages.studentDetail.fallbackTitle')}
        subtitle={
          detail
            ? t('ui.meta.classRankPercentile', {
                rank: detail.classRank,
                percent: (detail.classPercentile * 100).toFixed(0),
              })
            : t('ui.labels.loadingStudentContext')
        }
        actions={
          <div className="flex min-w-0 flex-wrap items-center gap-2">
            {detail ? (
              <button
                type="button"
                onClick={() => setShowIdentity((current) => !current)}
                className="inline-flex items-center gap-2 rounded-xl border border-slate-200 px-3 py-2.5 text-xs font-bold text-slate-600 dark:border-white/10 dark:text-white/65"
                aria-pressed={showIdentity}
              >
                {showIdentity ? <EyeOff size={14} /> : <Eye size={14} />}
                {showIdentity ? '隐藏身份' : '显示身份'}
              </button>
            ) : null}
            <button
              type="button"
              onClick={() => suggestMutation.mutate()}
              disabled={suggestMutation.isPending || !analysis}
              className="btn-liquid px-5 py-3 text-white flex items-center gap-2 disabled:opacity-60"
            >
              <Wand2 size={14} /> {t('ui.actions.generateIntervention')}
            </button>
          </div>
        }
      />

      {detailQuery.error && (
        <div className="min-w-0 rounded-2xl border border-rose-500/20 bg-rose-500/5 p-4 text-rose-700 sm:p-6 dark:text-rose-300">
          <div className="flex min-w-0 items-start gap-3">
            <ShieldCheck size={18} className="mt-0.5 shrink-0" />
            <div className="min-w-0">
              <div className="font-black">{errorStatus(detailQuery.error) === 403 || errorStatus(detailQuery.error) === 404 ? '学生已不在当前班级' : '学生分析暂时不可用'}</div>
              <div className="mt-2 break-words text-sm leading-6">{getApiErrorMessage(detailQuery.error)}</div>
              <button type="button" onClick={() => void detailQuery.refetch()} className="mt-4 inline-flex items-center gap-2 rounded-full border border-rose-500/30 px-4 py-2 text-xs font-bold">
                <RefreshCw size={13} /> 重新加载
              </button>
            </div>
          </div>
        </div>
      )}

      {detailQuery.isLoading ? <PanelSkeleton /> : null}

      {!detailQuery.isLoading && !detailQuery.error && !analysis ? (
        <div className="min-w-0 rounded-2xl border border-dashed border-slate-300 bg-white/60 p-4 text-sm leading-6 text-slate-500 sm:p-6 md:p-8 dark:border-white/15 dark:bg-white/[0.03] dark:text-white/50">
          当前没有可用的诊断或训练证据。学生完成一次诊断后，这里会出现可追溯的薄弱点和干预建议。
        </div>
      ) : null}

      {analysis && (
        <>
          {(() => {
            const snapshot = analysis.overview.latestSnapshot;
            const topPair = snapshot.topRiskPairs[0] || analysis.highRiskPairs[0];
            const activeIntervention = studentInterventions.find((item) => item.status !== 'COMPLETED') || studentInterventions[0];
            return (
              <DecisionCard eyebrow="教师判断" title="先处理一个最重要的薄弱点">
                <div className="grid min-w-0 grid-cols-1 gap-5 lg:grid-cols-[minmax(0,1.1fr)_minmax(0,0.9fr)] lg:items-start">
                  <div className="min-w-0">
                    <div className="break-words text-lg font-black text-slate-900 dark:text-white">
                      {topPair ? `${topPair.englishWord} / ${topPair.frenchWord}` : '暂无明确薄弱点'}
                    </div>
                    <p className="mt-2 text-sm leading-6 text-slate-600 dark:text-white/65">
                      {topPair
                        ? `风险 ${formatMaybePercent(topPair.riskScore)}，${topPair.incorrectCount}/${topPair.attemptCount} 次答题出错。先用短练习验证这一词对，再决定是否扩大干预范围。`
                        : '当前样本不足以形成可靠判断，先让学生完成一次诊断或训练。'}
                    </p>
                  </div>
                  <div className="min-w-0 rounded-2xl border border-primary/20 bg-white/70 p-4 dark:bg-white/[0.04]">
                    <div className="text-xs font-bold uppercase tracking-[0.18em] text-slate-400">下一步</div>
                    <div className="mt-2 break-words text-sm font-bold text-slate-800 dark:text-white/85">
                      {activeIntervention?.suggestedAction || `建议模式：${snapshot.recommendedTrainingMode}`}
                    </div>
                    <div className="mt-3 text-xs text-slate-500 dark:text-white/45">
                      {activeIntervention ? `已有干预：${interventionStatusLabel(activeIntervention.status)}` : '尚未采取行动'}
                    </div>
                  </div>
                </div>
              </DecisionCard>
            );
          })()}

          <div className="grid min-w-0 grid-cols-1 gap-4 sm:grid-cols-3 sm:gap-6">
            <div className="min-w-0 rounded-2xl liquid-glass p-4 sm:p-6">
              <SectionEyebrow className="text-xs">{t('ui.meta.correctRate')}</SectionEyebrow>
              <div className="mt-3 text-3xl font-black text-slate-900 dark:text-white">
                {formatMaybePercent(analysis.overview.latestSnapshot.recentAccuracy)}
              </div>
            </div>
            <div className="min-w-0 rounded-2xl liquid-glass p-4 sm:p-6">
              <SectionEyebrow className="text-xs">{t('ui.meta.risk')}</SectionEyebrow>
              <div className="mt-3 text-3xl font-black text-rose-500">
                {formatMaybePercent(analysis.overview.latestSnapshot.recentNegativeTransferRisk)}
              </div>
            </div>
            <div className="min-w-0 rounded-2xl liquid-glass p-4 sm:p-6">
              <SectionEyebrow className="text-xs">{t('ui.fields.averageReactionTime')}</SectionEyebrow>
              <div className="mt-3 text-3xl font-black text-slate-900 dark:text-white">
                {formatMs(analysis.overview.latestSnapshot.recentAvgReactionTimeMs)}
              </div>
            </div>
          </div>

          <div className="content-grid-2">
            <ChartCard title={t('ui.charts.studentRadar')} option={buildRadarOption(analysis.overview.radar)} loading={detailQuery.isLoading} isEmpty={!analysis.overview.radar.length} />
            <ChartCard title={t('ui.charts.trend7d')} option={buildTrendOption(analysis.trend7d)} loading={detailQuery.isLoading} isEmpty={!analysis.trend7d.series.length} />
          </div>

          <div className="content-grid-2">
            <ChartCard title={t('ui.charts.transferHeatmap')} option={buildHeatmapOption(analysis.transferHeatmap)} loading={detailQuery.isLoading} isEmpty={!analysis.transferHeatmap.cells.length} />
            <ChartCard title={t('ui.charts.latencyAccuracyScatter')} option={buildScatterOption(analysis.scatter)} loading={detailQuery.isLoading} isEmpty={!analysis.scatter.points.length} />
          </div>

          <div className="grid min-w-0 grid-cols-1 gap-5 xl:grid-cols-3">
            <section className="min-w-0 rounded-2xl liquid-glass-panel p-4 sm:rounded-[2.5rem] sm:p-6 md:p-8">
              <SectionEyebrow className="mb-6">{t('ui.sections.highRiskPairs')}</SectionEyebrow>
              <div className="space-y-4">
                {analysis.highRiskPairs.length ? analysis.highRiskPairs.slice(0, 6).map((item) => (
                  <div key={item.lexicalPairId} className="min-w-0 rounded-[1.6rem] border border-slate-200/70 bg-white/60 p-4 dark:border-white/10 dark:bg-white/5">
                    <div className="break-words font-black text-slate-900 dark:text-white">{item.englishWord} / {item.frenchWord}</div>
                    <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                      {lexicalPairTypeLabel(item.lexicalPairType)} · {t('ui.meta.risk')} {formatMaybePercent(item.riskScore)}
                    </div>
                  </div>
                )) : <div className="rounded-2xl border border-dashed border-slate-300 p-5 text-sm text-slate-500 dark:border-white/15 dark:text-white/45">暂无足够的词对证据。</div>}
              </div>
            </section>

            <section className="min-w-0 rounded-2xl liquid-glass-panel p-4 sm:rounded-[2.5rem] sm:p-6 md:p-8">
              <SectionEyebrow className="mb-6">{t('ui.sections.errorDistribution')}</SectionEyebrow>
              <div className="space-y-4">
                {analysis.errorDistribution.length ? analysis.errorDistribution.slice(0, 6).map((item) => (
                  <div key={item.key} className="min-w-0 rounded-[1.6rem] border border-slate-200/70 bg-white/60 p-4 dark:border-white/10 dark:bg-white/5">
                    <div className="flex min-w-0 flex-col gap-1 sm:flex-row sm:items-center sm:justify-between sm:gap-4">
                      <span className="min-w-0 break-words font-bold text-slate-900 dark:text-white">{item.label}</span>
                      <span className="shrink-0 text-sm text-slate-500 dark:text-white/45">
                        {item.count} / {formatMaybePercent(item.ratio)}
                      </span>
                    </div>
                  </div>
                )) : <div className="rounded-2xl border border-dashed border-slate-300 p-5 text-sm text-slate-500 dark:border-white/15 dark:text-white/45">暂无错误分布数据。</div>}
              </div>
            </section>

            <section className="min-w-0 rounded-2xl liquid-glass-panel p-4 sm:rounded-[2.5rem] sm:p-6 md:p-8">
              <div className="mb-6 flex items-center gap-3">
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
                <div className="space-y-3 text-sm text-rose-500">
                  <div>{getApiErrorMessage(suggestMutation.error)}</div>
                  <button type="button" onClick={() => suggestMutation.mutate()} className="inline-flex items-center gap-2 rounded-full border border-rose-500/30 px-4 py-2 text-xs font-bold"><RefreshCw size={13} /> 重试建议</button>
                </div>
              ) : (
                <div className="text-sm text-slate-500 dark:text-white/45">点击右上角按钮生成一条新的 AI 干预建议。</div>
              )}
            </section>
          </div>

          <div className="content-grid-sidebar">
            <section className="min-w-0 rounded-2xl liquid-glass-panel p-4 sm:rounded-[2.5rem] sm:p-6 md:p-8 xl:max-h-[720px] xl:overflow-y-auto">
              <div className="mb-6 flex min-w-0 flex-col gap-2 sm:flex-row sm:items-center sm:justify-between sm:gap-4">
                <SectionEyebrow>{t('ui.sections.interventionRecords')}</SectionEyebrow>
                <div className="text-sm text-slate-500 dark:text-white/45">{studentInterventions.length} 条</div>
              </div>

              {interventionsQuery.isLoading ? (
                <PanelSkeleton />
              ) : interventionsQuery.error ? (
                <div className="rounded-2xl border border-rose-500/20 bg-rose-500/5 px-4 py-3 text-sm text-rose-500">
                  <div>{getApiErrorMessage(interventionsQuery.error)}</div>
                  <button type="button" onClick={() => void interventionsQuery.refetch()} className="mt-3 inline-flex items-center gap-2 rounded-full border border-rose-500/30 px-4 py-2 text-xs font-bold"><RefreshCw size={13} /> 重试加载</button>
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
                      <div className="flex min-w-0 flex-col gap-3 sm:flex-row sm:items-start sm:justify-between sm:gap-4">
                        <div className="min-w-0">
                          <div className="flex flex-wrap gap-2">
                            <StatusBadge label={interventionPriorityLabel(item.priority)} tone="warning" />
                            <StatusBadge label={interventionStatusLabel(item.status)} tone={interventionStatusTone(item.status)} />
                          </div>
                          <div className="mt-2 break-words font-black text-slate-900 dark:text-white">{item.patternDetected}</div>
                          <div className="mt-2 break-words text-sm text-slate-500 dark:text-white/45">{item.suggestedAction}</div>
                        </div>
                        <div className="shrink-0 text-left text-xs text-slate-500 sm:text-right dark:text-white/45">
                          <div>{formatDateTime(item.plannedAt)}</div>
                          <div className="mt-2">{formatDateTime(item.completedAt)}</div>
                        </div>
                      </div>
                    </button>
                  ))}
                </div>
              )}
            </section>

            <section className="min-w-0 rounded-2xl liquid-glass-panel p-4 sm:rounded-[2.5rem] sm:p-6 md:p-8 xl:sticky xl:top-6 xl:self-start">
              <div className="mb-6 flex min-w-0 flex-col gap-2 sm:flex-row sm:items-center sm:justify-between sm:gap-4">
                <SectionEyebrow>{t('ui.sections.interventionRecords')}</SectionEyebrow>
                {selectedIntervention && (
                  <div className="min-w-0 break-words text-sm text-slate-500 dark:text-white/45">{t('ui.meta.lastUpdated', { time: formatDateTime(selectedIntervention.updatedAt) })}</div>
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

                  <InterventionEffectPanel effectTracking={selectedIntervention.effectTracking} />

                  <div className="grid min-w-0 grid-cols-1 gap-4 md:grid-cols-3">
                    <label className="min-w-0 space-y-2 text-sm">
                      <span className="text-slate-500 dark:text-white/45">优先级</span>
                      <select
                        value={form.priority}
                        onChange={(event) => setForm((current) => ({ ...current, priority: event.target.value }))}
                        className="native-select w-full min-w-0 rounded-2xl border border-slate-200 bg-white/70 px-4 py-3 dark:border-white/10 dark:bg-white/5"
                      >
                        {PRIORITY_OPTIONS.map((item) => (
                          <option key={item} value={item}>
                            {item}
                          </option>
                        ))}
                      </select>
                    </label>

                    <label className="min-w-0 space-y-2 text-sm">
                      <span className="text-slate-500 dark:text-white/45">状态</span>
                      <select
                        value={form.status}
                        onChange={(event) => setForm((current) => ({ ...current, status: event.target.value }))}
                        className="native-select w-full min-w-0 rounded-2xl border border-slate-200 bg-white/70 px-4 py-3 dark:border-white/10 dark:bg-white/5"
                      >
                        {STATUS_OPTIONS.map((item) => (
                          <option key={item} value={item}>
                            {item}
                          </option>
                        ))}
                      </select>
                    </label>

                    <label className="min-w-0 space-y-2 text-sm">
                      <span className="text-slate-500 dark:text-white/45">计划时间</span>
                      <input
                        type="datetime-local"
                        value={form.plannedAt}
                        onChange={(event) => setForm((current) => ({ ...current, plannedAt: event.target.value }))}
                        className="w-full min-w-0 rounded-2xl border border-slate-200 bg-white/70 px-4 py-3 dark:border-white/10 dark:bg-white/5"
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
                      <div>{getApiErrorMessage(saveInterventionMutation.error || completeInterventionMutation.error)}</div>
                      <button type="button" onClick={() => (saveInterventionMutation.error ? saveInterventionMutation.mutate() : completeInterventionMutation.mutate())} className="mt-3 inline-flex items-center gap-2 rounded-full border border-rose-500/30 px-4 py-2 text-xs font-bold"><RefreshCw size={13} /> 重试保存</button>
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
