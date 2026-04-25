import React from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { AlertTriangle, Brain, CheckCircle2, ChevronRight, FileText, Timer } from 'lucide-react';
import { flushSync } from 'react-dom';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { DiagnosisPdfReport } from '@/components/diagnosis/DiagnosisPdfReport';
import { PageHeader, PanelSkeleton, SectionEyebrow } from '@/components/common';
import { EChart } from '@/components/common/EChart';
import { getApiErrorMessage } from '@/lib/api';
import { clearDiagnosisLaunchParams, parseDiagnosisLaunchNumber } from '@/lib/diagnosis-launch';
import { exportReportPagesToPdf } from '@/lib/pdf-report';
import { aiService, diagnosisSessionService, diagnosisTemplateService, trainingService } from '@/lib/services';
import { buildRadarOption, formatDateTime, formatMaybePercent, formatMs, lexicalPairTypeLabel } from '@/lib/format';
import { buildTrainingHref } from '@/lib/training-launch';
import { toDiagnosisRadarChartMetrics } from './radarMetrics';
import type {
  DiagnosisItemResultDetailVO,
  DiagnosisOptionPayload,
  DiagnosisOptionViewVO,
  DiagnosisHistorySummaryVO,
  SubmitDiagnosisAnswerRequest,
} from '@/lib/contracts';
import { SessionFeedbackBanners, SessionOptionButton, SessionProgressHeader, SessionSaveActions } from '@/features/session-runtime/components';
import { HESITATION_BASELINE_MS, NEXT_ITEM_RETRY_DELAY_MS, SLOW_NEXT_ITEM_NOTICE_DELAY_MS } from '@/features/session-runtime/constants';
import { buildSessionSnapshot } from '@/features/session-runtime/helpers';
import { useSessionRuntime } from '@/features/session-runtime/useSessionRuntime';
import { diagnosisFlowReducer, initialDiagnosisFlowState } from './flow';

function findOptionLabel(options: DiagnosisOptionPayload[], answerKey?: string | null) {
  if (!answerKey) {
    return null;
  }
  return options.find((option) => option.key === answerKey)?.label || answerKey;
}

function DiagnosisItemReviewCard({ item }: { item: DiagnosisItemResultDetailVO }) {
  const selectedLabel = findOptionLabel(item.options, item.selectedAnswerKey);
  const correctLabel = findOptionLabel(item.options, item.correctAnswerKey);

  return (
    <div className="rounded-[1.6rem] border border-slate-200/70 bg-white/60 p-4 dark:border-white/10 dark:bg-white/5">
      <div className="flex items-start justify-between gap-4">
        <div>
          <div className="font-black text-slate-900 dark:text-white">
            {item.englishWord} / {item.frenchWord}
          </div>
          <div className="mt-2 text-sm leading-6 text-slate-500 dark:text-white/45">
            {item.taskType} · {item.detectedErrorType} · {formatMaybePercent(item.transferRiskScore)}
          </div>
          {(item.stimulus.promptText || item.stimulus.instruction || item.stimulus.contextSentence) && (
            <div className="mt-3 rounded-[1.2rem] border border-dashed border-slate-200/80 px-4 py-3 text-sm text-slate-600 dark:border-white/10 dark:text-white/60">
              {item.stimulus.instruction && <div className="font-semibold">{item.stimulus.instruction}</div>}
              {item.stimulus.promptText && <div className="mt-1">{item.stimulus.promptText}</div>}
              {item.stimulus.contextSentence && <div className="mt-2 italic">{item.stimulus.contextSentence}</div>}
            </div>
          )}
        </div>
        <div className="text-right text-sm text-slate-500 dark:text-white/45">
          <div>{item.correct ? '答对' : '答错'}</div>
          <div>{formatMs(item.reactionTimeMs)}</div>
        </div>
      </div>

      {!!item.options.length && (
        <div className="mt-4 grid gap-2">
          {item.options.map((option) => {
            const isSelected = option.key === item.selectedAnswerKey;
            const isCorrect = option.key === item.correctAnswerKey;
            return (
              <div
                key={option.key}
                className={`rounded-[1rem] border px-3 py-2 text-sm ${
                  isCorrect
                    ? 'border-emerald-500/30 bg-emerald-500/10 text-emerald-700 dark:text-emerald-300'
                    : isSelected
                      ? 'border-rose-500/20 bg-rose-500/5 text-rose-600 dark:text-rose-300'
                      : 'border-slate-200/70 bg-white/70 text-slate-600 dark:border-white/10 dark:bg-white/5 dark:text-white/60'
                }`}
              >
                {option.label}
              </div>
            );
          })}
        </div>
      )}

      <div className="mt-4 grid gap-2 text-sm text-slate-500 dark:text-white/45">
        <div>你的答案：{selectedLabel || '未作答'}</div>
        <div>正确答案：{correctLabel || '未返回'}</div>
      </div>
    </div>
  );
}

function DiagnosisInsightCard({
  title,
  items,
  toneClassName,
}: {
  title: string;
  items: string[];
  toneClassName: string;
}) {
  return (
    <div className={`rounded-[1.6rem] border p-4 ${toneClassName}`}>
      <SectionEyebrow>{title}</SectionEyebrow>
      <div className="mt-3 space-y-3">
        {items.map((entry) => (
          <div key={entry} className="rounded-[1rem] bg-white/70 px-3 py-3 text-sm leading-6 text-slate-700 dark:bg-white/5 dark:text-white/75">
            {entry}
          </div>
        ))}
      </div>
    </div>
  );
}

const DiagnosisPage: React.FC = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const queryClient = useQueryClient();
  const [state, dispatch] = React.useReducer(diagnosisFlowReducer, initialDiagnosisFlowState);
  const shownAtRef = React.useRef<number>(Date.now());
  const answerRequestRef = React.useRef<SubmitDiagnosisAnswerRequest | null>(null);
  const [submitErrorMessage, setSubmitErrorMessage] = React.useState<string | null>(null);
  const [submitInfoMessage, setSubmitInfoMessage] = React.useState<string | null>(null);
  const [loadInfoMessage, setLoadInfoMessage] = React.useState<string | null>(null);
  const [pendingNextItemId, setPendingNextItemId] = React.useState<number | null>(null);
  const [reportErrorMessage, setReportErrorMessage] = React.useState<string | null>(null);
  const [isPdfExporting, setIsPdfExporting] = React.useState(false);
  const [reportGeneratedAt, setReportGeneratedAt] = React.useState<string | null>(null);
  const [resumeCandidate, setResumeCandidate] = React.useState<DiagnosisHistorySummaryVO | null>(null);
  const reportRef = React.useRef<HTMLDivElement | null>(null);
  const requestedSource = searchParams.get('source');
  const requestedSourceSummaryId = parseDiagnosisLaunchNumber(searchParams.get('sourceSummaryId'));

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
      dispatch({ type: 'readyToSelect' });
      setResumeCandidate(inProgress);
      return;
    }
    setResumeCandidate(null);
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
    mutationFn: (templateId: number) =>
      diagnosisSessionService.create({
        templateId,
        launchSource: requestedSource || undefined,
        sourceSummaryId: requestedSourceSummaryId,
      }),
    onSuccess: (created) => {
      shownAtRef.current = Date.now();
      setSearchParams(clearDiagnosisLaunchParams(searchParams), { replace: true });
      dispatch({ type: 'startSession', sessionId: created.sessionId });
      void queryClient.invalidateQueries({ queryKey: ['diagnosis-history'] });
    },
  });

  const nextItemQuery = useQuery({
    queryKey: ['diagnosis-next-item', state.sessionId],
    queryFn: ({ signal }) => diagnosisSessionService.getNextItem(state.sessionId as number, { signal }),
    enabled: state.phase === 'running' && !!state.sessionId,
    retry: 1,
    retryDelay: NEXT_ITEM_RETRY_DELAY_MS,
  });

  const markCompleted = React.useCallback(() => {
    dispatch({ type: 'showResult' });
    void queryClient.invalidateQueries({ queryKey: ['diagnosis-history'] });
    void queryClient.invalidateQueries({ queryKey: ['student-overview'] });
    void queryClient.invalidateQueries({ queryKey: ['recommended-training-plan'] });
  }, [queryClient]);

  const submitAnswerMutation = useMutation({
    mutationFn: (payload: SubmitDiagnosisAnswerRequest) =>
      diagnosisSessionService.submitAnswer(state.sessionId as number, payload),
    onSuccess: async (progress, payload) => {
      setSubmitErrorMessage(null);
      if (progress.completed) {
        answerRequestRef.current = null;
        setPendingNextItemId(null);
        setSubmitInfoMessage(null);
        markCompleted();
        return;
      }
      shownAtRef.current = Date.now();
      setPendingNextItemId(payload.itemResultId);
      const refreshed = await nextItemQuery.refetch();
      if (refreshed.error) {
        setSubmitInfoMessage('答案已提交，但下一题加载失败。请重试加载当前题，系统不会重复计入本题。');
        return;
      }
      if (refreshed.data?.readyToComplete) {
        setPendingNextItemId(null);
        setSubmitInfoMessage('最后一题已完成，请确认交卷。');
        return;
      }
      setPendingNextItemId(null);
      setSubmitInfoMessage(null);
    },
    onError: async (error, payload) => {
      const refreshed = await nextItemQuery.refetch();
      if (refreshed.data?.sessionStatus === 'COMPLETED') {
        answerRequestRef.current = null;
        setSubmitErrorMessage(null);
        setSubmitInfoMessage('答案已提交，系统已同步到最新结果。');
        markCompleted();
        return;
      }
      if (refreshed.data?.readyToComplete) {
        setPendingNextItemId(null);
        setSubmitErrorMessage(null);
        setSubmitInfoMessage('答案已提交，请确认交卷。');
        shownAtRef.current = Date.now();
        return;
      }
      if (refreshed.data?.item && refreshed.data.item.itemResultId !== payload.itemResultId) {
        setSubmitErrorMessage(null);
        setSubmitInfoMessage('答案已提交，系统已同步到下一题。');
        shownAtRef.current = Date.now();
        return;
      }
      setSubmitInfoMessage(null);
      setSubmitErrorMessage(getApiErrorMessage(error));
    },
  });

  const resultQuery = useQuery({
    queryKey: ['diagnosis-result', state.sessionId],
    queryFn: ({ signal }) => diagnosisSessionService.getResult(state.sessionId as number, { signal }),
    enabled: state.phase === 'result' && !!state.sessionId,
  });

  const resultSummaryId = resultQuery.data?.summaryId;
  const explanationQuery = useQuery({
    queryKey: ['diagnosis-explanation', resultSummaryId],
    queryFn: ({ signal }) => aiService.explainDiagnosis(resultSummaryId, { signal }),
    enabled: state.phase === 'result' && !!resultSummaryId,
    retry: false,
  });

  const recommendedPlanQuery = useQuery({
    queryKey: ['recommended-training-plan', resultSummaryId],
    queryFn: ({ signal }) => trainingService.getRecommendedPlan({ diagnosisSummaryId: resultSummaryId }, { signal }),
    enabled: state.phase === 'result' && !!resultSummaryId,
    retry: false,
  });

  const handlePdfExport = async () => {
    if (!resultQuery.data) {
      return;
    }

    try {
      setIsPdfExporting(true);
      setReportErrorMessage(null);
      flushSync(() => {
        setReportGeneratedAt(new Date().toISOString());
      });
      await exportReportPagesToPdf(reportRef.current, `diagnosis-session-${resultQuery.data.sessionId}-report.pdf`);
    } catch (error) {
      setReportErrorMessage(error instanceof Error ? error.message : 'PDF 报告导出失败');
    } finally {
      setIsPdfExporting(false);
      setReportGeneratedAt(null);
    }
  };

  const completeSessionMutation = useMutation({
    mutationFn: () => diagnosisSessionService.complete(state.sessionId as number),
    onSuccess: () => {
      answerRequestRef.current = null;
      setPendingNextItemId(null);
      setSubmitErrorMessage(null);
      setSubmitInfoMessage(null);
      markCompleted();
    },
    onError: async (error) => {
      const refreshed = await nextItemQuery.refetch();
      if (refreshed.data?.sessionStatus === 'COMPLETED') {
        markCompleted();
        return;
      }
      setSubmitErrorMessage(getApiErrorMessage(error));
    },
  });

  const abandonSessionMutation = useMutation({
    mutationFn: (sessionId: number) => diagnosisSessionService.abandon(sessionId),
    onSuccess: async () => {
      answerRequestRef.current = null;
      setPendingNextItemId(null);
      setSubmitErrorMessage(null);
      setSubmitInfoMessage(null);
      setResumeCandidate(null);
      dispatch({ type: 'reset' });
      await queryClient.invalidateQueries({ queryKey: ['diagnosis-history'] });
    },
    onError: (error) => {
      setSubmitErrorMessage(getApiErrorMessage(error));
    },
  });

  React.useEffect(() => {
    if (state.phase !== 'running' || !nextItemQuery.data || nextItemQuery.data.hasNextItem) {
      return;
    }
    if (nextItemQuery.data.sessionStatus === 'COMPLETED') {
      answerRequestRef.current = null;
      markCompleted();
      return;
    }
    if (nextItemQuery.data.sessionStatus === 'ABANDONED') {
      answerRequestRef.current = null;
      setSubmitInfoMessage(null);
      setSubmitErrorMessage('当前诊断会话已被系统废弃，请返回重新开始。');
      dispatch({ type: 'reset' });
    }
  }, [markCompleted, nextItemQuery.data, state.phase]);

  const runtime = useSessionRuntime({
    active: state.phase === 'running',
    sessionId: state.sessionId,
    nextItem: nextItemQuery.data,
    refetchCurrent: nextItemQuery.refetch,
    buildSnapshot: (sessionId, nextItem) => buildSessionSnapshot(sessionId, nextItem),
    heartbeat: diagnosisSessionService.heartbeat,
    shouldHeartbeat: (nextItem) => nextItem?.hasNextItem === true,
    isHeartbeatInProgress: (heartbeat) => heartbeat.status === 'IN_PROGRESS',
    saveProgress: diagnosisSessionService.saveProgress,
    saveProgressKeepalive: diagnosisSessionService.saveProgressKeepalive,
    isCompleted: (nextItem) => nextItem?.sessionStatus === 'COMPLETED',
    onCompleted: () => markCompleted(),
  });

  const currentItem = nextItemQuery.data?.item;
  const staleSubmittedItemVisible = !!currentItem && pendingNextItemId === currentItem.itemResultId;
  const isAnswerLocked =
    submitAnswerMutation.isPending ||
    completeSessionMutation.isPending ||
    abandonSessionMutation.isPending ||
    nextItemQuery.isFetching;

  React.useEffect(() => {
    if (!currentItem) {
      setLoadInfoMessage(null);
      answerRequestRef.current = null;
      return;
    }
    runtime.resetFeedback();
    setLoadInfoMessage(null);
    if (answerRequestRef.current?.itemResultId !== currentItem.itemResultId) {
      answerRequestRef.current = null;
    }
    setSubmitErrorMessage(null);
  }, [currentItem?.itemResultId, runtime.resetFeedback]);

  React.useEffect(() => {
    if (!currentItem || pendingNextItemId === currentItem.itemResultId) {
      return;
    }
    setPendingNextItemId(null);
    setSubmitInfoMessage(null);
  }, [currentItem?.itemResultId, pendingNextItemId]);

  React.useEffect(() => {
    if (!pendingNextItemId || !nextItemQuery.isFetching || nextItemQuery.error) {
      setLoadInfoMessage(null);
      return;
    }
    const timer = window.setTimeout(() => {
      setLoadInfoMessage(
        nextItemQuery.failureCount > 0 ? '网络波动，正在重试加载下一题…' : '答案已提交，正在同步下一题…'
      );
    }, SLOW_NEXT_ITEM_NOTICE_DELAY_MS);
    return () => {
      window.clearTimeout(timer);
    };
  }, [nextItemQuery.error, nextItemQuery.failureCount, nextItemQuery.isFetching, pendingNextItemId]);

  const submitAnswer = async (option: DiagnosisOptionViewVO) => {
    if (!currentItem) {
      return;
    }
    const existingRequest =
      answerRequestRef.current?.itemResultId === currentItem.itemResultId ? answerRequestRef.current : null;
    const request = existingRequest ?? (() => {
      const reactionTimeMs = Math.max(1, Date.now() - shownAtRef.current);
      const hesitationTimeMs = Math.max(0, reactionTimeMs - HESITATION_BASELINE_MS);

      if (currentItem.taskType === 'REACTION_TIME') {
        return {
          itemResultId: currentItem.itemResultId,
          clientRequestId: crypto.randomUUID(),
          selectedAnswerKey: option.key,
          selectedSemanticMatch: option.semanticMatch ?? undefined,
          reactionTimeMs,
          hesitationTimeMs,
        } satisfies SubmitDiagnosisAnswerRequest;
      }

      return {
        itemResultId: currentItem.itemResultId,
        clientRequestId: crypto.randomUUID(),
        selectedAnswerKey: option.key,
        reactionTimeMs,
        hesitationTimeMs,
      } satisfies SubmitDiagnosisAnswerRequest;
    })();
    answerRequestRef.current = request;
    shownAtRef.current = Date.now();
    await submitAnswerMutation.mutateAsync(request);
  };

  const handleResumeContinue = () => {
    if (!resumeCandidate) {
      return;
    }
    shownAtRef.current = Date.now();
    dispatch({ type: 'resumeSession', sessionId: resumeCandidate.sessionId });
    setResumeCandidate(null);
  };

  const handleResumeAbandon = async () => {
    if (!resumeCandidate) {
      return;
    }
    await abandonSessionMutation.mutateAsync(resumeCandidate.sessionId);
  };

  const handleRunningAbandon = async () => {
    if (!state.sessionId || !window.confirm('确认放弃当前诊断吗？本次未完成内容将标记为已废弃。')) {
      return;
    }
    await abandonSessionMutation.mutateAsync(state.sessionId);
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
        <SessionFeedbackBanners
          submitErrorMessage={createSessionMutation.error ? getApiErrorMessage(createSessionMutation.error) : null}
        />
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
                  {template.targetClassName ? <span>班级：{template.targetClassName}</span> : null}
                </div>
              </button>
            ))}
          </div>
        )}

        {resumeCandidate && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/45 px-4 backdrop-blur-sm">
            <div className="w-full max-w-xl rounded-[2.4rem] border border-slate-200/70 bg-white p-8 shadow-2xl dark:border-white/10 dark:bg-slate-950">
              <div className="flex items-start gap-4">
                <div className="rounded-full bg-amber-500/10 p-3 text-amber-500">
                  <AlertTriangle size={20} />
                </div>
                <div className="flex-1">
                  <div className="text-2xl font-black text-slate-900 dark:text-white">发现未完成诊断</div>
                  <div className="mt-3 text-sm leading-6 text-slate-500 dark:text-white/50">
                    上次保存时间：{resumeCandidate.lastSavedAt ? formatDateTime(resumeCandidate.lastSavedAt) : '未知'}。你可以继续答题，也可以放弃本次后重新开始。
                  </div>
                </div>
              </div>
              <div className="mt-8 flex flex-wrap gap-3">
                <button type="button" onClick={handleResumeContinue} className="btn-liquid px-5 py-3 text-white">
                  继续答题
                </button>
                <button
                  type="button"
                  onClick={() => void handleResumeAbandon()}
                  disabled={abandonSessionMutation.isPending}
                  className="rounded-full border border-rose-200 px-5 py-3 text-sm font-bold text-rose-600 disabled:opacity-60 dark:border-rose-500/20"
                >
                  放弃并重开
                </button>
                <button
                  type="button"
                  onClick={() => navigate('/history')}
                  className="rounded-full border border-slate-200 px-5 py-3 text-sm font-bold dark:border-white/10"
                >
                  返回历史
                </button>
              </div>
            </div>
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
            <div className="flex flex-wrap items-center gap-3">
              <button
                type="button"
                onClick={() => void handleRunningAbandon()}
                disabled={isAnswerLocked}
                className="rounded-full border border-rose-200 px-5 py-3 text-sm font-bold text-rose-600 disabled:opacity-60 dark:border-rose-500/20"
              >
                放弃本次
              </button>
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
            </div>
          }
        />

        <SessionFeedbackBanners
          saveMessage={runtime.saveMessage}
          saveErrorMessage={runtime.saveErrorMessage}
          submitErrorMessage={submitErrorMessage}
          submitInfoMessage={submitInfoMessage}
          loadInfoMessage={loadInfoMessage}
          loadError={nextItemQuery.error}
          onRetryLoad={() => void nextItemQuery.refetch()}
        />

        {nextItemQuery.isLoading ? (
          <PanelSkeleton className="min-h-[360px]" />
        ) : nextItemQuery.data?.readyToComplete ? (
          <section className="rounded-[3rem] liquid-glass-panel p-10 edge-light">
            <div className="max-w-2xl">
              <div className="text-xs uppercase tracking-[0.24em] text-amber-500">Ready To Submit</div>
              <h2 className="mt-4 text-3xl font-black text-slate-900 dark:text-white">所有题目已完成</h2>
              <p className="mt-4 leading-7 text-slate-500 dark:text-white/45">
                最后一题已经记录完成。确认交卷后，系统会生成诊断结果页。
              </p>
              <div className="mt-8 flex flex-wrap gap-3">
                <button
                  type="button"
                  onClick={() => void completeSessionMutation.mutateAsync()}
                  disabled={completeSessionMutation.isPending}
                  className="btn-liquid px-6 py-3 text-white disabled:opacity-60"
                >
                  {completeSessionMutation.isPending ? '正在交卷...' : '确认交卷'}
                </button>
                <button
                  type="button"
                  onClick={() => void handleRunningAbandon()}
                  disabled={completeSessionMutation.isPending}
                  className="rounded-full border border-rose-200 px-6 py-3 text-sm font-bold text-rose-600 disabled:opacity-60 dark:border-rose-500/20"
                >
                  放弃本次
                </button>
              </div>
            </div>
          </section>
        ) : !currentItem || staleSubmittedItemVisible ? (
          <div className="rounded-[2rem] border border-slate-200 bg-white/70 px-6 py-8 text-sm text-slate-500 dark:border-white/10 dark:bg-white/5 dark:text-white/45">
            {staleSubmittedItemVisible ? '答案已提交，正在同步下一题或结果页，请稍候...' : '正在收尾诊断结果，请稍候...'}
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
  const canExportPdf = Boolean(result) && !explanationQuery.isLoading;
  const explanationErrorMessage = explanationQuery.error ? getApiErrorMessage(explanationQuery.error) : null;

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
        actions={
          <button
            type="button"
            onClick={() => void handlePdfExport()}
            disabled={!canExportPdf || isPdfExporting}
            className="btn-liquid flex items-center gap-2 px-5 py-3 text-white disabled:cursor-not-allowed disabled:opacity-60"
          >
            <FileText size={14} /> {isPdfExporting ? t('common.actions.exportingPdf') : t('common.actions.exportPdf')}
          </button>
        }
      />

      {resultQuery.error && (
        <div className="rounded-[2rem] border border-rose-500/20 bg-rose-500/5 p-6 text-rose-500">
          {getApiErrorMessage(resultQuery.error)}
        </div>
      )}

      {reportErrorMessage && (
        <div className="rounded-[2rem] border border-rose-500/20 bg-rose-500/5 p-6 text-rose-500">{reportErrorMessage}</div>
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
                          navigate(
                            buildTrainingHref({
                              mode: recommendedMode,
                              source: 'diagnosis-result',
                              diagnosisSummaryId: result.summaryId,
                            })
                          );
                          return;
                        }
                        navigate(buildTrainingHref({ source: 'diagnosis-result', diagnosisSummaryId: result.summaryId }));
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
                  {explanationQuery.data.diagnosisInsight ? (
                    <div className="grid gap-4">
                      <DiagnosisInsightCard
                        title={t('diagnosis.strengths')}
                        items={explanationQuery.data.diagnosisInsight.strengths}
                        toneClassName="border-emerald-500/20 bg-emerald-500/5"
                      />
                      <DiagnosisInsightCard
                        title={t('diagnosis.weaknesses')}
                        items={explanationQuery.data.diagnosisInsight.weaknesses}
                        toneClassName="border-rose-500/20 bg-rose-500/5"
                      />
                      <DiagnosisInsightCard
                        title={t('diagnosis.suggestions')}
                        items={explanationQuery.data.diagnosisInsight.suggestions}
                        toneClassName="border-sky-500/20 bg-sky-500/5"
                      />
                    </div>
                  ) : null}
                  <p className="text-base leading-7 text-slate-800 dark:text-white/85">{explanationQuery.data.explanation}</p>
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
                <div className="text-sm text-rose-500">{explanationErrorMessage}</div>
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
                  <DiagnosisItemReviewCard key={item.itemResultId} item={item} />
                ))}
              </div>
            </section>
          </div>
        </>
      )}

      {result && reportGeneratedAt ? (
        <DiagnosisPdfReport
          reportRef={reportRef}
          generatedAt={reportGeneratedAt}
          result={result}
          explanation={explanationQuery.data}
          explanationErrorMessage={explanationErrorMessage}
        />
      ) : null}
    </div>
  );
};

export default DiagnosisPage;
