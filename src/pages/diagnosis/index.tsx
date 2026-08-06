import React from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import {
  ArrowRight,
  Brain,
  CheckCircle2,
  ChevronRight,
  Clock3,
  FileText,
  Languages,
  Sparkles,
  Target,
  Timer,
} from 'lucide-react';
import { flushSync } from 'react-dom';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { DiagnosisPdfReport } from '@/components/diagnosis/DiagnosisPdfReport';
import { PageHeader, PanelSkeleton, SectionEyebrow } from '@/components/common';
import { EChart } from '@/components/common/EChart';
import { FeedbackState } from '@/components/common/FeedbackState';
import { ConfirmationDialog } from '@/components/common/ConfirmationDialog';
import { getApiErrorMessage, normalizeApiError } from '@/lib/api';
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

function DiagnosisPreparationSteps() {
  const { t } = useTranslation();
  const steps = [
    {
      label: t('diagnosis.preparation.steps.prepare'),
      description: t('diagnosis.preparation.steps.prepareDescription'),
    },
    {
      label: t('diagnosis.preparation.steps.answer'),
      description: t('diagnosis.preparation.steps.answerDescription'),
    },
    {
      label: t('diagnosis.preparation.steps.result'),
      description: t('diagnosis.preparation.steps.resultDescription'),
    },
  ];

  return (
    <nav aria-label={t('diagnosis.preparation.stepperLabel')} className="rounded-[2rem] border border-slate-200/70 bg-white/65 p-4 dark:border-white/10 dark:bg-white/5">
      <ol className="grid gap-3 md:grid-cols-3">
        {steps.map((step, index) => (
          <li
            key={step.label}
            aria-current={index === 0 ? 'step' : undefined}
            className={`flex items-center gap-3 rounded-[1.35rem] border px-4 py-3 ${
              index === 0
                ? 'border-primary/25 bg-primary/10 text-slate-900 dark:text-white'
                : 'border-transparent text-slate-500 dark:text-white/45'
            }`}
          >
            <span
              aria-hidden="true"
              className={`flex size-9 shrink-0 items-center justify-center rounded-full text-sm font-black ${
                index === 0 ? 'bg-primary text-white' : 'bg-slate-100 dark:bg-white/10'
              }`}
            >
              {index + 1}
            </span>
            <span>
              <span className="block text-sm font-black">{step.label}</span>
              <span className="mt-0.5 block text-xs leading-5 opacity-75">{step.description}</span>
            </span>
          </li>
        ))}
      </ol>
    </nav>
  );
}

function DiagnosisPreparationOverview({ restarting = false }: { restarting?: boolean }) {
  const { t } = useTranslation();
  const facts = [
    {
      icon: Clock3,
      title: t('diagnosis.preparation.timeTitle'),
      description: t('diagnosis.preparation.timeDescription'),
    },
    {
      icon: Languages,
      title: t('diagnosis.preparation.measureTitle'),
      description: t('diagnosis.preparation.measureDescription'),
    },
    {
      icon: Sparkles,
      title: t('diagnosis.preparation.outcomeTitle'),
      description: t('diagnosis.preparation.outcomeDescription'),
    },
  ];

  return (
    <section className="liquid-glass-panel relative overflow-hidden rounded-[3rem] p-6 edge-light md:p-10">
      <div
        aria-hidden="true"
        className="pointer-events-none absolute inset-0 opacity-[0.16] dark:opacity-[0.1]"
        style={{
          backgroundImage:
            'linear-gradient(to right, hsl(var(--primary) / 0.22) 1px, transparent 1px), linear-gradient(to bottom, hsl(var(--primary) / 0.16) 1px, transparent 1px)',
          backgroundSize: '44px 44px',
          maskImage: 'linear-gradient(to bottom right, black, transparent 72%)',
        }}
      />
      <div aria-hidden="true" className="pointer-events-none absolute inset-y-0 left-[18%] w-px bg-gradient-to-b from-transparent via-primary/30 to-transparent" />

      <div className="relative">
        <div className="max-w-3xl">
          <div className="inline-flex items-center gap-2 rounded-full border border-primary/20 bg-primary/10 px-4 py-2 text-xs font-black uppercase tracking-[0.22em] text-primary">
            <Target size={15} />
            {t('diagnosis.preparation.eyebrow')}
          </div>
          <h2 className="type-page-title mt-6 text-slate-900 dark:text-white">
            {t(restarting ? 'diagnosis.preparation.restartTitle' : 'diagnosis.preparation.title')}
          </h2>
          <p className="mt-4 max-w-2xl text-base leading-8 text-slate-600 dark:text-white/60">
            {t('diagnosis.preparation.description')}
          </p>
        </div>

        <div className="mt-8 grid gap-4 lg:grid-cols-3">
          {facts.map(({ icon: Icon, title, description }) => (
            <article key={title} className="rounded-[1.65rem] border border-white/70 bg-white/75 p-5 shadow-sm dark:border-white/10 dark:bg-slate-950/45">
              <div className="flex size-10 items-center justify-center rounded-2xl bg-primary/10 text-primary">
                <Icon size={20} />
              </div>
              <h3 className="mt-4 font-black text-slate-900 dark:text-white">{title}</h3>
              <p className="mt-2 text-sm leading-6 text-slate-500 dark:text-white/50">{description}</p>
            </article>
          ))}
        </div>
      </div>
    </section>
  );
}

const DiagnosisPage: React.FC = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const queryClient = useQueryClient();
  const [state, dispatch] = React.useReducer(diagnosisFlowReducer, initialDiagnosisFlowState);
  const shownAtRef = React.useRef<number>(0);
  const answerRequestRef = React.useRef<SubmitDiagnosisAnswerRequest | null>(null);
  const [submitErrorMessage, setSubmitErrorMessage] = React.useState<string | null>(null);
  const [submitInfoMessage, setSubmitInfoMessage] = React.useState<string | null>(null);
  const [loadInfoMessage, setLoadInfoMessage] = React.useState<string | null>(null);
  const [pendingNextItemId, setPendingNextItemId] = React.useState<number | null>(null);
  const [reportErrorMessage, setReportErrorMessage] = React.useState<string | null>(null);
  const [isPdfExporting, setIsPdfExporting] = React.useState(false);
  const [reportGeneratedAt, setReportGeneratedAt] = React.useState<string | null>(null);
  const [resumeCandidate, setResumeCandidate] = React.useState<DiagnosisHistorySummaryVO | null>(null);
  const [abandonConfirmSessionId, setAbandonConfirmSessionId] = React.useState<number | null>(null);
  const [isRestarting, setIsRestarting] = React.useState(false);
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
    enabled: state.phase === 'select' && !resumeCandidate && !historyQuery.error,
  });

  const createSessionMutation = useMutation({
    mutationFn: (templateId: number) =>
      diagnosisSessionService.create({
        templateId,
        launchSource: requestedSource || undefined,
        sourceSummaryId: requestedSourceSummaryId,
      }),
    onSuccess: (created) => {
      setSearchParams(clearDiagnosisLaunchParams(searchParams), { replace: true });
      setIsRestarting(false);
      dispatch({ type: 'startSession', sessionId: created.sessionId });
      void queryClient.invalidateQueries({ queryKey: ['diagnosis-history'] });
    },
    onError: (error) => {
      if (normalizeApiError(error).code === 'ACTIVE_SESSION_EXISTS') {
        void historyQuery.refetch();
      }
    },
  });
  const activeSessionConflict =
    !!createSessionMutation.error && normalizeApiError(createSessionMutation.error).code === 'ACTIVE_SESSION_EXISTS';

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
        return;
      }
      if (refreshed.data?.item && refreshed.data.item.itemResultId !== payload.itemResultId) {
        setSubmitErrorMessage(null);
        setSubmitInfoMessage('答案已提交，系统已同步到下一题。');
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
    refetchInterval: (query) => {
      const status = query.state.data?.completionHooksStatus;
      if (status === 'PENDING' || status === 'IN_PROGRESS') {
        return 1500;
      }
      return status === 'FAILED' ? 5000 : false;
    },
  });

  const resultSummaryId = resultQuery.data?.summaryId;
  const explanationQuery = useQuery({
    queryKey: ['diagnosis-explanation', resultSummaryId],
    queryFn: ({ signal }) => aiService.explainDiagnosisAsync(resultSummaryId, { signal }),
    enabled: state.phase === 'result' && !!resultSummaryId,
    retry: false,
  });

  const recommendedPlanQuery = useQuery({
    queryKey: ['recommended-training-plan', resultSummaryId],
    queryFn: ({ signal }) => trainingService.getRecommendedPlan({ diagnosisSummaryId: resultSummaryId }, { signal }),
    enabled:
      state.phase === 'result' &&
      !!resultSummaryId &&
      (!resultQuery.data?.completionHooksStatus || resultQuery.data.completionHooksStatus === 'DONE'),
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
      setIsRestarting(true);
      dispatch({ type: 'reset' });
      await queryClient.invalidateQueries({ queryKey: ['diagnosis-history'] });
    },
    onError: (error) => {
      setSubmitErrorMessage(getApiErrorMessage(error));
    },
  });

  React.useEffect(() => {
    if (state.phase !== 'running' || !nextItemQuery.data?.readyToComplete || completeSessionMutation.isPending) {
      return;
    }
    completeSessionMutation.mutate();
  }, [completeSessionMutation.isPending, completeSessionMutation.mutate, nextItemQuery.data?.readyToComplete, state.phase]);

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

  React.useLayoutEffect(() => {
    if (!currentItem) {
      shownAtRef.current = 0;
      return;
    }
    shownAtRef.current = window.performance.now();
  }, [currentItem?.itemResultId]);

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
      const elapsedMs = shownAtRef.current > 0 ? window.performance.now() - shownAtRef.current : 1;
      const reactionTimeMs = Math.min(2_147_483_647, Math.max(1, Math.round(elapsedMs)));
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
    await submitAnswerMutation.mutateAsync(request);
  };

  const handleResumeContinue = () => {
    if (!resumeCandidate) {
      return;
    }
    dispatch({ type: 'resumeSession', sessionId: resumeCandidate.sessionId });
    setResumeCandidate(null);
  };

  const handleResumeAbandon = () => {
    if (!resumeCandidate) {
      return;
    }
    setAbandonConfirmSessionId(resumeCandidate.sessionId);
  };

  const handleRunningAbandon = () => {
    if (!state.sessionId) {
      return;
    }
    setAbandonConfirmSessionId(state.sessionId);
  };

  const abandonConfirmation = (
    <ConfirmationDialog
      open={abandonConfirmSessionId !== null}
      title="确认放弃当前诊断？"
      description="系统将结束这次未完成诊断，并把会话标记为已放弃。"
      safety="已完成的历史诊断不会受影响，但本次未完成内容不能再从当前进度继续。"
      nextStep="如需保留当前进度请取消；仅在确定重新开始时确认放弃。"
      confirmLabel="确认放弃诊断"
      cancelLabel="取消，保留进度"
      pending={abandonSessionMutation.isPending}
      pendingTitle="正在结束诊断"
      pendingDescription="放弃请求已经提交，请等待服务器确认。"
      onCancel={() => setAbandonConfirmSessionId(null)}
      onConfirm={() => {
        if (abandonConfirmSessionId !== null) {
          abandonSessionMutation.mutate(abandonConfirmSessionId, {
            onSettled: () => setAbandonConfirmSessionId(null),
          });
        }
      }}
    />
  );

  if (state.phase === 'boot' || historyQuery.isLoading) {
    return (
      <div className="space-y-8">
        <PageHeader title={t('diagnosis.selectTitle')} subtitle={t('diagnosis.selectSubtitle')} />
        <DiagnosisPreparationSteps />
        <DiagnosisPreparationOverview />
        <section aria-labelledby="diagnosis-next-action-loading" className="space-y-4">
          <div>
            <SectionEyebrow>{t('diagnosis.preparation.actionEyebrow')}</SectionEyebrow>
            <h2 id="diagnosis-next-action-loading" className="mt-2 text-2xl font-black text-slate-900 dark:text-white">
              {t('diagnosis.preparation.actionTitle')}
            </h2>
          </div>
          <FeedbackState
            kind="loading"
            title={t('diagnosis.preparation.bootTitle')}
            description={t('diagnosis.preparation.bootDescription')}
            impact={t('diagnosis.preparation.bootImpact')}
            nextStep={t('diagnosis.preparation.bootNextStep')}
          />
        </section>
      </div>
    );
  }

  if (state.phase === 'select') {
    return (
      <div className="space-y-8">
        <PageHeader title={t('diagnosis.selectTitle')} subtitle={t('diagnosis.selectSubtitle')} />
        <DiagnosisPreparationSteps />
        <DiagnosisPreparationOverview restarting={isRestarting} />
        <SessionFeedbackBanners
          submitErrorMessage={
            submitErrorMessage ||
            (createSessionMutation.error && !activeSessionConflict && !resumeCandidate
              ? getApiErrorMessage(createSessionMutation.error)
              : null)
          }
        />
        <section aria-labelledby="diagnosis-next-action" className="space-y-5">
          <div className="max-w-3xl">
            <SectionEyebrow>{t('diagnosis.preparation.actionEyebrow')}</SectionEyebrow>
            <h2 id="diagnosis-next-action" className="mt-2 text-3xl font-black tracking-tight text-slate-900 dark:text-white">
              {t(isRestarting ? 'diagnosis.preparation.restartActionTitle' : 'diagnosis.preparation.actionTitle')}
            </h2>
            <p className="mt-3 text-sm leading-6 text-slate-500 dark:text-white/50">
              {t('diagnosis.preparation.actionDescription')}
            </p>
          </div>

          {resumeCandidate ? (
            <div className="space-y-3">
              <FeedbackState
                kind="saved"
                title={t('diagnosis.preparation.resume.title')}
                description={t('diagnosis.preparation.resume.description', {
                  lastSavedAt: resumeCandidate.lastSavedAt
                    ? formatDateTime(resumeCandidate.lastSavedAt)
                    : t('diagnosis.preparation.resume.unknownTime'),
                })}
                impact={t('diagnosis.preparation.resume.impact')}
                nextStep={t('diagnosis.preparation.resume.nextStep')}
                primaryAction={{
                  label: t('diagnosis.preparation.resume.continue'),
                  onClick: handleResumeContinue,
                  disabled: abandonSessionMutation.isPending,
                }}
                secondaryAction={{
                  label: t('diagnosis.preparation.resume.abandon'),
                  onClick: handleResumeAbandon,
                  tone: 'danger',
                  disabled: abandonSessionMutation.isPending,
                }}
              />
              <button
                type="button"
                onClick={() => navigate('/history')}
                className="rounded-full border border-slate-200 px-5 py-3 text-sm font-bold text-slate-700 transition-colors hover:border-primary/30 hover:text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/60 dark:border-white/10 dark:text-white/70"
              >
                {t('diagnosis.preparation.viewHistory')}
              </button>
            </div>
          ) : activeSessionConflict ? (
            <FeedbackState
              kind={historyQuery.isFetching ? 'loading' : 'error'}
              title={t('diagnosis.preparation.activeSessionTitle')}
              description={t('diagnosis.preparation.activeSessionDescription')}
              impact={t('diagnosis.preparation.activeSessionImpact')}
              nextStep={t('diagnosis.preparation.activeSessionNextStep')}
              primaryAction={
                historyQuery.isFetching
                  ? undefined
                  : {
                      label: t('diagnosis.preparation.retryHistory'),
                      onClick: () => void historyQuery.refetch(),
                    }
              }
            />
          ) : historyQuery.error ? (
            <FeedbackState
              kind="error"
              title={t('diagnosis.preparation.historyErrorTitle')}
              description={getApiErrorMessage(historyQuery.error)}
              impact={t('diagnosis.preparation.historyErrorImpact')}
              nextStep={t('diagnosis.preparation.historyErrorNextStep')}
              primaryAction={{
                label: t(historyQuery.isFetching ? 'diagnosis.preparation.retrying' : 'diagnosis.preparation.retryHistory'),
                onClick: () => void historyQuery.refetch(),
                disabled: historyQuery.isFetching,
              }}
            />
          ) : templatesQuery.error ? (
            <FeedbackState
              kind="error"
              title={t('diagnosis.preparation.templateErrorTitle')}
              description={getApiErrorMessage(templatesQuery.error)}
              impact={t('diagnosis.preparation.templateErrorImpact')}
              nextStep={t('diagnosis.preparation.templateErrorNextStep')}
              primaryAction={{
                label: t(templatesQuery.isFetching ? 'diagnosis.preparation.retrying' : 'diagnosis.preparation.retryTemplates'),
                onClick: () => void templatesQuery.refetch(),
                disabled: templatesQuery.isFetching,
              }}
            />
          ) : templatesQuery.isLoading ? (
            <PanelSkeleton className="min-h-[280px]" />
          ) : !templatesQuery.data?.records.length ? (
            <FeedbackState
              kind="empty"
              title={t('diagnosis.preparation.emptyTitle')}
              description={t('diagnosis.noTemplates')}
              impact={t('diagnosis.preparation.emptyImpact')}
              nextStep={t('diagnosis.preparation.emptyNextStep')}
              primaryAction={{
                label: t('diagnosis.preparation.backDashboard'),
                onClick: () => navigate('/dashboard'),
              }}
              secondaryAction={{
                label: t('diagnosis.preparation.viewHistory'),
                onClick: () => navigate('/history'),
              }}
            />
          ) : (
            <div className="grid gap-6 lg:grid-cols-2">
              {templatesQuery.data.records.map((template) => {
                const descriptionId = `diagnosis-template-${template.id}-description`;
                const isStartingThisTemplate = createSessionMutation.isPending && createSessionMutation.variables === template.id;

                return (
                  <article key={template.id} className="liquid-glass rounded-[2.4rem] p-7 edge-light">
                    <div className="flex items-start justify-between gap-4">
                      <div>
                        <div className="inline-flex items-center gap-2 text-xs font-black uppercase tracking-[0.2em] text-primary">
                          <Brain size={15} />
                          {t('diagnosis.preparation.templateEyebrow')}
                        </div>
                        <h3 className="mt-3 text-2xl font-black text-slate-900 dark:text-white">
                          {template.templateName}
                        </h3>
                        <p id={descriptionId} className="mt-3 text-sm leading-6 text-slate-500 dark:text-white/45">
                          {template.description || t('diagnosis.noDescription')}
                        </p>
                      </div>
                      <span className="shrink-0 rounded-full border border-emerald-500/20 bg-emerald-500/10 px-3 py-1 text-[10px] font-black uppercase tracking-[0.16em] text-emerald-700 dark:text-emerald-300">
                        {template.status}
                      </span>
                    </div>
                    <div className="mt-6 flex flex-wrap gap-2 text-sm text-slate-600 dark:text-white/55">
                      <span className="rounded-full bg-slate-100 px-3 py-1.5 dark:bg-white/10">
                        {t('diagnosis.statusTemplateCount', { count: template.itemCount })}
                      </span>
                      <span className="rounded-full bg-slate-100 px-3 py-1.5 dark:bg-white/10">
                        {t('diagnosis.statusDurationMinutes', { count: template.estimatedDurationMinutes })}
                      </span>
                      {template.targetClassName ? (
                        <span className="rounded-full bg-slate-100 px-3 py-1.5 dark:bg-white/10">
                          {t('diagnosis.preparation.targetClass', { className: template.targetClassName })}
                        </span>
                      ) : null}
                    </div>
                    <button
                      type="button"
                      aria-describedby={descriptionId}
                      disabled={createSessionMutation.isPending}
                      onClick={() => createSessionMutation.mutate(template.id)}
                      className="btn-liquid mt-7 inline-flex w-full items-center justify-center gap-2 px-5 py-3 text-sm font-black text-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/60 focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-55"
                    >
                      {t(
                        isStartingThisTemplate
                          ? 'diagnosis.preparation.startingTemplate'
                          : 'diagnosis.preparation.startTemplate'
                      )}
                      <ArrowRight size={17} aria-hidden="true" />
                    </button>
                  </article>
                );
              })}
            </div>
          )}
        </section>
        {abandonConfirmation}
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
              <SessionSaveActions
                isSaving={runtime.isSaving}
                disabled={isAnswerLocked}
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
          isSaving={runtime.isSaving}
          saveMessage={runtime.saveMessage}
          saveErrorMessage={runtime.saveErrorMessage}
          saveConflictMessage={runtime.saveConflictMessage}
          submitErrorMessage={submitErrorMessage}
          submitInfoMessage={submitInfoMessage}
          loadInfoMessage={loadInfoMessage}
          loadError={nextItemQuery.error}
          onRetrySave={() => void runtime.saveProgressManually()}
          onRetrySubmit={() => {
            if (answerRequestRef.current) {
              void submitAnswerMutation.mutateAsync(answerRequestRef.current);
            }
          }}
          onRetryLoad={() => void nextItemQuery.refetch()}
        />

        {nextItemQuery.isLoading ? (
          <PanelSkeleton className="min-h-[360px]" />
        ) : nextItemQuery.data?.readyToComplete ? (
          <section className="rounded-[3rem] liquid-glass-panel p-10 edge-light">
            <div className="max-w-2xl">
              <div className="text-xs uppercase tracking-[0.24em] text-amber-500">Generating Result</div>
              <h2 className="type-section-title mt-4 text-slate-900 dark:text-white">正在生成诊断结果</h2>
              <p className="mt-4 leading-7 text-slate-500 dark:text-white/45">
                所有答案均已记录，系统正在完成评分并生成结果，请稍候。
              </p>
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
              currentItem={nextItemQuery.data?.currentItemOrder}
              label={t('diagnosis.progress', {
                current: nextItemQuery.data?.currentItemOrder || 0,
                total: nextItemQuery.data?.totalItems || 0,
              })}
              answeredItems={nextItemQuery.data?.answeredItems}
              totalItems={nextItemQuery.data?.totalItems}
              savedState={runtime.saveConflictMessage ? 'conflict' : runtime.saveErrorMessage ? 'error' : runtime.isSaving ? 'saving' : runtime.saveMessage ? 'saved' : 'idle'}
              onExit={handleRunningAbandon}
              exitLabel="退出本次诊断"
              exitDisabled={isAnswerLocked}
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
        {abandonConfirmation}
      </div>
    );
  }

  const result = resultQuery.data;
  const completionHooksPending =
    result?.completionHooksStatus === 'PENDING' || result?.completionHooksStatus === 'IN_PROGRESS';
  const completionHooksFailed = result?.completionHooksStatus === 'FAILED';
  const completionHooksReady = !result?.completionHooksStatus || result.completionHooksStatus === 'DONE';
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

      {completionHooksPending && (
        <div className="rounded-[2rem] border border-sky-500/20 bg-sky-500/10 p-6 text-sm text-sky-800 dark:text-sky-200" aria-live="polite">
          诊断结果已生成，学习档案和推荐训练正在更新。更新完成前暂不可开始推荐训练。
        </div>
      )}

      {completionHooksFailed && (
        <div className="rounded-[2rem] border border-amber-500/20 bg-amber-500/10 p-6 text-sm text-amber-800 dark:text-amber-200" role="alert">
          核心诊断结果不受影响，但学习档案更新暂时失败，后台将自动重试。
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
                <h2 className="type-page-title mt-5 text-slate-900 dark:text-white">
                  {t('diagnosis.completedTitle')}
                </h2>
                <p className="mt-4 leading-7 text-slate-500 dark:text-white/45">
                  {t('diagnosis.completedDescription')}
                </p>
              </div>
                  <div className="flex flex-wrap gap-3">
                    <button
                      type="button"
                      disabled={!completionHooksReady}
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
                      className="btn-liquid px-6 py-3 text-white disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      开始推荐训练
                    </button>
                    <button
                      type="button"
                      onClick={() => {
                        setIsRestarting(true);
                        dispatch({ type: 'reset' });
                      }}
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
              <div className="type-numeric mt-3 text-3xl font-semibold text-slate-900 dark:text-white">
                {formatMaybePercent(result.metrics.positiveTransferScore)}
              </div>
            </div>
            <div className="rounded-[2rem] liquid-glass p-6">
              <div className="text-xs uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">
                {t('diagnosis.metrics.negativeTransferRisk')}
              </div>
              <div className="type-numeric mt-3 text-3xl font-semibold text-rose-500">
                {formatMaybePercent(result.metrics.negativeTransferRisk)}
              </div>
            </div>
            <div className="rounded-[2rem] liquid-glass p-6">
              <div className="text-xs uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">
                {t('diagnosis.metrics.semanticDiscrimination')}
              </div>
              <div className="type-numeric mt-3 text-3xl font-semibold text-slate-900 dark:text-white">
                {formatMaybePercent(result.metrics.semanticDiscrimination)}
              </div>
            </div>
            <div className="rounded-[2rem] liquid-glass p-6">
              <div className="text-xs uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">
                {t('diagnosis.metrics.averageReactionTime')}
              </div>
              <div className="type-numeric mt-3 text-3xl font-semibold text-slate-900 dark:text-white">
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
