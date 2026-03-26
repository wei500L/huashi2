import React from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { AlertTriangle, Award, Brain, Clock3, Rocket } from 'lucide-react';
import { PageHeader, PanelSkeleton } from '@/components/common';
import { aiService, trainingService } from '@/lib/services';
import { formatDateTime, formatMaybePercent, formatMs, lexicalPairTypeLabel } from '@/lib/format';
import { getApiErrorMessage, normalizeApiError } from '@/lib/api';
import type { TrainingOptionViewVO } from '@/lib/contracts';
import { initialTrainingFlowState, trainingFlowReducer } from './flow';

const TrainingPage: React.FC = () => {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const [state, dispatch] = React.useReducer(trainingFlowReducer, initialTrainingFlowState);
  const shownAtRef = React.useRef<number>(Date.now());

  const historyQuery = useQuery({
    queryKey: ['training-history', 'in-progress'],
    queryFn: ({ signal }) =>
      trainingService.listHistory({ pageNo: 1, pageSize: 1, status: 'IN_PROGRESS' }, { signal }),
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
    dispatch({ type: 'readyHome' });
  }, [historyQuery.data]);

  React.useEffect(() => {
    if (historyQuery.error) {
      dispatch({ type: 'readyHome' });
    }
  }, [historyQuery.error]);

  const recommendedPlanQuery = useQuery({
    queryKey: ['recommended-training-plan'],
    queryFn: ({ signal }) => trainingService.getRecommendedPlan({ signal }),
    enabled: state.phase === 'home',
    retry: false,
  });

  const aiRecommendationQuery = useQuery({
    queryKey: ['ai-recommend-training', recommendedPlanQuery.data?.sourceDiagnosisSummaryId],
    queryFn: ({ signal }) =>
      aiService.recommendTraining(recommendedPlanQuery.data?.sourceDiagnosisSummaryId, { signal }),
    enabled: state.phase === 'home' && !!recommendedPlanQuery.data,
    retry: false,
  });

  const wrongBookQuery = useQuery({
    queryKey: ['wrong-book'],
    queryFn: ({ signal }) => trainingService.getWrongBook({ signal }),
  });

  const reviewScheduleQuery = useQuery({
    queryKey: ['review-schedule', true],
    queryFn: ({ signal }) => trainingService.getReviewSchedule(true, { signal }),
  });

  const startMutation = useMutation({
    mutationFn: (mode: string) =>
      trainingService.startSession({
        planId: recommendedPlanQuery.data!.planId,
        mode,
      }),
    onSuccess: (created) => {
      shownAtRef.current = Date.now();
      dispatch({ type: 'startSession', sessionId: created.sessionId });
      void queryClient.invalidateQueries({ queryKey: ['training-history'] });
    },
  });

  const nextItemQuery = useQuery({
    queryKey: ['training-next-item', state.sessionId],
    queryFn: ({ signal }) => trainingService.getNextItem(state.sessionId as number, { signal }),
    enabled: state.phase === 'running' && !!state.sessionId,
  });

  const markCompleted = React.useCallback((sessionId: number) => {
    dispatch({ type: 'showSummary', sessionId });
    void queryClient.invalidateQueries({ queryKey: ['training-history'] });
    void queryClient.invalidateQueries({ queryKey: ['student-overview'] });
    void queryClient.invalidateQueries({ queryKey: ['student-trends'] });
    void queryClient.invalidateQueries({ queryKey: ['wrong-book'] });
    void queryClient.invalidateQueries({ queryKey: ['review-schedule'] });
  }, [queryClient]);

  const saveProgressSnapshot = React.useCallback(async () => {
    if (!state.sessionId || state.phase !== 'running') {
      return;
    }
    const sessionId = state.sessionId;
    const snapshot = nextItemQuery.data
      ? {
          sessionId,
          currentItemOrder: nextItemQuery.data.currentItemOrder,
          answeredItems: nextItemQuery.data.answeredItems,
          timestamp: new Date().toISOString(),
        }
      : { sessionId, timestamp: new Date().toISOString() };

    try {
      await trainingService.saveProgressKeepalive(sessionId, snapshot);
    } catch (error) {
      const normalizedError = normalizeApiError(error);
      if (normalizedError.status !== 409) {
        return;
      }
      const refreshed = await nextItemQuery.refetch();
      if (refreshed.data?.sessionStatus === 'COMPLETED') {
        markCompleted(refreshed.data.sessionId);
      }
    }
  }, [markCompleted, nextItemQuery, state.phase, state.sessionId]);

  const answerMutation = useMutation({
    mutationFn: (payload: {
      itemResultId: number;
      selectedAnswerKey: string;
      reactionTimeMs: number;
      hesitationTimeMs: number;
    }) => trainingService.submitAnswer(state.sessionId as number, payload),
    onSuccess: async (progress) => {
      if (progress.completed) {
        markCompleted(progress.sessionId);
        return;
      }
      shownAtRef.current = Date.now();
      await nextItemQuery.refetch();
    },
  });

  const summaryQuery = useQuery({
    queryKey: ['training-summary', state.summarySessionId],
    queryFn: ({ signal }) => trainingService.getSummary(state.summarySessionId as number, { signal }),
    enabled: state.phase === 'summary' && !!state.summarySessionId,
  });

  React.useEffect(() => {
    if (state.phase !== 'running' || !nextItemQuery.data || nextItemQuery.data.hasNextItem) {
      return;
    }
    if (nextItemQuery.data.sessionStatus === 'COMPLETED') {
      markCompleted(nextItemQuery.data.sessionId);
    }
  }, [markCompleted, nextItemQuery.data, state.phase]);

  React.useEffect(() => {
    if (!state.sessionId || state.phase !== 'running') {
      return;
    }
    const onVisibilityChange = () => {
      if (document.hidden) {
        void saveProgressSnapshot();
      }
    };
    const onBeforeUnload = () => {
      void saveProgressSnapshot();
    };
    window.addEventListener('beforeunload', onBeforeUnload);
    document.addEventListener('visibilitychange', onVisibilityChange);
    return () => {
      window.removeEventListener('beforeunload', onBeforeUnload);
      document.removeEventListener('visibilitychange', onVisibilityChange);
    };
  }, [saveProgressSnapshot, state.phase, state.sessionId]);

  const planError = recommendedPlanQuery.error ? normalizeApiError(recommendedPlanQuery.error) : null;
  const currentItem = nextItemQuery.data?.item;

  const submitAnswer = async (option: TrainingOptionViewVO) => {
    if (!currentItem) {
      return;
    }
    const reactionTimeMs = Math.max(1, Date.now() - shownAtRef.current);
    const hesitationTimeMs = Math.max(0, reactionTimeMs - 1200);
    shownAtRef.current = Date.now();
    await answerMutation.mutateAsync({
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

  if (state.phase === 'running') {
    return (
      <div className="mx-auto max-w-5xl space-y-8">
        <PageHeader title={t('training.runningTitle')} subtitle={t('training.runningSubtitle')} />

        {nextItemQuery.error && (
          <div className="rounded-[2rem] border border-rose-500/20 bg-rose-500/5 p-6 text-rose-500">
            {getApiErrorMessage(nextItemQuery.error)}
          </div>
        )}

        {nextItemQuery.isLoading ? (
          <PanelSkeleton className="min-h-[360px]" />
        ) : !currentItem ? (
          <div className="rounded-[2rem] border border-slate-200 bg-white/70 px-6 py-8 text-sm text-slate-500 dark:border-white/10 dark:bg-white/5 dark:text-white/45">
            正在生成训练总结，请稍候...
          </div>
        ) : (
          <>
            <div className="flex items-center justify-between">
              <div className="text-sm text-slate-500 dark:text-white/45">
                {t('training.progress', {
                  current: nextItemQuery.data?.currentItemOrder || 0,
                  total: nextItemQuery.data?.totalItems || 0,
                })}
              </div>
              <div className="h-2 w-56 overflow-hidden rounded-full bg-slate-200 dark:bg-white/10">
                <div
                  className="h-full bg-gradient-to-r from-emerald-500 to-sky-500"
                  style={{
                    width: `${((nextItemQuery.data?.answeredItems || 0) / Math.max(1, nextItemQuery.data?.totalItems || 1)) * 100}%`,
                  }}
                />
              </div>
            </div>

            <section className="rounded-[3rem] liquid-glass-panel p-10 edge-light">
              <div className="text-xs uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">
                {currentItem.mode} · {currentItem.cognitiveTag} · {lexicalPairTypeLabel(currentItem.lexicalPairType)}
              </div>

              <div className="mt-8 grid gap-6 md:grid-cols-2">
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

              <div className="mt-8 rounded-[2rem] border border-dashed border-slate-300 bg-white/40 p-6 dark:border-white/10 dark:bg-white/5">
                <div className="text-lg font-bold text-slate-900 dark:text-white">
                  {currentItem.content.question}
                </div>
                {currentItem.content.sentence && (
                  <div className="mt-3 italic text-slate-500 dark:text-white/45">
                    {currentItem.content.sentence}
                  </div>
                )}
              </div>

              <div className="mt-8 grid gap-4">
                {currentItem.options.map((option) => (
                  <button
                    key={option.key}
                    type="button"
                    disabled={answerMutation.isPending}
                    onClick={() => void submitAnswer(option)}
                    className="w-full rounded-[1.8rem] border border-slate-200 bg-white/70 px-5 py-4 text-left transition-all hover:border-primary/50 disabled:opacity-60 dark:border-white/10 dark:bg-white/5"
                  >
                    <div className="flex items-center justify-between gap-4">
                      <span className="font-bold text-slate-900 dark:text-white">{option.label}</span>
                      <Rocket size={16} className="text-primary" />
                    </div>
                  </button>
                ))}
              </div>
            </section>
          </>
        )}
      </div>
    );
  }

  if (state.phase === 'summary') {
    const summary = summaryQuery.data;

    return (
      <div className="space-y-8">
        <PageHeader
          title={t('training.summaryTitle')}
          subtitle={
            summary
              ? t('training.summaryLoadedSubtitle', {
                  sessionId: summary.sessionId,
                  mode: summary.mode,
                })
              : t('training.summarySubtitle')
          }
        />

        {summaryQuery.error && (
          <div className="rounded-[2rem] border border-rose-500/20 bg-rose-500/5 p-6 text-rose-500">
            {getApiErrorMessage(summaryQuery.error)}
          </div>
        )}

        {!summary && summaryQuery.isLoading ? (
          <div className="grid gap-8">
            <PanelSkeleton />
            <PanelSkeleton />
          </div>
        ) : null}

        {summary && (
          <>
            <section className="rounded-[3rem] liquid-glass-panel p-10 edge-light">
              <div className="flex flex-col items-start justify-between gap-8 lg:flex-row">
                <div>
                  <div className="inline-flex items-center gap-3 rounded-full border border-amber-500/20 bg-amber-500/10 px-4 py-2 text-xs uppercase tracking-[0.24em] text-amber-500">
                    <Award size={14} />
                    {t('training.sessionCompleted')}
                  </div>
                  <h2 className="mt-5 text-4xl font-black text-slate-900 dark:text-white">
                    {t('training.sessionCompletedTitle')}
                  </h2>
                  <p className="mt-4 leading-7 text-slate-500 dark:text-white/45">
                    {summary.improvementHint}
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => dispatch({ type: 'resetHome' })}
                  className="btn-liquid px-6 py-3 text-white"
                >
                  {t('common.actions.backToTrainingHome')}
                </button>
              </div>
            </section>

            <div className="grid gap-6 md:grid-cols-3">
              <div className="rounded-[2rem] liquid-glass p-6">
                <div className="text-xs uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">
                  {t('training.accuracy')}
                </div>
                <div className="mt-3 text-3xl font-black text-slate-900 dark:text-white">
                  {formatMaybePercent(summary.accuracy)}
                </div>
              </div>
              <div className="rounded-[2rem] liquid-glass p-6">
                <div className="text-xs uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">
                  {t('training.averageReactionTime')}
                </div>
                <div className="mt-3 text-3xl font-black text-slate-900 dark:text-white">
                  {formatMs(summary.averageReactionTime)}
                </div>
              </div>
              <div className="rounded-[2rem] liquid-glass p-6">
                <div className="text-xs uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">
                  {t('training.nextMode')}
                </div>
                <div className="mt-3 text-3xl font-black text-slate-900 dark:text-white">
                  {summary.nextRecommendedMode}
                </div>
              </div>
            </div>

            <section className="rounded-[2.5rem] liquid-glass-panel p-8">
              <div className="mb-4 text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">
                {t('training.riskWords')}
              </div>
              <div className="grid gap-4 md:grid-cols-2">
                {summary.riskWordsToReview.map((item) => (
                  <div
                    key={item.lexicalPairId}
                    className="rounded-[1.6rem] border border-slate-200/70 bg-white/60 p-4 dark:border-white/10 dark:bg-white/5"
                  >
                    <div className="font-black text-slate-900 dark:text-white">
                      {item.englishWord} / {item.frenchWord}
                    </div>
                    <div className="mt-2 text-sm text-slate-500 dark:text-white/45">{item.reason}</div>
                  </div>
                ))}
              </div>
            </section>
          </>
        )}
      </div>
    );
  }

  return (
    <div className="space-y-8">
      <PageHeader title={t('training.homeTitle')} subtitle={t('training.homeSubtitle')} />

      {historyQuery.error && (
        <div className="rounded-[2rem] border border-rose-500/20 bg-rose-500/5 px-6 py-4 text-sm text-rose-500">
          {getApiErrorMessage(historyQuery.error)}
        </div>
      )}

      <div className="rounded-[2rem] border border-emerald-500/20 bg-emerald-500/5 px-6 py-4 text-sm text-emerald-600 dark:text-emerald-400">
        {t('training.recoverNotice')}
      </div>

      {recommendedPlanQuery.error && planError?.status !== 409 && (
        <div className="rounded-[2rem] border border-rose-500/20 bg-rose-500/5 p-6 text-rose-500">
          {getApiErrorMessage(recommendedPlanQuery.error)}
        </div>
      )}

      {planError?.status === 409 ? (
        <section className="rounded-[2.5rem] liquid-glass-panel p-10">
          <div className="flex items-start gap-4">
            <AlertTriangle className="mt-1 shrink-0 text-amber-500" />
            <div>
              <div className="text-2xl font-black text-slate-900 dark:text-white">
                {t('training.noPlanTitle')}
              </div>
              <p className="mt-3 leading-7 text-slate-500 dark:text-white/45">
                {t('training.noPlanDescription')}
              </p>
            </div>
          </div>
        </section>
      ) : recommendedPlanQuery.isLoading && !recommendedPlanQuery.data ? (
        <div className="grid gap-8">
          <PanelSkeleton />
          <PanelSkeleton />
        </div>
      ) : (
        <>
          <section className="rounded-[3rem] liquid-glass-panel p-10 edge-light">
            <div className="grid gap-8 xl:grid-cols-[1fr_0.9fr]">
              <div>
                <div className="inline-flex items-center gap-3 rounded-full border border-sky-500/20 bg-sky-500/10 px-4 py-2 text-xs uppercase tracking-[0.24em] text-sky-500">
                  <Rocket size={14} />
                  {t('training.recommendedPlan')}
                </div>
                <h2 className="mt-5 text-4xl font-black text-slate-900 dark:text-white">
                  {recommendedPlanQuery.data?.priorityMode || t('training.recommendationLoading')}
                </h2>
                <p className="mt-4 leading-7 text-slate-500 dark:text-white/45">
                  {recommendedPlanQuery.data?.recommendationReason || t('training.planLoading')}
                </p>
                {!!recommendedPlanQuery.data?.targetMetrics.length && (
                  <div className="mt-6 flex flex-wrap gap-3">
                    {recommendedPlanQuery.data.targetMetrics.map((metric) => (
                      <span
                        key={metric}
                        className="rounded-full border border-slate-200/70 px-4 py-2 text-sm dark:border-white/10"
                      >
                        {metric}
                      </span>
                    ))}
                  </div>
                )}
              </div>
              <div className="rounded-[2.2rem] border border-slate-200/80 bg-white/60 p-6 dark:border-white/10 dark:bg-white/5">
                <div className="mb-4 text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">
                  {t('training.aiRecommendationTitle')}
                </div>
                {aiRecommendationQuery.isLoading ? (
                  <PanelSkeleton className="min-h-[220px] p-0" />
                ) : aiRecommendationQuery.data ? (
                  <div className="space-y-4">
                    <p className="text-sm leading-7 text-slate-800 dark:text-white/85">
                      {aiRecommendationQuery.data.explanation}
                    </p>
                    {aiRecommendationQuery.data.fallbackReason && (
                      <div className="text-xs uppercase tracking-[0.24em] text-amber-500">
                        {t('training.fallbackReason')} {aiRecommendationQuery.data.fallbackReason}
                      </div>
                    )}
                  </div>
                ) : aiRecommendationQuery.error ? (
                  <div className="text-sm text-rose-500">{getApiErrorMessage(aiRecommendationQuery.error)}</div>
                ) : null}
              </div>
            </div>
          </section>

          <div className="grid gap-8 xl:grid-cols-[1.15fr_0.85fr]">
            <section className="rounded-[2.5rem] liquid-glass-panel p-8">
              <div className="mb-4 text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">
                {t('training.suggestedSessionsTitle')}
              </div>
              <div className="grid gap-4 md:grid-cols-2">
                {(recommendedPlanQuery.data?.suggestedSessions || []).map((session) => (
                  <button
                    key={session.mode}
                    type="button"
                    onClick={() => startMutation.mutate(session.mode)}
                    disabled={startMutation.isPending}
                    className="text-left rounded-[1.8rem] border border-slate-200/80 bg-white/60 p-5 transition-all hover:border-primary/40 disabled:opacity-60 dark:border-white/10 dark:bg-white/5"
                  >
                    <div className="font-black text-slate-900 dark:text-white">{session.label}</div>
                    <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                      {t('training.suggestedQuestionCount', { count: session.count })}
                    </div>
                  </button>
                ))}
              </div>
            </section>

            <section className="rounded-[2.5rem] liquid-glass-panel p-8">
              <div className="mb-4 text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">
                {t('training.recommendedPairsTitle')}
              </div>
              <div className="max-h-[420px] space-y-4 overflow-y-auto no-scrollbar">
                {(recommendedPlanQuery.data?.recommendedPairs || []).slice(0, 6).map((item) => (
                  <div
                    key={item.planItemId}
                    className="rounded-[1.6rem] border border-slate-200/70 bg-white/60 p-4 dark:border-white/10 dark:bg-white/5"
                  >
                    <div className="font-black text-slate-900 dark:text-white">
                      {item.englishWord} / {item.frenchWord}
                    </div>
                    <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                      {item.recommendedMode} · {item.recommendedReason}
                    </div>
                  </div>
                ))}
              </div>
            </section>
          </div>
        </>
      )}

      <div className="grid gap-8 xl:grid-cols-2">
        <section className="rounded-[2.5rem] liquid-glass-panel p-8">
          <div className="mb-4 flex items-center gap-3">
            <Clock3 size={16} className="text-primary" />
            <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">
              {t('training.reviewScheduleTitle')}
            </div>
          </div>
          {reviewScheduleQuery.isLoading ? (
            <PanelSkeleton className="p-0" />
          ) : (
            <div className="space-y-4">
              {(reviewScheduleQuery.data || []).slice(0, 5).map((item) => (
                <div
                  key={item.reviewScheduleId}
                  className="rounded-[1.6rem] border border-slate-200/70 bg-white/60 p-4 dark:border-white/10 dark:bg-white/5"
                >
                  <div className="font-bold text-slate-900 dark:text-white">
                    {item.englishWord} / {item.frenchWord}
                  </div>
                  <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                    {item.reviewMode} · {formatDateTime(item.dueAt)}
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>

        <section className="rounded-[2.5rem] liquid-glass-panel p-8">
          <div className="mb-4 flex items-center gap-3">
            <Brain size={16} className="text-primary" />
            <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">
              {t('training.wrongBookTitle')}
            </div>
          </div>
          {wrongBookQuery.isLoading ? (
            <PanelSkeleton className="p-0" />
          ) : (
            <div className="space-y-4">
              {(wrongBookQuery.data || []).slice(0, 5).map((item) => (
                <div
                  key={item.wrongBookId}
                  className="rounded-[1.6rem] border border-slate-200/70 bg-white/60 p-4 dark:border-white/10 dark:bg-white/5"
                >
                  <div className="font-bold text-slate-900 dark:text-white">
                    {item.englishWord} / {item.frenchWord}
                  </div>
                  <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                    {lexicalPairTypeLabel(item.lexicalPairType)} · {item.lastErrorType} ·{' '}
                    {t('training.wrongCount', { count: item.wrongCount })}
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>
      </div>
    </div>
  );
};

export default TrainingPage;
