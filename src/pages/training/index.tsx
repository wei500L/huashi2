import React from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AlertTriangle, Award, Brain, Clock3, Rocket } from 'lucide-react';
import { PageHeader } from '@/components/common';
import { aiService, trainingService } from '@/lib/services';
import { formatDateTime, formatMaybePercent, formatMs, lexicalPairTypeLabel } from '@/lib/format';
import { normalizeApiError } from '@/lib/api';
import type { TrainingOptionViewVO } from '@/lib/contracts';

type TrainingPhase = 'boot' | 'home' | 'running' | 'summary';

const TrainingPage: React.FC = () => {
  const queryClient = useQueryClient();
  const [phase, setPhase] = React.useState<TrainingPhase>('boot');
  const [sessionId, setSessionId] = React.useState<number | null>(null);
  const [summarySessionId, setSummarySessionId] = React.useState<number | null>(null);
  const shownAtRef = React.useRef<number>(Date.now());

  const historyQuery = useQuery({
    queryKey: ['training-history', 'in-progress'],
    queryFn: () => trainingService.listHistory({ pageNo: 1, pageSize: 1, status: 'IN_PROGRESS' }),
  });

  React.useEffect(() => {
    if (!historyQuery.data) {
      return;
    }
    const inProgress = historyQuery.data.records[0];
    if (inProgress?.sessionId) {
      setSessionId(inProgress.sessionId);
      shownAtRef.current = Date.now();
      setPhase('running');
      return;
    }
    setPhase('home');
  }, [historyQuery.data]);

  React.useEffect(() => {
    if (historyQuery.error) {
      setPhase('home');
    }
  }, [historyQuery.error]);

  const recommendedPlanQuery = useQuery({
    queryKey: ['recommended-training-plan'],
    queryFn: () => trainingService.getRecommendedPlan(),
    enabled: phase === 'home',
    retry: false,
  });

  const aiRecommendationQuery = useQuery({
    queryKey: ['ai-recommend-training', recommendedPlanQuery.data?.sourceDiagnosisSummaryId],
    queryFn: () => aiService.recommendTraining(recommendedPlanQuery.data?.sourceDiagnosisSummaryId),
    enabled: phase === 'home' && !!recommendedPlanQuery.data,
    retry: false,
  });

  const wrongBookQuery = useQuery({
    queryKey: ['wrong-book'],
    queryFn: () => trainingService.getWrongBook(),
  });

  const reviewScheduleQuery = useQuery({
    queryKey: ['review-schedule', true],
    queryFn: () => trainingService.getReviewSchedule(true),
  });

  const startMutation = useMutation({
    mutationFn: (mode: string) =>
      trainingService.startSession({
        planId: recommendedPlanQuery.data!.planId,
        mode,
      }),
    onSuccess: (created) => {
      setSessionId(created.sessionId);
      shownAtRef.current = Date.now();
      setPhase('running');
      void queryClient.invalidateQueries({ queryKey: ['training-history'] });
    },
  });

  const nextItemQuery = useQuery({
    queryKey: ['training-next-item', sessionId],
    queryFn: () => trainingService.getNextItem(sessionId as number),
    enabled: phase === 'running' && !!sessionId,
  });

  const completeMutation = useMutation({
    mutationFn: (value: number) => trainingService.complete(value),
    onSuccess: (_, currentSessionId) => {
      setSummarySessionId(currentSessionId);
      setPhase('summary');
      void queryClient.invalidateQueries({ queryKey: ['training-history'] });
      void queryClient.invalidateQueries({ queryKey: ['student-overview'] });
      void queryClient.invalidateQueries({ queryKey: ['student-trends'] });
      void queryClient.invalidateQueries({ queryKey: ['wrong-book'] });
      void queryClient.invalidateQueries({ queryKey: ['review-schedule'] });
    },
  });

  const answerMutation = useMutation({
    mutationFn: (payload: { itemResultId: number; selectedAnswerKey: string; reactionTimeMs: number; hesitationTimeMs: number }) =>
      trainingService.submitAnswer(sessionId as number, payload),
    onSuccess: async (progress) => {
      if (progress.completed || progress.answeredItems >= progress.totalItems) {
        await completeMutation.mutateAsync(progress.sessionId);
        return;
      }
      shownAtRef.current = Date.now();
      await nextItemQuery.refetch();
    },
  });

  const summaryQuery = useQuery({
    queryKey: ['training-summary', summarySessionId],
    queryFn: () => trainingService.getSummary(summarySessionId as number),
    enabled: phase === 'summary' && !!summarySessionId,
  });

  React.useEffect(() => {
    if (!sessionId || phase !== 'running') {
      return;
    }
    const persist = () => {
      const snapshot = nextItemQuery.data
        ? {
            sessionId,
            currentItemOrder: nextItemQuery.data.currentItemOrder,
            answeredItems: nextItemQuery.data.answeredItems,
            timestamp: new Date().toISOString(),
          }
        : { sessionId, timestamp: new Date().toISOString() };
      void trainingService.saveProgress(sessionId, snapshot);
    };
    const onVisibilityChange = () => {
      if (document.hidden) {
        persist();
      }
    };
    window.addEventListener('beforeunload', persist);
    document.addEventListener('visibilitychange', onVisibilityChange);
    return () => {
      window.removeEventListener('beforeunload', persist);
      document.removeEventListener('visibilitychange', onVisibilityChange);
    };
  }, [nextItemQuery.data, phase, sessionId]);

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

  if (phase === 'boot' || historyQuery.isLoading) {
    return (
      <div className="min-h-[60vh] flex items-center justify-center">
        <div className="text-sm uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">loading training session</div>
      </div>
    );
  }

  if (phase === 'running') {
    return (
      <div className="max-w-5xl mx-auto space-y-8">
        <PageHeader title="训练进行中" subtitle="训练 session 会自动保存进度；刷新后会优先恢复当前未完成训练。" />

        {nextItemQuery.error && (
          <div className="rounded-[2rem] border border-rose-500/20 bg-rose-500/5 p-6 text-rose-500">{nextItemQuery.error.message}</div>
        )}

        {nextItemQuery.isLoading || !currentItem ? (
          <div className="min-h-[360px] rounded-[2.8rem] liquid-glass-panel flex items-center justify-center">
            <div className="text-sm uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">loading training item</div>
          </div>
        ) : (
          <>
            <div className="flex items-center justify-between">
              <div className="text-sm text-slate-500 dark:text-white/45">
                第 {nextItemQuery.data?.currentItemOrder}/{nextItemQuery.data?.totalItems} 题
              </div>
              <div className="w-56 h-2 rounded-full bg-slate-200 dark:bg-white/10 overflow-hidden">
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

              <div className="mt-8 grid md:grid-cols-2 gap-6">
                <div className="rounded-[2rem] border border-slate-200/80 dark:border-white/10 bg-white/60 dark:bg-white/5 p-8">
                  <div className="text-sm uppercase tracking-[0.24em] text-sky-500 mb-3">English</div>
                  <div className="text-4xl font-black text-slate-900 dark:text-white">{currentItem.englishWord}</div>
                </div>
                <div className="rounded-[2rem] border border-slate-200/80 dark:border-white/10 bg-white/60 dark:bg-white/5 p-8">
                  <div className="text-sm uppercase tracking-[0.24em] text-rose-500 mb-3">French</div>
                  <div className="text-4xl font-black text-slate-900 dark:text-white">{currentItem.frenchWord}</div>
                </div>
              </div>

              <div className="mt-8 rounded-[2rem] border border-dashed border-slate-300 dark:border-white/10 p-6 bg-white/40 dark:bg-white/5">
                <div className="text-lg font-bold text-slate-900 dark:text-white">{currentItem.content.question}</div>
                {currentItem.content.sentence && (
                  <div className="mt-3 text-slate-500 dark:text-white/45 italic">{currentItem.content.sentence}</div>
                )}
              </div>

              <div className="mt-8 grid gap-4">
                {currentItem.options.map((option) => (
                  <button
                    key={option.key}
                    type="button"
                    disabled={answerMutation.isPending || completeMutation.isPending}
                    onClick={() => void submitAnswer(option)}
                    className="w-full rounded-[1.8rem] border border-slate-200 dark:border-white/10 bg-white/70 dark:bg-white/5 px-5 py-4 text-left hover:border-primary/50 transition-all disabled:opacity-60"
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

  if (phase === 'summary') {
    const summary = summaryQuery.data;
    return (
      <div className="space-y-8">
        <PageHeader title="训练总结" subtitle={summary ? `Session #${summary.sessionId} · ${summary.mode}` : '正在加载本次训练总结'} />

        {summaryQuery.error && (
          <div className="rounded-[2rem] border border-rose-500/20 bg-rose-500/5 p-6 text-rose-500">{summaryQuery.error.message}</div>
        )}

        {summary && (
          <>
            <section className="rounded-[3rem] liquid-glass-panel p-10 edge-light">
              <div className="flex flex-col lg:flex-row items-start justify-between gap-8">
                <div>
                  <div className="inline-flex items-center gap-3 rounded-full border border-amber-500/20 bg-amber-500/10 px-4 py-2 text-xs uppercase tracking-[0.24em] text-amber-500">
                    <Award size={14} />
                    session completed
                  </div>
                  <h2 className="mt-5 text-4xl font-black text-slate-900 dark:text-white">本轮训练已完成</h2>
                  <p className="mt-4 text-slate-500 dark:text-white/45 leading-7">{summary.improvementHint}</p>
                </div>
                <button
                  type="button"
                  onClick={() => {
                    setPhase('home');
                    setSessionId(null);
                    setSummarySessionId(null);
                  }}
                  className="btn-liquid px-6 py-3 text-white"
                >
                  返回训练首页
                </button>
              </div>
            </section>

            <div className="grid md:grid-cols-3 gap-6">
              <div className="rounded-[2rem] liquid-glass p-6">
                <div className="text-xs uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">本次正确率</div>
                <div className="mt-3 text-3xl font-black text-slate-900 dark:text-white">{formatMaybePercent(summary.accuracy)}</div>
              </div>
              <div className="rounded-[2rem] liquid-glass p-6">
                <div className="text-xs uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">平均反应时</div>
                <div className="mt-3 text-3xl font-black text-slate-900 dark:text-white">{formatMs(summary.averageReactionTime)}</div>
              </div>
              <div className="rounded-[2rem] liquid-glass p-6">
                <div className="text-xs uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">下一推荐模式</div>
                <div className="mt-3 text-3xl font-black text-slate-900 dark:text-white">{summary.nextRecommendedMode}</div>
              </div>
            </div>

            <section className="rounded-[2.5rem] liquid-glass-panel p-8">
              <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30 mb-4">risk words to review</div>
              <div className="grid md:grid-cols-2 gap-4">
                {summary.riskWordsToReview.map((item) => (
                  <div key={item.lexicalPairId} className="rounded-[1.6rem] border border-slate-200/70 dark:border-white/10 p-4 bg-white/60 dark:bg-white/5">
                    <div className="font-black text-slate-900 dark:text-white">{item.englishWord} / {item.frenchWord}</div>
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
      <PageHeader title="个性化训练" subtitle="系统会先检查是否存在未完成训练，再决定恢复或创建新 session。" />

      {historyQuery.error && (
        <div className="rounded-[2rem] border border-rose-500/20 bg-rose-500/5 px-6 py-4 text-sm text-rose-500">{historyQuery.error.message}</div>
      )}

      <div className="rounded-[2rem] border border-emerald-500/20 bg-emerald-500/5 px-6 py-4 text-sm text-emerald-600 dark:text-emerald-400">
        训练链路已支持后端原生保存与恢复。离开页面时会保存快照，重新进入会恢复最近一个未完成 session。
      </div>

      {recommendedPlanQuery.error && planError?.status !== 409 && (
        <div className="rounded-[2rem] border border-rose-500/20 bg-rose-500/5 p-6 text-rose-500">{recommendedPlanQuery.error.message}</div>
      )}

      {planError?.status === 409 ? (
        <section className="rounded-[2.5rem] liquid-glass-panel p-10">
          <div className="flex items-start gap-4">
            <AlertTriangle className="text-amber-500 shrink-0 mt-1" />
            <div>
              <div className="text-2xl font-black text-slate-900 dark:text-white">尚无推荐训练计划</div>
              <p className="mt-3 text-slate-500 dark:text-white/45 leading-7">
                训练计划依赖最近一次诊断 summary。请先到诊断页完成一次真实诊断，再回来开始训练。
              </p>
            </div>
          </div>
        </section>
      ) : (
        <>
          <section className="rounded-[3rem] liquid-glass-panel p-10 edge-light">
            <div className="grid xl:grid-cols-[1fr_0.9fr] gap-8">
              <div>
                <div className="inline-flex items-center gap-3 rounded-full border border-sky-500/20 bg-sky-500/10 px-4 py-2 text-xs uppercase tracking-[0.24em] text-sky-500">
                  <Rocket size={14} />
                  recommended plan
                </div>
                <h2 className="mt-5 text-4xl font-black text-slate-900 dark:text-white">
                  {recommendedPlanQuery.data?.priorityMode || '正在生成训练建议'}
                </h2>
                <p className="mt-4 text-slate-500 dark:text-white/45 leading-7">
                  {recommendedPlanQuery.data?.recommendationReason || '系统正在读取最新训练计划。'}
                </p>
                {!!recommendedPlanQuery.data?.targetMetrics.length && (
                  <div className="mt-6 flex flex-wrap gap-3">
                    {recommendedPlanQuery.data.targetMetrics.map((metric) => (
                      <span key={metric} className="rounded-full border border-slate-200/70 dark:border-white/10 px-4 py-2 text-sm">
                        {metric}
                      </span>
                    ))}
                  </div>
                )}
              </div>
              <div className="rounded-[2.2rem] border border-slate-200/80 dark:border-white/10 p-6 bg-white/60 dark:bg-white/5">
                <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30 mb-4">AI recommendation</div>
                {aiRecommendationQuery.isLoading ? (
                  <div className="text-sm text-slate-500 dark:text-white/45">正在生成 AI 训练建议...</div>
                ) : aiRecommendationQuery.data ? (
                  <div className="space-y-4">
                    <p className="text-sm leading-7 text-slate-800 dark:text-white/85">{aiRecommendationQuery.data.explanation}</p>
                    {aiRecommendationQuery.data.fallbackReason && (
                      <div className="text-xs uppercase tracking-[0.24em] text-amber-500">规则回退：{aiRecommendationQuery.data.fallbackReason}</div>
                    )}
                  </div>
                ) : aiRecommendationQuery.error ? (
                  <div className="text-sm text-rose-500">{aiRecommendationQuery.error.message}</div>
                ) : null}
              </div>
            </div>
          </section>

          <div className="grid xl:grid-cols-[1.15fr_0.85fr] gap-8">
            <section className="rounded-[2.5rem] liquid-glass-panel p-8">
              <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30 mb-4">suggested sessions</div>
              <div className="grid md:grid-cols-2 gap-4">
                {(recommendedPlanQuery.data?.suggestedSessions || []).map((session) => (
                  <button
                    key={session.mode}
                    type="button"
                    onClick={() => startMutation.mutate(session.mode)}
                    disabled={startMutation.isPending}
                    className="text-left rounded-[1.8rem] border border-slate-200/80 dark:border-white/10 bg-white/60 dark:bg-white/5 p-5 hover:border-primary/40 transition-all disabled:opacity-60"
                  >
                    <div className="font-black text-slate-900 dark:text-white">{session.label}</div>
                    <div className="mt-2 text-sm text-slate-500 dark:text-white/45">建议题量 {session.count}</div>
                  </button>
                ))}
              </div>
            </section>

            <section className="rounded-[2.5rem] liquid-glass-panel p-8">
              <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30 mb-4">recommended pairs</div>
              <div className="space-y-4 max-h-[420px] overflow-y-auto no-scrollbar">
                {(recommendedPlanQuery.data?.recommendedPairs || []).slice(0, 6).map((item) => (
                  <div key={item.planItemId} className="rounded-[1.6rem] border border-slate-200/70 dark:border-white/10 p-4 bg-white/60 dark:bg-white/5">
                    <div className="font-black text-slate-900 dark:text-white">{item.englishWord} / {item.frenchWord}</div>
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

      <div className="grid xl:grid-cols-2 gap-8">
        <section className="rounded-[2.5rem] liquid-glass-panel p-8">
          <div className="flex items-center gap-3 mb-4">
            <Clock3 size={16} className="text-primary" />
            <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">review schedule</div>
          </div>
          <div className="space-y-4">
            {(reviewScheduleQuery.data || []).slice(0, 5).map((item) => (
              <div key={item.reviewScheduleId} className="rounded-[1.6rem] border border-slate-200/70 dark:border-white/10 p-4 bg-white/60 dark:bg-white/5">
                <div className="font-bold text-slate-900 dark:text-white">{item.englishWord} / {item.frenchWord}</div>
                <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                  {item.reviewMode} · {formatDateTime(item.dueAt)}
                </div>
              </div>
            ))}
          </div>
        </section>

        <section className="rounded-[2.5rem] liquid-glass-panel p-8">
          <div className="flex items-center gap-3 mb-4">
            <Brain size={16} className="text-primary" />
            <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">wrong book</div>
          </div>
          <div className="space-y-4">
            {(wrongBookQuery.data || []).slice(0, 5).map((item) => (
              <div key={item.wrongBookId} className="rounded-[1.6rem] border border-slate-200/70 dark:border-white/10 p-4 bg-white/60 dark:bg-white/5">
                <div className="font-bold text-slate-900 dark:text-white">{item.englishWord} / {item.frenchWord}</div>
                <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                  {lexicalPairTypeLabel(item.lexicalPairType)} · {item.lastErrorType} · 错误 {item.wrongCount} 次
                </div>
              </div>
            ))}
          </div>
        </section>
      </div>
    </div>
  );
};

export default TrainingPage;
