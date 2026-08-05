import React from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { AlertTriangle, Award, Brain, Clock3, Rocket } from 'lucide-react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { PageHeader, PanelSkeleton } from '@/components/common';
import { FeedbackState } from '@/components/common/FeedbackState';
import { ConfirmationDialog } from '@/components/common/ConfirmationDialog';
import { TrainingModeSummaryCard } from '@/components/common/TrainingModeSummaryCard';
import { buildDiagnosisHref } from '@/lib/diagnosis-launch';
import { aiService, trainingService } from '@/lib/services';
import { errorTypeLabel, formatDateTime, formatMaybePercent, formatMs, lexicalPairTypeLabel, trainingModeLabel } from '@/lib/format';
import { getApiErrorMessage, normalizeApiError } from '@/lib/api';
import { useBodyScrollLock, useDialogAccessibility } from '@/lib/a11y';
import { buildTrainingHref, clearTrainingLaunchParams, parseTrainingLaunchNumber, type TrainingLaunchParams } from '@/lib/training-launch';
import type {
  SessionCompletionHookStatus,
  SubmitTrainingAnswerRequest,
  TrainingAnswerOutcomeVO,
  TrainingHistorySummaryVO,
  TrainingItemResultDetailVO,
  TrainingMode,
  TrainingOptionViewVO,
  TrainingSessionProgressVO,
} from '@/lib/contracts';
import { SessionFeedbackBanners, SessionOptionButton, SessionProgressHeader, SessionSaveActions } from '@/features/session-runtime/components';
import { HESITATION_BASELINE_MS, NEXT_ITEM_RETRY_DELAY_MS, SLOW_NEXT_ITEM_NOTICE_DELAY_MS } from '@/features/session-runtime/constants';
import { buildSessionSnapshot } from '@/features/session-runtime/helpers';
import { useSessionRuntime } from '@/features/session-runtime/useSessionRuntime';
import { initialTrainingFlowState, trainingFlowReducer } from './flow';

type SessionLaunchContext = Omit<TrainingLaunchParams, 'mode'>;
type TrainingAnswerFeedback = {
  itemResultId: number;
  mode: TrainingMode;
  outcome: TrainingAnswerOutcomeVO;
  progress: TrainingSessionProgressVO;
};

const TRAINING_MODES: readonly TrainingMode[] = [
  'COGNATE_BOOST',
  'FALSE_FRIEND_DISCRIM',
  'CONTEXT_FIX',
  'SPEED_CHALLENGE',
];

function parseRequestedTrainingMode(value: string | null): TrainingMode | null {
  if (!value) {
    return null;
  }
  return TRAINING_MODES.includes(value as TrainingMode) ? (value as TrainingMode) : null;
}

function findTrainingOptionLabel(options: TrainingOptionViewVO[], answerKey?: string | null) {
  if (!answerKey) {
    return null;
  }
  return options.find((option) => option.key === answerKey)?.label || answerKey;
}

function adaptationActionLabel(action?: string | null) {
  switch (action) {
    case 'ESCALATE_FALSE_FRIEND':
      return '加强假朋友词辨析训练';
    case 'BOOST_CONTEXT':
      return '加强语境线索训练';
    case 'BOOST_SPEED':
      return '进入速度巩固训练';
    case 'QUEUE_REVIEW':
      return '加入后续复习';
    case 'KEEP_STABLE':
      return '保持当前训练节奏';
    default:
      return action || null;
  }
}

function TrainingItemReviewCard({ item }: { item: TrainingItemResultDetailVO }) {
  const selectedLabel = findTrainingOptionLabel(item.options, item.selectedAnswerKey);
  const correctLabel = findTrainingOptionLabel(item.options, item.correctAnswerKey);
  const outcomeLabel = item.correct == null ? '未判定' : item.correct ? '答对' : '答错';
  const englishWord = item.englishWord || '--';
  const frenchWord = item.frenchWord || '--';

  return (
    <div className="rounded-[1.6rem] border border-slate-200/70 bg-white/60 p-4 dark:border-white/10 dark:bg-white/5">
      <div className="flex items-start justify-between gap-4">
        <div>
          <div className="font-black text-slate-900 dark:text-white">
            {englishWord} / {frenchWord}
          </div>
          <div className="mt-2 text-sm leading-6 text-slate-500 dark:text-white/45">
            {trainingModeLabel(item.mode)} · {item.cognitiveTag} · {item.detectedErrorType ? errorTypeLabel(item.detectedErrorType) : '已完成'}
          </div>
          <div className="mt-3 rounded-[1.2rem] border border-dashed border-slate-200/80 px-4 py-3 text-sm text-slate-600 dark:border-white/10 dark:text-white/60">
            <div className="font-semibold">{item.content.question}</div>
            {item.content.sentence && <div className="mt-2 italic">{item.content.sentence}</div>}
            {item.stimulus.explanation && <div className="mt-2">{item.stimulus.explanation}</div>}
          </div>
        </div>
        <div className="text-right text-sm text-slate-500 dark:text-white/45">
          <div>{outcomeLabel}</div>
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

const TrainingPage: React.FC = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const queryClient = useQueryClient();
  const [state, dispatch] = React.useReducer(trainingFlowReducer, initialTrainingFlowState);
  const shownAtRef = React.useRef<number>(0);
  const answerRequestRef = React.useRef<SubmitTrainingAnswerRequest | null>(null);
  const autoStartKeyRef = React.useRef<string | null>(null);
  const [submitErrorMessage, setSubmitErrorMessage] = React.useState<string | null>(null);
  const [submitInfoMessage, setSubmitInfoMessage] = React.useState<string | null>(null);
  const [loadInfoMessage, setLoadInfoMessage] = React.useState<string | null>(null);
  const [pendingNextItemId, setPendingNextItemId] = React.useState<number | null>(null);
  const [answerFeedback, setAnswerFeedback] = React.useState<TrainingAnswerFeedback | null>(null);
  const [completionRefreshSessionId, setCompletionRefreshSessionId] = React.useState<number | null>(null);
  const [resumeCandidate, setResumeCandidate] = React.useState<TrainingHistorySummaryVO | null>(null);
  const [abandonConfirmSessionId, setAbandonConfirmSessionId] = React.useState<number | null>(null);
  const resumeDialogRef = React.useRef<HTMLDivElement | null>(null);
  const resumeContinueButtonRef = React.useRef<HTMLButtonElement | null>(null);

  const requestedMode = React.useMemo(() => parseRequestedTrainingMode(searchParams.get('mode')), [searchParams]);
  const requestedSource = searchParams.get('source');
  const requestedDiagnosisSummaryId = parseTrainingLaunchNumber(searchParams.get('diagnosisSummaryId'));
  const requestedLexicalPairId = parseTrainingLaunchNumber(searchParams.get('lexicalPairId'));
  const requestedWrongBookId = parseTrainingLaunchNumber(searchParams.get('wrongBookId'));
  const requestedReviewScheduleId = parseTrainingLaunchNumber(searchParams.get('reviewScheduleId'));
  const closeResumeDialog = React.useCallback(() => setResumeCandidate(null), []);
  useBodyScrollLock(!!resumeCandidate);
  useDialogAccessibility({
    open: !!resumeCandidate,
    containerRef: resumeDialogRef,
    initialFocusRef: resumeContinueButtonRef,
    onClose: closeResumeDialog,
  });
  const baseLaunchParams = React.useMemo<SessionLaunchContext>(
    () => ({
      source: requestedSource,
      diagnosisSummaryId: requestedDiagnosisSummaryId,
      lexicalPairId: requestedLexicalPairId,
      wrongBookId: requestedWrongBookId,
      reviewScheduleId: requestedReviewScheduleId,
    }),
    [
      requestedDiagnosisSummaryId,
      requestedLexicalPairId,
      requestedReviewScheduleId,
      requestedSource,
      requestedWrongBookId,
    ]
  );

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
      dispatch({ type: 'readyHome' });
      setResumeCandidate(inProgress);
      return;
    }
    setResumeCandidate(null);
    dispatch({ type: 'readyHome' });
  }, [historyQuery.data]);

  React.useEffect(() => {
    if (historyQuery.error) {
      dispatch({ type: 'readyHome' });
    }
  }, [historyQuery.error]);

  const recommendedPlanQuery = useQuery({
    queryKey: ['recommended-training-plan', requestedDiagnosisSummaryId],
    queryFn: ({ signal }) =>
      trainingService.getRecommendedPlan({ diagnosisSummaryId: requestedDiagnosisSummaryId }, { signal }),
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
    mutationFn: (payload: { mode: TrainingMode } & SessionLaunchContext) =>
      trainingService.startSession({
        planId: recommendedPlanQuery.data!.planId,
        mode: payload.mode,
        launchSource: payload.source || undefined,
        diagnosisSummaryId: payload.diagnosisSummaryId,
        lexicalPairId: payload.lexicalPairId,
        wrongBookId: payload.wrongBookId,
        reviewScheduleId: payload.reviewScheduleId,
      }),
    onSuccess: (created) => {
      dispatch({ type: 'startSession', sessionId: created.sessionId });
      void queryClient.invalidateQueries({ queryKey: ['training-history'] });
    },
    onError: (error) => {
      if (normalizeApiError(error).code === 'ACTIVE_SESSION_EXISTS') {
        void historyQuery.refetch();
      }
    },
  });

  const nextItemQuery = useQuery({
    queryKey: ['training-next-item', state.sessionId],
    queryFn: ({ signal }) => trainingService.getNextItem(state.sessionId as number, { signal }),
    enabled: state.phase === 'running' && !!state.sessionId,
    retry: 1,
    retryDelay: NEXT_ITEM_RETRY_DELAY_MS,
  });

  const invalidateCompletionDependentQueries = React.useCallback(async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['student-overview'] }),
      queryClient.invalidateQueries({ queryKey: ['student-trends'] }),
      queryClient.invalidateQueries({ queryKey: ['wrong-book'] }),
      queryClient.invalidateQueries({ queryKey: ['review-schedule'] }),
    ]);
  }, [queryClient]);

  const completionHooksQuery = useQuery({
    queryKey: ['training-completion-hooks', completionRefreshSessionId],
    queryFn: ({ signal }) => trainingService.getNextItem(completionRefreshSessionId as number, { signal }),
    enabled: completionRefreshSessionId != null,
    retry: 1,
    refetchInterval: (query) => {
      const status = query.state.data?.completionHooksStatus;
      if (status === 'DONE') {
        return false;
      }
      return status === 'FAILED' ? 5000 : 1500;
    },
  });

  React.useEffect(() => {
    if (!completionRefreshSessionId) {
      return;
    }
    const status = completionHooksQuery.data?.completionHooksStatus;
    if (status === 'DONE') {
      setCompletionRefreshSessionId(null);
      void invalidateCompletionDependentQueries();
      return;
    }
    if (status === 'FAILED') {
      return;
    }
  }, [completionHooksQuery.data?.completionHooksStatus, completionRefreshSessionId, invalidateCompletionDependentQueries]);

  const markCompleted = React.useCallback((sessionId: number, completionHooksStatus?: SessionCompletionHookStatus | null) => {
    dispatch({ type: 'showSummary', sessionId });
    void queryClient.invalidateQueries({ queryKey: ['training-history'] });
    if (!completionHooksStatus || completionHooksStatus === 'DONE') {
      setCompletionRefreshSessionId(null);
      void invalidateCompletionDependentQueries();
      return;
    }
    if (completionHooksStatus === 'PENDING' || completionHooksStatus === 'IN_PROGRESS') {
      setCompletionRefreshSessionId(sessionId);
      return;
    }
    setCompletionRefreshSessionId(null);
  }, [invalidateCompletionDependentQueries, queryClient]);

  const continueAfterFeedback = React.useCallback(async () => {
    if (!answerFeedback) {
      return;
    }
    const { itemResultId, progress } = answerFeedback;
    setAnswerFeedback(null);
    answerRequestRef.current = null;
    if (progress.completed) {
      setPendingNextItemId(null);
      setSubmitInfoMessage(null);
      markCompleted(progress.sessionId, progress.completionHooksStatus);
      return;
    }

    setPendingNextItemId(itemResultId);
    const refreshed = await nextItemQuery.refetch();
    if (refreshed.error) {
      setSubmitInfoMessage('答案已提交，但下一题加载失败。请重试加载当前题，系统不会重复计入本题。');
      return;
    }
    setPendingNextItemId(null);
    setSubmitInfoMessage(null);
  }, [answerFeedback, markCompleted, nextItemQuery]);

  const answerMutation = useMutation({
    mutationFn: (payload: SubmitTrainingAnswerRequest) =>
      trainingService.submitAnswer(state.sessionId as number, payload),
    onSuccess: (submission, payload) => {
      setSubmitErrorMessage(null);
      setSubmitInfoMessage(null);
      setAnswerFeedback({
        itemResultId: payload.itemResultId,
        mode: nextItemQuery.data?.mode || 'COGNATE_BOOST',
        outcome: submission.outcome,
        progress: submission.progress,
      });
    },
    onError: async (error, payload) => {
      const refreshed = await nextItemQuery.refetch();
      if (refreshed.data?.sessionStatus === 'COMPLETED') {
        answerRequestRef.current = null;
        setSubmitErrorMessage(null);
        setSubmitInfoMessage('答案已提交，系统已同步到最新总结。');
        markCompleted(refreshed.data.sessionId, refreshed.data.completionHooksStatus);
        return;
      }
      if (refreshed.data?.readyToComplete) {
        setPendingNextItemId(null);
        setSubmitErrorMessage(null);
        setSubmitInfoMessage('答案已提交，请确认交卷。');
        return;
      }
      if (refreshed.data?.item && refreshed.data.item.itemResultId !== payload.itemResultId) {
        setPendingNextItemId(null);
        setSubmitErrorMessage(null);
        setSubmitInfoMessage('答案已提交，系统已同步到下一题。');
        return;
      }
      setSubmitInfoMessage(null);
      setSubmitErrorMessage(getApiErrorMessage(error));
    },
  });

  const summaryQuery = useQuery({
    queryKey: ['training-summary', state.summarySessionId],
    queryFn: ({ signal }) => trainingService.getSummary(state.summarySessionId as number, { signal }),
    enabled: state.phase === 'summary' && !!state.summarySessionId,
  });

  const completeMutation = useMutation({
    mutationFn: () => trainingService.complete(state.sessionId as number),
    onSuccess: (progress) => {
      answerRequestRef.current = null;
      setPendingNextItemId(null);
      setSubmitErrorMessage(null);
      setSubmitInfoMessage(null);
      markCompleted(progress.sessionId, progress.completionHooksStatus);
    },
    onError: async (error) => {
      const refreshed = await nextItemQuery.refetch();
      if (refreshed.data?.sessionStatus === 'COMPLETED') {
        markCompleted(refreshed.data.sessionId, refreshed.data.completionHooksStatus);
        return;
      }
      setSubmitErrorMessage(getApiErrorMessage(error));
    },
  });

  const abandonMutation = useMutation({
    mutationFn: (sessionId: number) => trainingService.abandon(sessionId),
    onSuccess: async () => {
      answerRequestRef.current = null;
      setPendingNextItemId(null);
      setSubmitErrorMessage(null);
      setSubmitInfoMessage(null);
      setResumeCandidate(null);
      setCompletionRefreshSessionId(null);
      dispatch({ type: 'resetHome' });
      await queryClient.invalidateQueries({ queryKey: ['training-history'] });
      await queryClient.invalidateQueries({ queryKey: ['recommended-training-plan'] });
    },
    onError: (error) => {
      setSubmitErrorMessage(getApiErrorMessage(error));
    },
  });

  React.useEffect(() => {
    if (state.phase !== 'running' || !nextItemQuery.data?.readyToComplete || completeMutation.isPending) {
      return;
    }
    completeMutation.mutate();
  }, [completeMutation.isPending, completeMutation.mutate, nextItemQuery.data?.readyToComplete, state.phase]);

  const clearTrainingIntent = React.useCallback(() => {
    if (
      !searchParams.get('mode') &&
      !searchParams.get('source') &&
      !searchParams.get('diagnosisSummaryId') &&
      !searchParams.get('lexicalPairId') &&
      !searchParams.get('wrongBookId') &&
      !searchParams.get('reviewScheduleId')
    ) {
      return;
    }
    setSearchParams(clearTrainingLaunchParams(searchParams), { replace: true });
  }, [searchParams, setSearchParams]);

  const startSessionForMode = React.useCallback(async (mode: TrainingMode, overrides?: Partial<SessionLaunchContext>) => {
    try {
      await startMutation.mutateAsync({
        mode,
        ...baseLaunchParams,
        ...overrides,
      });
    } finally {
      clearTrainingIntent();
    }
  }, [baseLaunchParams, clearTrainingIntent, startMutation]);

  React.useEffect(() => {
    if (state.phase !== 'running' || !nextItemQuery.data || nextItemQuery.data.hasNextItem) {
      return;
    }
    if (nextItemQuery.data.sessionStatus === 'COMPLETED') {
      answerRequestRef.current = null;
      markCompleted(nextItemQuery.data.sessionId, nextItemQuery.data.completionHooksStatus);
      return;
    }
    if (nextItemQuery.data.sessionStatus === 'ABANDONED') {
      answerRequestRef.current = null;
      setSubmitInfoMessage(null);
      setSubmitErrorMessage('当前训练会话已被系统废弃，请返回重新开始。');
      dispatch({ type: 'resetHome' });
    }
  }, [markCompleted, nextItemQuery.data, state.phase]);

  const runtime = useSessionRuntime({
    active: state.phase === 'running',
    sessionId: state.sessionId,
    nextItem: nextItemQuery.data,
    refetchCurrent: nextItemQuery.refetch,
    buildSnapshot: (sessionId, nextItem) => buildSessionSnapshot(sessionId, nextItem),
    heartbeat: trainingService.heartbeat,
    shouldHeartbeat: (nextItem) => nextItem?.hasNextItem === true,
    isHeartbeatInProgress: (heartbeat) => heartbeat.status === 'IN_PROGRESS',
    saveProgress: trainingService.saveProgress,
    saveProgressKeepalive: trainingService.saveProgressKeepalive,
    isCompleted: (nextItem) => nextItem?.sessionStatus === 'COMPLETED',
    onCompleted: (nextItem) => markCompleted(nextItem.sessionId, nextItem.completionHooksStatus),
  });

  const planError = recommendedPlanQuery.error ? normalizeApiError(recommendedPlanQuery.error) : null;

  React.useEffect(() => {
    if (state.phase !== 'home' || !requestedMode || !recommendedPlanQuery.data || resumeCandidate) {
      return;
    }
    const autoStartKey = [
      recommendedPlanQuery.data.planId,
      requestedMode,
      requestedDiagnosisSummaryId ?? 'none',
      requestedLexicalPairId ?? 'none',
      requestedWrongBookId ?? 'none',
      requestedReviewScheduleId ?? 'none',
    ].join(':');
    if (autoStartKeyRef.current === autoStartKey || startMutation.isPending) {
      return;
    }
    autoStartKeyRef.current = autoStartKey;
    void startSessionForMode(requestedMode);
  }, [
    recommendedPlanQuery.data,
    requestedDiagnosisSummaryId,
    requestedLexicalPairId,
    requestedMode,
    requestedReviewScheduleId,
    requestedWrongBookId,
    resumeCandidate,
    startMutation.isPending,
    startSessionForMode,
    state.phase,
  ]);

  React.useEffect(() => {
    if (planError?.status === 409 && requestedMode) {
      clearTrainingIntent();
    }
  }, [clearTrainingIntent, planError?.status, requestedMode]);

  const currentItem = nextItemQuery.data?.item;
  const staleSubmittedItemVisible = !!currentItem && pendingNextItemId === currentItem.itemResultId;
  const isAnswerLocked =
    answerMutation.isPending ||
    !!answerFeedback ||
    completeMutation.isPending ||
    abandonMutation.isPending ||
    nextItemQuery.isFetching ||
    staleSubmittedItemVisible;

  React.useLayoutEffect(() => {
    if (!currentItem) {
      shownAtRef.current = 0;
      return;
    }
    shownAtRef.current = window.performance.now();
  }, [currentItem?.itemResultId]);

  React.useEffect(() => {
    if (!answerFeedback || answerFeedback.mode !== 'SPEED_CHALLENGE') {
      return;
    }
    const timer = window.setTimeout(() => {
      void continueAfterFeedback();
    }, 600);
    return () => window.clearTimeout(timer);
  }, [answerFeedback, continueAfterFeedback]);

  React.useEffect(() => {
    if (!currentItem) {
      setLoadInfoMessage(null);
      answerRequestRef.current = null;
      return;
    }
    runtime.resetFeedback();
    setAnswerFeedback(null);
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

  const submitAnswer = async (option: TrainingOptionViewVO) => {
    if (!currentItem) {
      return;
    }
    const existingRequest =
      answerRequestRef.current?.itemResultId === currentItem.itemResultId ? answerRequestRef.current : null;
    const request = existingRequest ?? (() => {
      const elapsedMs = shownAtRef.current > 0 ? window.performance.now() - shownAtRef.current : 1;
      const reactionTimeMs = Math.min(2_147_483_647, Math.max(1, Math.round(elapsedMs)));
      return {
        itemResultId: currentItem.itemResultId,
        clientRequestId: crypto.randomUUID(),
        selectedAnswerKey: option.key,
        reactionTimeMs,
        hesitationTimeMs: Math.max(0, reactionTimeMs - HESITATION_BASELINE_MS),
      } satisfies SubmitTrainingAnswerRequest;
    })();
    answerRequestRef.current = request;
    await answerMutation.mutateAsync(request);
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
      title="确认放弃当前训练？"
      description="系统将结束这次未完成训练，并把会话标记为已放弃。"
      safety="已完成的训练历史不会受影响，但本次未完成内容不能再从当前进度继续。"
      nextStep="如需保留当前进度请取消；仅在确定重新开始时确认放弃。"
      confirmLabel="确认放弃训练"
      cancelLabel="取消，保留进度"
      pending={abandonMutation.isPending}
      pendingTitle="正在结束训练"
      pendingDescription="放弃请求已经提交，请等待服务器确认。"
      onCancel={() => setAbandonConfirmSessionId(null)}
      onConfirm={() => {
        if (abandonConfirmSessionId !== null) {
          abandonMutation.mutate(abandonConfirmSessionId, {
            onSettled: () => setAbandonConfirmSessionId(null),
          });
        }
      }}
    />
  );

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
        <PageHeader
          title={t('training.runningTitle')}
          subtitle={t('training.runningSubtitle')}
          actions={
            <div className="flex flex-wrap items-center gap-3">
              <button
                type="button"
                onClick={handleRunningAbandon}
                disabled={isAnswerLocked}
                className="rounded-full border border-rose-200 px-5 py-3 text-sm font-bold text-rose-600 disabled:opacity-60 dark:border-rose-500/20"
              >
                放弃本次
              </button>
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
              <div className="text-xs uppercase tracking-[0.24em] text-amber-500">Generating Summary</div>
              <h2 className="type-section-title mt-4 text-slate-900 dark:text-white">正在生成训练总结</h2>
              <p className="mt-4 leading-7 text-slate-500 dark:text-white/45">
                所有答案均已记录，系统正在完成本轮总结，请稍候。
              </p>
            </div>
          </section>
        ) : !currentItem || staleSubmittedItemVisible ? (
          <div className="rounded-[2rem] border border-slate-200 bg-white/70 px-6 py-8 text-sm text-slate-500 dark:border-white/10 dark:bg-white/5 dark:text-white/45">
            {staleSubmittedItemVisible ? '答案已提交，正在同步下一题或总结页，请稍候...' : '正在生成训练总结，请稍候...'}
          </div>
        ) : (
          <>
            <SessionProgressHeader
              label={t('training.progress', {
                current: nextItemQuery.data?.currentItemOrder || 0,
                total: nextItemQuery.data?.totalItems || 0,
              })}
              answeredItems={nextItemQuery.data?.answeredItems}
              totalItems={nextItemQuery.data?.totalItems}
              gradientClassName="bg-gradient-to-r from-emerald-500 to-sky-500"
            />

            <section className="rounded-[3rem] liquid-glass-panel p-10 edge-light">
              <div className="text-xs uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">
                {trainingModeLabel(currentItem.mode)} · {currentItem.cognitiveTag} · {lexicalPairTypeLabel(currentItem.lexicalPairType)}
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
                  <SessionOptionButton
                    key={option.key}
                    disabled={isAnswerLocked}
                    onClick={() => void submitAnswer(option)}
                    label={option.label}
                    icon={<Rocket size={16} className="text-primary" />}
                  />
                ))}
              </div>

              {answerFeedback && (
                <div
                  className={`mt-6 rounded-[1.6rem] border p-5 ${
                    answerFeedback.outcome.correct
                      ? 'border-emerald-500/25 bg-emerald-500/10 text-emerald-800 dark:text-emerald-200'
                      : 'border-rose-500/25 bg-rose-500/10 text-rose-800 dark:text-rose-200'
                  }`}
                  aria-live="polite"
                >
                  <div className="text-lg font-black">
                    {answerFeedback.outcome.correct ? '回答正确' : '回答错误'}
                  </div>
                  <div className="mt-3 text-sm leading-7">
                    正确答案：{findTrainingOptionLabel(currentItem.options, answerFeedback.outcome.correctAnswerKey)}
                  </div>
                  {!answerFeedback.outcome.correct && answerFeedback.outcome.detectedErrorType && (
                    <div className="mt-2 text-sm leading-7">
                      迁移偏差：{errorTypeLabel(answerFeedback.outcome.detectedErrorType)}
                    </div>
                  )}
                  {answerFeedback.outcome.explanation && (
                    <div className="mt-2 text-sm leading-7">{answerFeedback.outcome.explanation}</div>
                  )}
                  {adaptationActionLabel(answerFeedback.outcome.adaptationAction) && (
                    <div className="mt-2 text-sm font-semibold leading-7">
                      纠偏动作：{adaptationActionLabel(answerFeedback.outcome.adaptationAction)}
                    </div>
                  )}
                  {answerFeedback.mode === 'SPEED_CHALLENGE' ? (
                    <div className="mt-3 text-xs font-semibold">即将进入下一题…</div>
                  ) : (
                    <button
                      type="button"
                      onClick={() => void continueAfterFeedback()}
                      className="mt-4 rounded-full bg-slate-900 px-5 py-2.5 text-sm font-bold text-white dark:bg-white dark:text-slate-900"
                    >
                      {answerFeedback.progress.completed ? '查看训练总结' : '下一题'}
                    </button>
                  )}
                </div>
              )}

              {isAnswerLocked && !answerFeedback && (
                <div className="mt-6 text-sm text-slate-500 dark:text-white/45">系统正在提交答案并加载下一题，请稍候。</div>
              )}
            </section>
          </>
        )}
        {abandonConfirmation}
      </div>
    );
  }

  if (state.phase === 'summary') {
    const summary = summaryQuery.data;
    const completionHooksStatus = completionHooksQuery.data?.completionHooksStatus;
    const completionHooksPending =
      completionRefreshSessionId != null &&
      completionHooksStatus !== 'DONE' &&
      completionHooksStatus !== 'FAILED';
    const completionHooksFailed = completionHooksStatus === 'FAILED';
    const completionHooksReady = completionRefreshSessionId == null || completionHooksStatus === 'DONE';

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

        {completionHooksPending && (
          <div className="rounded-[2rem] border border-sky-500/20 bg-sky-500/10 p-6 text-sm text-sky-800 dark:text-sky-200" aria-live="polite">
            训练总结已生成，错题本、复习计划和学习档案正在更新。
          </div>
        )}

        {completionHooksFailed && (
          <div className="rounded-[2rem] border border-amber-500/20 bg-amber-500/10 p-6 text-sm text-amber-800 dark:text-amber-200" role="alert">
            核心训练总结不受影响，但学习数据更新暂时失败，后台将自动重试。
          </div>
        )}

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
                  <h2 className="type-page-title mt-5 text-slate-900 dark:text-white">
                    {t('training.sessionCompletedTitle')}
                  </h2>
                  <p className="mt-4 leading-7 text-slate-500 dark:text-white/45">
                    {summary.improvementHint}
                  </p>
                </div>
                <div className="flex flex-wrap gap-3">
                  <button
                    type="button"
                    onClick={() => {
                      if (summary.nextRecommendedMode) {
                        navigate(
                          buildTrainingHref({
                            mode: summary.nextRecommendedMode,
                            source: 'training-summary',
                          })
                        );
                        return;
                      }
                      dispatch({ type: 'resetHome' });
                    }}
                    className="btn-liquid px-6 py-3 text-white"
                  >
                    继续下一推荐训练
                  </button>
                  <button
                    type="button"
                    disabled={!completionHooksReady}
                    onClick={() => navigate('/errors')}
                    className="rounded-full border border-slate-200 px-6 py-3 text-sm font-bold disabled:cursor-not-allowed disabled:opacity-50 dark:border-white/10"
                  >
                    查看错题与复习
                  </button>
                </div>
              </div>
            </section>

            <div className="grid gap-6 md:grid-cols-3">
              <div className="rounded-[2rem] liquid-glass p-6">
                <div className="text-xs uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">
                  {t('training.accuracy')}
                </div>
                <div className="type-numeric mt-3 text-3xl font-semibold text-slate-900 dark:text-white">
                  {formatMaybePercent(summary.accuracy)}
                </div>
              </div>
              <div className="rounded-[2rem] liquid-glass p-6">
                <div className="text-xs uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">
                  {t('training.averageReactionTime')}
                </div>
                <div className="type-numeric mt-3 text-3xl font-semibold text-slate-900 dark:text-white">
                  {formatMs(summary.averageReactionTime)}
                </div>
              </div>
              <div className="rounded-[2rem] liquid-glass p-6">
                <div className="text-xs uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">
                  {t('training.nextMode')}
                </div>
                <div className="type-section-title mt-3 text-slate-900 dark:text-white">
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

            <section className="rounded-[2.5rem] liquid-glass-panel p-8">
              <div className="mb-4 text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">
                题目回看
              </div>
              <div className="space-y-4">
                {summary.items.map((item) => (
                  <TrainingItemReviewCard key={item.itemResultId} item={item} />
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
      <SessionFeedbackBanners
        submitErrorMessage={startMutation.error ? getApiErrorMessage(startMutation.error) : null}
      />

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
              <button
                type="button"
                onClick={() => navigate(buildDiagnosisHref({ source: 'training-no-plan' }))}
                className="mt-6 btn-liquid px-6 py-3 text-white"
              >
                先去完成诊断
              </button>
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
                {recommendedPlanQuery.data?.priorityMode ? (
                  <div className="mt-5">
                    <TrainingModeSummaryCard mode={recommendedPlanQuery.data.priorityMode} />
                  </div>
                ) : (
                  <h2 className="type-page-title mt-5 text-slate-900 dark:text-white">
                    {t('training.recommendationLoading')}
                  </h2>
                )}
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
                  <TrainingModeSummaryCard
                    key={session.mode}
                    onClick={() => void startSessionForMode(session.mode)}
                    disabled={startMutation.isPending}
                    mode={session.mode}
                    count={session.count}
                  />
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
                      {trainingModeLabel(item.recommendedMode)} · {item.recommendedReason}
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
          <div className="mb-4 flex items-center justify-between gap-3">
            <div className="flex items-center gap-3">
              <Clock3 size={16} className="text-primary" />
              <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">
                {t('training.reviewScheduleTitle')}
              </div>
            </div>
            {!!reviewScheduleQuery.data?.length && (
              <button
                type="button"
                onClick={() =>
                  navigate(
                    buildTrainingHref({
                      mode: reviewScheduleQuery.data[0].reviewMode,
                      source: 'review-schedule',
                      lexicalPairId: reviewScheduleQuery.data[0].lexicalPairId,
                      wrongBookId: reviewScheduleQuery.data[0].wrongBookId,
                      reviewScheduleId: reviewScheduleQuery.data[0].reviewScheduleId,
                    })
                  )
                }
                className="text-sm font-bold text-primary"
              >
                立即复习
              </button>
            )}
          </div>
          {reviewScheduleQuery.isLoading ? (
            <PanelSkeleton className="p-0" />
          ) : reviewScheduleQuery.error ? (
            <div className="rounded-[1.6rem] border border-rose-500/20 bg-rose-500/5 px-4 py-3 text-sm text-rose-500">
              {getApiErrorMessage(reviewScheduleQuery.error)}
            </div>
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
                    {trainingModeLabel(item.reviewMode)} · {formatDateTime(item.dueAt)}
                  </div>
                  <button
                    type="button"
                    onClick={() =>
                      navigate(
                        buildTrainingHref({
                          mode: item.reviewMode,
                          source: 'review-schedule-item',
                          lexicalPairId: item.lexicalPairId,
                          wrongBookId: item.wrongBookId,
                          reviewScheduleId: item.reviewScheduleId,
                        })
                      )
                    }
                    className="mt-4 rounded-full border border-slate-200 px-4 py-2 text-sm font-bold text-primary dark:border-white/10"
                  >
                    开始
                  </button>
                </div>
              ))}
              {!reviewScheduleQuery.data?.length && (
                <div className="text-sm text-slate-500 dark:text-white/45">暂无待复习计划。</div>
              )}
            </div>
          )}
        </section>

        <section className="rounded-[2.5rem] liquid-glass-panel p-8">
          <div className="mb-4 flex items-center justify-between gap-3">
            <div className="flex items-center gap-3">
              <Brain size={16} className="text-primary" />
              <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">
                {t('training.wrongBookTitle')}
              </div>
            </div>
            <button
              type="button"
              onClick={() => navigate('/errors')}
              className="text-sm font-bold text-primary"
            >
              查看全部
            </button>
          </div>
          {wrongBookQuery.isLoading ? (
            <PanelSkeleton className="p-0" />
          ) : wrongBookQuery.error ? (
            <div className="rounded-[1.6rem] border border-rose-500/20 bg-rose-500/5 px-4 py-3 text-sm text-rose-500">
              {getApiErrorMessage(wrongBookQuery.error)}
            </div>
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
                    {lexicalPairTypeLabel(item.lexicalPairType)} · {trainingModeLabel(item.recommendedMode)} · {errorTypeLabel(item.lastErrorType)} ·{' '}
                    {t('training.wrongCount', { count: item.wrongCount })}
                  </div>
                  <button
                    type="button"
                    onClick={() =>
                      navigate(
                        buildTrainingHref({
                          mode: item.recommendedMode,
                          source: 'wrong-book',
                          lexicalPairId: item.lexicalPairId,
                          wrongBookId: item.wrongBookId,
                        })
                      )
                    }
                    className="mt-4 rounded-full border border-slate-200 px-4 py-2 text-sm font-bold text-primary dark:border-white/10"
                  >
                    开始纠错
                  </button>
                </div>
              ))}
              {!wrongBookQuery.data?.length && (
                <div className="text-sm text-slate-500 dark:text-white/45">暂无错题记录。</div>
              )}
            </div>
          )}
        </section>
      </div>

      {resumeCandidate && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/45 px-4 backdrop-blur-sm">
          <div
            ref={resumeDialogRef}
            role="dialog"
            aria-modal="true"
            aria-labelledby="training-resume-title"
            tabIndex={-1}
            className="w-full max-w-xl rounded-[2.4rem] border border-slate-200/70 bg-white p-8 shadow-2xl dark:border-white/10 dark:bg-slate-950"
          >
            <span id="training-resume-title" className="sr-only">发现已保存的未完成训练</span>
            <FeedbackState
              kind="saved"
              compact
              className="border-0 bg-transparent p-0 shadow-none"
              title="发现已保存的未完成训练"
              description={`系统找到了上次未完成的训练，最近保存时间：${resumeCandidate.lastSavedAt ? formatDateTime(resumeCandidate.lastSavedAt) : '未知'}。`}
              impact="服务器保留了已完成的训练进度；继续训练不会覆盖其他历史记录。"
              nextStep="选择“继续训练”恢复进度；仅在确定不再需要本次训练时选择放弃。"
            />
            <div className="mt-8 flex flex-wrap gap-3">
              <button ref={resumeContinueButtonRef} type="button" onClick={handleResumeContinue} className="btn-liquid px-5 py-3 text-white">
                继续训练
              </button>
              <button
                type="button"
                onClick={handleResumeAbandon}
                disabled={abandonMutation.isPending}
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
      {abandonConfirmation}
    </div>
  );
};

export default TrainingPage;
