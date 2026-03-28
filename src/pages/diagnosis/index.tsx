import React from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { Brain, CheckCircle2, ChevronRight, Timer } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { PageHeader, PanelSkeleton } from '@/components/common';
import { EChart } from '@/components/common/EChart';
import { getApiErrorMessage } from '@/lib/api';
import { aiService, diagnosisSessionService, diagnosisTemplateService, trainingService } from '@/lib/services';
import { buildRadarOption, formatDateTime, formatMaybePercent, formatMs, lexicalPairTypeLabel } from '@/lib/format';
import type { AnalyticsRadarMetricVO, DiagnosisOptionViewVO, DiagnosisRadarMetric } from '@/lib/contracts';
import { SessionFeedbackBanners, SessionOptionButton, SessionProgressHeader, SessionSaveActions } from '@/features/session-runtime/components';
import { buildSessionSnapshot } from '@/features/session-runtime/helpers';
import { useSessionRuntime } from '@/features/session-runtime/useSessionRuntime';
import { diagnosisFlowReducer, initialDiagnosisFlowState } from './flow';

const DIAGNOSIS_RADAR_MAX = 1;

export function toDiagnosisRadarChartMetrics(
  radarMetrics?: DiagnosisRadarMetric[] | null
): AnalyticsRadarMetricVO[] {
  return (radarMetrics || []).map((metric) => ({
    key: metric.code,
    label: metric.label,
    value: metric.value,
    max: DIAGNOSIS_RADAR_MAX,
  }));
}

const DiagnosisPage: React.FC = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [state, dispatch] = React.useReducer(diagnosisFlowReducer, initialDiagnosisFlowState);
  const shownAtRef = React.useRef<number>(Date.now());

  const historyQuery = useQuery({
    queryKey: ['diagnosis-history', 'in-progress'],
    queryFn: ({ signal }) =>
      diagnosisSessionService.listHistory({ pageNo: 1, pageSize: 1, status: 'IN_PROGRESS' }, { signal }),
  });

  React.useEffect(() => {
    if (!historyQuery.data) {
      return;
    }
    const inProgress = historyQuery.data.records[0];
    if (inProgress?.sessionId) {
      shownAtRef.current = Date.now();
      dispatch({ type: 'resumeSession', sessionId: inProgress.sessionId });
      return;
    }
    dispatch({ type: 'readyToSelect' });
  }, [historyQuery.data]);

  React.useEffect(() => {
    if (historyQuery.error) {
      dispatch({ type: 'readyToSelect' });
    }
  }, [historyQuery.error]);

  const templatesQuery = useQuery({
    queryKey: ['student-diagnosis-templates'],
    queryFn: ({ signal }) => diagnosisTemplateService.listPublished({ pageNo: 1, pageSize: 20 }, { signal }),
    enabled: state.phase === 'select',
  });

  const createSessionMutation = useMutation({
    mutationFn: (templateId: number) => diagnosisSessionService.create(templateId),
    onSuccess: (created) => {
      shownAtRef.current = Date.now();
      dispatch({ type: 'startSession', sessionId: created.sessionId });
      void queryClient.invalidateQueries({ queryKey: ['diagnosis-history'] });
    },
  });

  const nextItemQuery = useQuery({
    queryKey: ['diagnosis-next-item', state.sessionId],
    queryFn: ({ signal }) => diagnosisSessionService.getNextItem(state.sessionId as number, { signal }),
    enabled: state.phase === 'running' && !!state.sessionId,
  });

  const markCompleted = React.useCallback(() => {
    dispatch({ type: 'showResult' });
    void queryClient.invalidateQueries({ queryKey: ['diagnosis-history'] });
    void queryClient.invalidateQueries({ queryKey: ['student-overview'] });
    void queryClient.invalidateQueries({ queryKey: ['recommended-training-plan'] });
  }, [queryClient]);

  const submitAnswerMutation = useMutation({
    mutationFn: (payload: {
      itemResultId: number;
      selectedSemanticMatch?: boolean;
      selectedAnswerKey?: string;
      reactionTimeMs: number;
      hesitationTimeMs: number;
    }) => diagnosisSessionService.submitAnswer(state.sessionId as number, payload),
    onSuccess: async (progress) => {
      if (progress.completed) {
        markCompleted();
        return;
      }
      shownAtRef.current = Date.now();
      await nextItemQuery.refetch();
    },
  });

  const resultQuery = useQuery({
    queryKey: ['diagnosis-result', state.sessionId],
    queryFn: ({ signal }) => diagnosisSessionService.getResult(state.sessionId as number, { signal }),
    enabled: state.phase === 'result' && !!state.sessionId,
  });

  const explanationQuery = useQuery({
    queryKey: ['diagnosis-explanation', state.sessionId],
    queryFn: ({ signal }) => aiService.explainDiagnosis(undefined, { signal }),
    enabled: state.phase === 'result' && !!state.sessionId,
    retry: false,
  });

  const recommendedPlanQuery = useQuery({
    queryKey: ['recommended-training-plan'],
    queryFn: ({ signal }) => trainingService.getRecommendedPlan({ signal }),
    enabled: state.phase === 'result' && !!state.sessionId,
    retry: false,
  });

  React.useEffect(() => {
    if (state.phase !== 'running' || !nextItemQuery.data || nextItemQuery.data.hasNextItem) {
      return;
    }
    if (nextItemQuery.data.sessionStatus === 'COMPLETED') {
      markCompleted();
    }
  }, [markCompleted, nextItemQuery.data, state.phase]);

  const runtime = useSessionRuntime({
    active: state.phase === 'running',
    sessionId: state.sessionId,
    nextItem: nextItemQuery.data,
    refetchCurrent: nextItemQuery.refetch,
    buildSnapshot: (sessionId, nextItem) => buildSessionSnapshot(sessionId, nextItem),
    saveProgress: diagnosisSessionService.saveProgress,
    saveProgressKeepalive: diagnosisSessionService.saveProgressKeepalive,
    isCompleted: (nextItem) => nextItem?.sessionStatus === 'COMPLETED',
    onCompleted: () => markCompleted(),
  });

  const currentItem = nextItemQuery.data?.item;
  const isAnswerLocked = submitAnswerMutation.isPending || nextItemQuery.isFetching;

  React.useEffect(() => {
    if (!currentItem) {
      return;
    }
    runtime.resetFeedback();
  }, [currentItem?.itemResultId]);

  const submitAnswer = async (option: DiagnosisOptionViewVO) => {
    if (!currentItem) {
      return;
    }
    const reactionTimeMs = Math.max(1, Date.now() - shownAtRef.current);
    const hesitationTimeMs = Math.max(0, reactionTimeMs - 1200);
    shownAtRef.current = Date.now();

    if (currentItem.taskType === 'REACTION_TIME') {
      await submitAnswerMutation.mutateAsync({
        itemResultId: currentItem.itemResultId,
        selectedAnswerKey: option.key,
        selectedSemanticMatch: option.semanticMatch ?? undefined,
        reactionTimeMs,
        hesitationTimeMs,
      });
      return;
    }

    await submitAnswerMutation.mutateAsync({
      itemResultId: currentItem.itemResultId,
      selectedAnswerKey: option.key,
      reactionTimeMs,
      hesitationTimeMs,
    });
  };

  if (state.phase === 'boot' || historyQuery.isLoading) {
    return (
      <div className="mx-auto max-w-5xl">
        <PanelSkeleton className="min-h-[360px]" />
      </div>
    );
  }

  if (state.phase === 'select') {
    return (
      <div className="space-y-8">
        <PageHeader title={t('diagnosis.selectTitle')} subtitle={t('diagnosis.selectSubtitle')} />
        <section className="liquid-glass-panel rounded-[3rem] p-10 edge-light">
          <div className="max-w-3xl">
            <div className="inline-flex rounded-3xl border border-primary/20 bg-primary/10 p-4">
              <Brain size={32} className="text-primary" />
            </div>
            <h2 className="mt-6 text-4xl font-black text-slate-900 dark:text-white">
              {t('diagnosis.startTitle')}
            </h2>
            <p className="mt-4 leading-7 text-slate-500 dark:text-white/50">
              {t('diagnosis.startDescription')}
            </p>
          </div>
        </section>

        {templatesQuery.error && (
          <div className="rounded-[2rem] border border-rose-500/20 bg-rose-500/5 p-6 text-rose-500">
            {getApiErrorMessage(templatesQuery.error)}
          </div>
        )}

        {createSessionMutation.error && (
          <div className="rounded-[2rem] border border-rose-500/20 bg-rose-500/5 p-6 text-rose-500">
            {getApiErrorMessage(createSessionMutation.error)}
          </div>
        )}

        {templatesQuery.isLoading ? (
          <PanelSkeleton />
        ) : !templatesQuery.data?.records.length ? (
          <div className="rounded-[2rem] border border-slate-200 p-8 text-slate-500 dark:border-white/10 dark:text-white/45">
            {t('diagnosis.noTemplates')}
          </div>
        ) : (
          <div className="grid gap-6 lg:grid-cols-2">
            {templatesQuery.data.records.map((template) => (
              <button
                key={template.id}
                type="button"
                disabled={createSessionMutation.isPending}
                onClick={() => createSessionMutation.mutate(template.id)}
                className="text-left liquid-glass rounded-[2.4rem] p-7 edge-light transition-all hover:border-primary/40 disabled:opacity-60"
              >
                <div className="flex items-start justify-between gap-4">
                  <div>
                    <div className="text-xs uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">
                      {template.status}
                    </div>
                    <div className="mt-3 text-2xl font-black text-slate-900 dark:text-white">
                      {template.templateName}
                    </div>
                    <div className="mt-3 text-sm leading-6 text-slate-500 dark:text-white/45">
                      {template.description || t('diagnosis.noDescription')}
                    </div>
                  </div>
                  <ChevronRight className="shrink-0 text-primary" />
                </div>
                <div className="mt-6 flex gap-4 text-sm text-slate-500 dark:text-white/45">
                  <span>{t('diagnosis.statusTemplateCount', { count: template.itemCount })}</span>
                  <span>{t('diagnosis.statusDurationMinutes', { count: template.estimatedDurationMinutes })}</span>
                </div>
              </button>
            ))}
          </div>
        )}
      </div>
    );
  }

  if (state.phase === 'running') {
    return (
      <div className="mx-auto max-w-5xl space-y-8">
        <PageHeader
          title={t('diagnosis.runningTitle')}
          subtitle={
            state.sessionId
              ? t('diagnosis.runningSessionSubtitle', { sessionId: state.sessionId })
              : t('diagnosis.runningSubtitle')
          }
          actions={
            <SessionSaveActions
              isBusy={runtime.isSaving || isAnswerLocked}
              onSave={() => {
                void runtime.saveProgressManually();
              }}
              onSaveAndExit={() => {
                void runtime.saveProgressManually({
                  exitAfterSave: true,
                  onSuccess: () => navigate('/history'),
                });
              }}
            />
          }
        />

        <SessionFeedbackBanners
          saveMessage={runtime.saveMessage}
          saveErrorMessage={runtime.saveErrorMessage}
          loadError={nextItemQuery.error}
          onRetryLoad={() => void nextItemQuery.refetch()}
        />

        {nextItemQuery.isLoading ? (
          <PanelSkeleton className="min-h-[360px]" />
        ) : !currentItem ? (
          <div className="rounded-[2rem] border border-slate-200 bg-white/70 px-6 py-8 text-sm text-slate-500 dark:border-white/10 dark:bg-white/5 dark:text-white/45">
            正在收尾诊断结果，请稍候...
          </div>
        ) : (
          <>
            <SessionProgressHeader
              icon={<Timer size={16} />}
              label={t('diagnosis.progress', {
                current: nextItemQuery.data?.currentItemOrder || 0,
                total: nextItemQuery.data?.totalItems || 0,
              })}
              answeredItems={nextItemQuery.data?.answeredItems}
              totalItems={nextItemQuery.data?.totalItems}
              gradientClassName="bg-gradient-to-r from-sky-500 to-blue-500"
            />

            <section className="rounded-[3rem] liquid-glass-panel p-10 edge-light">
              <div className="text-xs uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">
                {currentItem.taskType} · {lexicalPairTypeLabel(currentItem.lexicalPairType)}
              </div>
              <div className="mt-8 grid items-start gap-6 md:grid-cols-2">
                <div className="rounded-[2rem] border border-slate-200/80 bg-white/60 p-8 dark:border-white/10 dark:bg-white/5">
                  <div className="mb-3 text-sm uppercase tracking-[0.24em] text-sky-500">
                    {t('diagnosis.english')}
                  </div>
                  <div className="text-4xl font-black text-slate-900 dark:text-white">
                    {currentItem.englishWord}
                  </div>
                </div>
                <div className="rounded-[2rem] border border-slate-200/80 bg-white/60 p-8 dark:border-white/10 dark:bg-white/5">
                  <div className="mb-3 text-sm uppercase tracking-[0.24em] text-rose-500">
                    {t('diagnosis.french')}
                  </div>
                  <div className="text-4xl font-black text-slate-900 dark:text-white">
                    {currentItem.frenchWord}
                  </div>
                </div>
              </div>

              {(currentItem.stimulus.promptText ||
                currentItem.stimulus.instruction ||
                currentItem.stimulus.contextSentence) && (
                <div className="mt-8 rounded-[2rem] border border-dashed border-slate-300 bg-white/40 p-6 dark:border-white/10 dark:bg-white/5">
                  {currentItem.stimulus.instruction && (
                    <div className="text-sm font-bold text-slate-700 dark:text-white/75">
                      {currentItem.stimulus.instruction}
                    </div>
                  )}
                  {currentItem.stimulus.promptText && (
                    <div className="mt-3 text-lg text-slate-800 dark:text-white/85">
                      {currentItem.stimulus.promptText}
                    </div>
                  )}
                  {currentItem.stimulus.contextSentence && (
                    <div className="mt-3 italic text-slate-500 dark:text-white/45">
                      {currentItem.stimulus.contextSentence}
                    </div>
                  )}
                </div>
              )}

              <div className="mt-8 grid gap-4">
                {currentItem.options.map((option) => (
                  <SessionOptionButton
                    key={option.key}
                    disabled={isAnswerLocked}
                    onClick={() => void submitAnswer(option)}
                    label={option.label}
                    icon={<ChevronRight className="text-primary" size={16} />}
                  />
                ))}
              </div>

              {isAnswerLocked && (
                <div className="mt-6 text-sm text-slate-500 dark:text-white/45">系统正在提交答案并加载下一题，请稍候。</div>
              )}
            </section>
          </>
        )}
      </div>
    );
  }

  const result = resultQuery.data;
  const radarOption = buildRadarOption(toDiagnosisRadarChartMetrics(result?.chartPayload.radarMetrics));

  return (
    <div className="space-y-8 pb-20">
      <PageHeader
        title={t('diagnosis.resultTitle')}
        subtitle={
          result
            ? t('diagnosis.resultLoadedSubtitle', {
                templateName: result.templateName,
                completedAt: formatDateTime(result.completedAt),
              })
            : t('diagnosis.resultSubtitle')
        }
      />

      {resultQuery.error && (
        <div className="rounded-[2rem] border border-rose-500/20 bg-rose-500/5 p-6 text-rose-500">
          {getApiErrorMessage(resultQuery.error)}
        </div>
      )}

      {!result && resultQuery.isLoading ? (
        <div className="grid gap-8">
          <PanelSkeleton />
          <PanelSkeleton />
        </div>
      ) : null}

      {result && (
        <>
          <section className="liquid-glass-panel rounded-[3rem] p-10 edge-light">
            <div className="flex flex-col items-start justify-between gap-8 lg:flex-row">
              <div>
                <div className="inline-flex items-center gap-3 rounded-full border border-emerald-500/20 bg-emerald-500/10 px-4 py-2 text-xs uppercase tracking-[0.24em] text-emerald-500">
                  <CheckCircle2 size={14} />
                  {t('diagnosis.completedBadge')}
                </div>
                <h2 className="mt-5 text-4xl font-black text-slate-900 dark:text-white">
                  {t('diagnosis.completedTitle')}
                </h2>
                <p className="mt-4 leading-7 text-slate-500 dark:text-white/45">
                  {t('diagnosis.completedDescription')}
                </p>
              </div>
                  <div className="flex flex-wrap gap-3">
                    <button
                      type="button"
                      onClick={() => {
                        const recommendedMode =
                          recommendedPlanQuery.data?.suggestedSessions[0]?.mode || recommendedPlanQuery.data?.priorityMode;
                        if (recommendedMode) {
                          navigate(`/training?mode=${encodeURIComponent(recommendedMode)}&source=diagnosis-result`);
                          return;
                        }
                        navigate('/training');
                      }}
                      className="btn-liquid px-6 py-3 text-white"
                    >
                      开始推荐训练
                    </button>
                    <button
                      type="button"
                      onClick={() => dispatch({ type: 'reset' })}
                      className="rounded-full border border-slate-200 px-6 py-3 text-sm font-bold dark:border-white/10"
                    >
                      {t('diagnosis.restart')}
                    </button>
                  </div>
                </div>
              </section>

          <div className="grid gap-6 md:grid-cols-2 xl:grid-cols-4">
            <div className="rounded-[2rem] liquid-glass p-6">
              <div className="text-xs uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">
                {t('diagnosis.metrics.positiveTransferScore')}
              </div>
              <div className="mt-3 text-3xl font-black text-slate-900 dark:text-white">
                {formatMaybePercent(result.metrics.positiveTransferScore)}
              </div>
            </div>
            <div className="rounded-[2rem] liquid-glass p-6">
              <div className="text-xs uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">
                {t('diagnosis.metrics.negativeTransferRisk')}
              </div>
              <div className="mt-3 text-3xl font-black text-rose-500">
                {formatMaybePercent(result.metrics.negativeTransferRisk)}
              </div>
            </div>
            <div className="rounded-[2rem] liquid-glass p-6">
              <div className="text-xs uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">
                {t('diagnosis.metrics.semanticDiscrimination')}
              </div>
              <div className="mt-3 text-3xl font-black text-slate-900 dark:text-white">
                {formatMaybePercent(result.metrics.semanticDiscrimination)}
              </div>
            </div>
            <div className="rounded-[2rem] liquid-glass p-6">
              <div className="text-xs uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">
                {t('diagnosis.metrics.averageReactionTime')}
              </div>
              <div className="mt-3 text-3xl font-black text-slate-900 dark:text-white">
                {formatMs(result.metrics.averageReactionTime)}
              </div>
            </div>
          </div>

          <div className="grid gap-8 xl:grid-cols-[1.1fr_0.9fr]">
            <section className="rounded-[2.5rem] liquid-glass-panel p-8">
              <div className="mb-4 text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">
                {t('diagnosis.radarProfile')}
              </div>
              <div className="h-[360px]">
                <EChart option={radarOption} />
              </div>
            </section>
            <section className="rounded-[2.5rem] liquid-glass-panel p-8">
              <div className="mb-4 text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">
                {t('diagnosis.aiExplain')}
              </div>
              {explanationQuery.isLoading ? (
                <PanelSkeleton className="min-h-[280px] p-0" />
              ) : explanationQuery.data ? (
                <div className="space-y-4">
                  <p className="text-base leading-7 text-slate-800 dark:text-white/85">
                    {explanationQuery.data.explanation}
                  </p>
                  <div className="rounded-[1.6rem] border border-slate-200/70 bg-white/60 p-4 dark:border-white/10 dark:bg-white/5">
                    <div className="text-sm font-bold text-slate-900 dark:text-white">
                      {t('diagnosis.teacherNote')}
                    </div>
                    <div className="mt-2 text-sm leading-6 text-slate-500 dark:text-white/45">
                      {explanationQuery.data.teacherNote}
                    </div>
                  </div>
                </div>
              ) : explanationQuery.error ? (
                <div className="text-sm text-rose-500">{getApiErrorMessage(explanationQuery.error)}</div>
              ) : (
                <div className="text-sm text-slate-500 dark:text-white/45">{t('diagnosis.noExplanation')}</div>
              )}
            </section>
          </div>

          <div className="grid gap-8 xl:grid-cols-2">
            <section className="rounded-[2.5rem] liquid-glass-panel p-8">
              <div className="mb-4 text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">
                {t('diagnosis.highRiskPairs')}
              </div>
              <div className="space-y-4">
                {result.highRiskLexicalPairs.map((item) => (
                  <div
                    key={item.lexicalPairId}
                    className="rounded-[1.6rem] border border-slate-200/70 bg-white/60 p-4 dark:border-white/10 dark:bg-white/5"
                  >
                    <div className="flex items-center justify-between gap-4">
                      <div>
                        <div className="font-black text-slate-900 dark:text-white">
                          {item.englishWord} / {item.frenchWord}
                        </div>
                        <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                          {lexicalPairTypeLabel(item.lexicalPairType)} · {item.dominantErrorType}
                        </div>
                      </div>
                      <div className="text-right">
                        <div className="font-black text-rose-500">{formatMaybePercent(item.riskScore)}</div>
                        <div className="text-xs text-slate-400 dark:text-white/30">
                          {formatMs(item.averageReactionTime)}
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </section>

            <section className="rounded-[2.5rem] liquid-glass-panel p-8">
              <div className="mb-4 text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">
                {t('diagnosis.answerBreakdown')}
              </div>
              <div className="space-y-4">
                {result.items.map((item) => (
                  <div
                    key={item.itemResultId}
                    className="rounded-[1.6rem] border border-slate-200/70 bg-white/60 p-4 dark:border-white/10 dark:bg-white/5"
                  >
                    <div className="flex items-start justify-between gap-4">
                      <div>
                        <div className="font-black text-slate-900 dark:text-white">
                          {item.englishWord} / {item.frenchWord}
                        </div>
                        <div className="mt-2 text-sm leading-6 text-slate-500 dark:text-white/45">
                          {item.taskType} · {item.detectedErrorType} · {formatMaybePercent(item.transferRiskScore)}
                        </div>
                      </div>
                      <div className="text-right text-sm text-slate-500 dark:text-white/45">
                        <div>{item.correct ? t('diagnosis.correct') : t('diagnosis.incorrect')}</div>
                        <div>{formatMs(item.reactionTimeMs)}</div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </section>
          </div>
        </>
      )}
    </div>
  );
};

export default DiagnosisPage;
