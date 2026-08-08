import React from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { CheckCircle2, ChevronLeft, ChevronRight, Clock3, Save, Send } from 'lucide-react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { PageHeader, SectionEyebrow, StatusBadge } from '@/components/common';
import { FeedbackState } from '@/components/common/FeedbackState';
import { ConfirmationDialog } from '@/components/common/ConfirmationDialog';
import { getApiErrorMessage, normalizeApiError } from '@/lib/api';
import { assessmentQuestionTypeLabel, formatDateTime } from '@/lib/format';
import { assessmentService } from '@/lib/services';
import type { AssessmentAttemptDetailVO, AssessmentAttemptQuestionVO } from '@/lib/contracts';
import {
  clearAssessmentDraft,
  markAssessmentDraftSaved,
  readAssessmentDraft,
  writeAssessmentDraft,
} from '@/features/assessment/draftStorage';
import { enqueueSerializedTask } from '@/features/assessment/saveQueue';
import { useLeaveProtection } from '@/features/session-runtime/useLeaveProtection';
import { SessionProgressHeader } from '@/features/session-runtime/components';

function formatRemaining(remainingMs: number) {
  if (remainingMs <= 0) {
    return '00:00';
  }
  const totalSeconds = Math.floor(remainingMs / 1000);
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;
  const pad = (value: number) => String(value).padStart(2, '0');
  return hours > 0 ? `${pad(hours)}:${pad(minutes)}:${pad(seconds)}` : `${pad(minutes)}:${pad(seconds)}`;
}

function buildInitialResponses(detail?: AssessmentAttemptDetailVO | null) {
  return (detail?.questions || []).reduce<Record<number, string[]>>((accumulator, question) => {
    accumulator[question.questionOrder] = question.responses || [];
    return accumulator;
  }, {});
}

function buildSavePayload(
  detail: AssessmentAttemptDetailVO,
  responsesByOrder: Record<number, string[]>,
  baseVersion: number
) {
  return {
    responses: detail.questions.map((question) => ({
      questionOrder: question.questionOrder,
      responses: responsesByOrder[question.questionOrder] || [],
    })),
    baseVersion,
  };
}

function hasResponses(responses?: string[]) {
  return !!responses?.map((item) => item.trim()).filter(Boolean).length;
}

function isConflictMessage(message?: string | null) {
  return !!message && /(冲突|其他|设备|another|conflict|version)/i.test(message);
}

function mergeDraftResponses(detail: AssessmentAttemptDetailVO) {
  const serverResponses = buildInitialResponses(detail);
  const draft = readAssessmentDraft(detail.attemptId);
  if (!draft) {
    return { responsesByOrder: serverResponses, restored: false, conflicted: false };
  }
  if (draft.baseVersion !== detail.version) {
    return { responsesByOrder: serverResponses, restored: false, conflicted: true };
  }
  let restored = false;
  const merged = { ...serverResponses };
  detail.questions.forEach((question) => {
    if (!Object.prototype.hasOwnProperty.call(draft.responsesByOrder, question.questionOrder)) {
      return;
    }
    const localResponses = draft.responsesByOrder[question.questionOrder] || [];
    const serverQuestionResponses = serverResponses[question.questionOrder] || [];
    if (serverQuestionResponses.join('\u0000') === localResponses.join('\u0000')) {
      return;
    }
    merged[question.questionOrder] = localResponses;
    restored = true;
  });
  return { responsesByOrder: merged, restored, conflicted: false };
}

type PersistMode = 'manual' | 'auto' | 'background';

const StudentAssessmentAttemptPage: React.FC = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const params = useParams<{ attemptId: string }>();
  const attemptId = Number(params.attemptId);
  const isValidAttemptId = Number.isSafeInteger(attemptId) && attemptId > 0;
  const [selectedQuestionOrder, setSelectedQuestionOrder] = React.useState(1);
  const [responsesByOrder, setResponsesByOrder] = React.useState<Record<number, string[]>>({});
  const [answeredCount, setAnsweredCount] = React.useState(0);
  const [lastSavedAt, setLastSavedAt] = React.useState<string | null>(null);
  const [saveNotice, setSaveNotice] = React.useState<string | null>(null);
  const [saveErrorMessage, setSaveErrorMessage] = React.useState<string | null>(null);
  const [submitErrorMessage, setSubmitErrorMessage] = React.useState<string | null>(null);
  const [submitConfirmOpen, setSubmitConfirmOpen] = React.useState(false);
  const [clientNow, setClientNow] = React.useState(Date.now());
  const [serverOffsetMs, setServerOffsetMs] = React.useState(0);
  const [isSubmitting, setIsSubmitting] = React.useState(false);
  const [submitLocked, setSubmitLocked] = React.useState(false);
  const [isSaving, setIsSaving] = React.useState(false);
  const hydratedAttemptIdRef = React.useRef<number | null>(null);
  const allowNavigationRef = React.useRef<(callback: () => void) => void>((callback) => {
    callback();
  });
  const autoSubmitTriggeredRef = React.useRef(false);
  const autoSaveTimerRef = React.useRef<number | null>(null);
  const skipAutosaveRef = React.useRef(true);
  const latestSaveRequestRef = React.useRef(0);
  const saveQueueRef = React.useRef<Promise<void>>(Promise.resolve());
  const currentVersionRef = React.useRef(1);
  const questionTitleRef = React.useRef<HTMLDivElement | null>(null);

  const detailQuery = useQuery({
    queryKey: ['student-assessment-attempt', attemptId],
    queryFn: ({ signal }) => assessmentService.getStudentAttempt(attemptId, { signal }),
    enabled: isValidAttemptId,
    retry: false,
  });

  const heartbeatQuery = useQuery({
    queryKey: ['student-assessment-attempt-heartbeat', attemptId],
    queryFn: ({ signal }) => assessmentService.getStudentAttemptHeartbeat(attemptId, { signal }),
    enabled: isValidAttemptId && detailQuery.data?.attemptId === attemptId,
    retry: false,
    refetchInterval: (query) => {
      if (detailQuery.data?.status !== 'IN_PROGRESS') {
        return false;
      }
      return query.state.data?.status === 'SUBMITTED' ? false : 15000;
    },
  });

  React.useEffect(() => {
    if (!detailQuery.data || detailQuery.data.attemptId !== attemptId) {
      return;
    }
    setServerOffsetMs(new Date(detailQuery.data.serverTime).getTime() - Date.now());
    if (detailQuery.data.status === 'SUBMITTED') {
      clearAssessmentDraft(detailQuery.data.attemptId);
    }
    if (hydratedAttemptIdRef.current === detailQuery.data.attemptId) {
      return;
    }
    hydratedAttemptIdRef.current = detailQuery.data.attemptId;
    const hydratedResponses = mergeDraftResponses(detailQuery.data);
    currentVersionRef.current = detailQuery.data.version;
    setResponsesByOrder(hydratedResponses.responsesByOrder);
    setAnsweredCount(detailQuery.data.answeredCount);
    setLastSavedAt(detailQuery.data.lastSavedAt || null);
    setSelectedQuestionOrder(detailQuery.data.questions.find((question) => !question.answered)?.questionOrder || detailQuery.data.questions[0]?.questionOrder || 1);
    setSaveNotice(
      hydratedResponses.conflicted
        ? '检测到其他页面或设备已保存更新，本地旧草稿未自动覆盖服务器答案。'
        : hydratedResponses.restored
          ? '已恢复本地草稿。'
          : null
    );
    setSaveErrorMessage(null);
    setSubmitErrorMessage(null);
    setIsSubmitting(false);
    setIsSaving(false);
    setSubmitLocked(false);
    autoSubmitTriggeredRef.current = false;
    skipAutosaveRef.current = true;
    if (autoSaveTimerRef.current !== null) {
      window.clearTimeout(autoSaveTimerRef.current);
      autoSaveTimerRef.current = null;
    }
  }, [attemptId, detailQuery.data]);

  React.useEffect(() => {
    if (!heartbeatQuery.data || heartbeatQuery.data.attemptId !== attemptId) {
      return;
    }
    setServerOffsetMs(new Date(heartbeatQuery.data.serverTime).getTime() - Date.now());
    setAnsweredCount(heartbeatQuery.data.answeredCount);
    setLastSavedAt(heartbeatQuery.data.lastSavedAt || null);
    if (heartbeatQuery.data.status === 'IN_PROGRESS' && heartbeatQuery.data.version !== currentVersionRef.current) {
      setSaveErrorMessage('检测到其他页面或设备更新了答卷。请刷新页面同步最新版本后再继续。');
    }
    queryClient.setQueryData<AssessmentAttemptDetailVO | undefined>(
      ['student-assessment-attempt', attemptId],
      (current) =>
        current
          ? {
              ...current,
              status: heartbeatQuery.data.status,
              answeredCount: heartbeatQuery.data.answeredCount,
              expiresAt: heartbeatQuery.data.expiresAt,
              submittedAt: heartbeatQuery.data.submittedAt || null,
              lastSavedAt: heartbeatQuery.data.lastSavedAt || null,
              version: heartbeatQuery.data.version,
              serverTime: heartbeatQuery.data.serverTime,
            }
          : current
    );
  }, [attemptId, heartbeatQuery.data, queryClient]);

  React.useEffect(() => {
    const timer = window.setInterval(() => setClientNow(Date.now()), 1000);
    return () => window.clearInterval(timer);
  }, []);

  React.useEffect(
    () => () => {
      if (autoSaveTimerRef.current !== null) {
        window.clearTimeout(autoSaveTimerRef.current);
      }
    },
    []
  );

  const detail = detailQuery.data?.attemptId === attemptId ? detailQuery.data : undefined;
  const currentServerNow = clientNow + serverOffsetMs;
  const expiresAtMs = detail ? new Date(detail.expiresAt).getTime() : Number.NaN;
  const remainingMs = detail && Number.isFinite(expiresAtMs) ? expiresAtMs - currentServerNow : null;
  const orderedQuestions = detail?.questions || [];
  const currentQuestion = orderedQuestions.find((question) => question.questionOrder === selectedQuestionOrder) || orderedQuestions[0];
  const answeredCountFromLocal = orderedQuestions.filter((question) => hasResponses(responsesByOrder[question.questionOrder])).length;
  const unansweredQuestionOrders = orderedQuestions.filter((question) => !hasResponses(responsesByOrder[question.questionOrder])).map((question) => question.questionOrder);
  const canEdit = detail?.status === 'IN_PROGRESS' && !submitLocked;
  const shouldWarnBeforeLeave = detail?.status === 'IN_PROGRESS' && !submitLocked;

  React.useEffect(() => {
    questionTitleRef.current?.focus();
  }, [currentQuestion?.questionOrder]);

  const navigateToResult = React.useCallback(
    (nextAttemptId: number) => {
      clearAssessmentDraft(nextAttemptId);
      allowNavigationRef.current(() => navigate(`/assessments/attempts/${nextAttemptId}/result`, { replace: true }));
    },
    [navigate]
  );

  const resolveSubmittedAttempt = React.useCallback(async () => {
    try {
      const refreshed = await assessmentService.getStudentAttemptHeartbeat(attemptId);
      if (refreshed.status === 'SUBMITTED') {
        queryClient.setQueryData<AssessmentAttemptDetailVO | undefined>(
          ['student-assessment-attempt', attemptId],
          (current) =>
            current
              ? {
                  ...current,
                  status: refreshed.status,
                  answeredCount: refreshed.answeredCount,
                  expiresAt: refreshed.expiresAt,
                  submittedAt: refreshed.submittedAt || null,
                  lastSavedAt: refreshed.lastSavedAt || null,
                  version: refreshed.version,
                  serverTime: refreshed.serverTime,
                }
              : current
        );
        return refreshed;
      }
    } catch {
      // Keep the old 409 recovery path viable when the heartbeat probe fails.
    }

    const refreshedDetail = await detailQuery.refetch();
    return refreshedDetail.data?.status === 'SUBMITTED' ? refreshedDetail.data : null;
  }, [attemptId, detailQuery, queryClient]);

  const persistResponses = React.useCallback(
    async (
      mode: PersistMode,
      snapshot: Record<number, string[]>,
      options?: { ignoreLock?: boolean; keepalive?: boolean; silentSuccess?: boolean }
    ) => {
      if (!detail || detail.status !== 'IN_PROGRESS') {
        return null;
      }
      if (submitLocked && !options?.ignoreLock) {
        return null;
      }

      const requestId = ++latestSaveRequestRef.current;
      return enqueueSerializedTask(saveQueueRef, async () => {
        if (!options?.keepalive) {
          setIsSaving(true);
          setSaveErrorMessage(null);
          if (!options?.silentSuccess) {
            setSaveNotice(
              mode === 'manual'
                ? '正在保存答案...'
                : '正在自动保存答案...'
            );
          }
        }

        try {
          const payload = buildSavePayload(detail, snapshot, currentVersionRef.current);
          const progress = options?.keepalive
            ? await assessmentService.saveStudentResponsesKeepalive(attemptId, payload)
            : await assessmentService.saveStudentResponses(attemptId, payload);

          currentVersionRef.current = progress.version;
          if (options?.keepalive || requestId === latestSaveRequestRef.current) {
            setAnsweredCount(progress.answeredCount);
            setLastSavedAt(progress.lastSavedAt || null);
            if (!options?.silentSuccess) {
              setSaveNotice(
                mode === 'manual'
                  ? '答案已保存。'
                  : progress.lastSavedAt
                    ? `已自动保存于 ${formatDateTime(progress.lastSavedAt)}`
                    : '答案已自动保存。'
              );
            }
          }
          markAssessmentDraftSaved(attemptId, snapshot, progress.version);
          queryClient.setQueryData<AssessmentAttemptDetailVO | undefined>(
            ['student-assessment-attempt', attemptId],
            (current) => current ? { ...current, version: progress.version } : current
          );

          if (mode === 'manual') {
            await queryClient.invalidateQueries({ queryKey: ['student-assessments'] });
            await queryClient.invalidateQueries({ queryKey: ['student-assessment-history'] });
          }
          return progress;
        } catch (error) {
          if (!options?.keepalive && requestId === latestSaveRequestRef.current) {
            setSaveNotice(null);
            setSaveErrorMessage(
              getApiErrorMessage(
                error,
                mode === 'manual' ? '保存答案失败' : mode === 'background' ? '离开前保存失败，请先手动保存后再离开。' : '自动保存失败'
              )
            );
          }
          throw error;
        } finally {
          if (!options?.keepalive && requestId === latestSaveRequestRef.current) {
            setIsSaving(false);
          }
        }
      });
    },
    [attemptId, detail, queryClient, submitLocked]
  );

  const { allowNavigation } = useLeaveProtection({
    active: shouldWarnBeforeLeave,
    leaveConfirm: '当前测评仍在进行中。离开页面前会尝试自动保存，确认离开吗？',
    onRouteLeave: async () => {
      try {
        await persistResponses('background', responsesByOrder, { silentSuccess: true });
        return true;
      } catch (error) {
        const submittedAttempt = normalizeApiError(error).status === 409 ? await resolveSubmittedAttempt() : null;
        if (submittedAttempt) {
          clearAssessmentDraft(attemptId);
        }
        return !!submittedAttempt;
      }
    },
    onBackgroundPersist: async () => {
      await persistResponses('background', responsesByOrder, { keepalive: true, silentSuccess: true });
    },
  });
  allowNavigationRef.current = allowNavigation;

  const handleSubmit = React.useCallback(
    async (reason: 'manual' | 'timeout' = 'manual') => {
      if (!detail || detail.status !== 'IN_PROGRESS') {
        return;
      }
      if (autoSaveTimerRef.current !== null) {
        window.clearTimeout(autoSaveTimerRef.current);
        autoSaveTimerRef.current = null;
      }

      setSubmitLocked(true);
      setIsSubmitting(true);
      setSubmitErrorMessage(null);
      setSaveErrorMessage(null);
      setSaveNotice(reason === 'timeout' ? '作答时间已结束，系统正在自动交卷...' : '正在提交答卷，请勿关闭页面。');

      try {
        const submitFinalSnapshot = (baseVersion: number) => assessmentService.submitStudentAttempt(attemptId, {
          ...buildSavePayload(detail, responsesByOrder, baseVersion),
          reason: reason === 'timeout' ? 'TIMEOUT' as const : 'MANUAL' as const,
        });
        let result: Awaited<ReturnType<typeof submitFinalSnapshot>>;
        try {
          result = await submitFinalSnapshot(currentVersionRef.current);
        } catch (error) {
          if (normalizeApiError(error).code !== 'VERSION_CONFLICT') {
            throw error;
          }
          const refreshed = await assessmentService.getStudentAttemptHeartbeat(attemptId);
          if (refreshed.status !== 'IN_PROGRESS') {
            throw error;
          }
          currentVersionRef.current = refreshed.version;
          result = await submitFinalSnapshot(refreshed.version);
        }
        currentVersionRef.current = result.version;
        clearAssessmentDraft(attemptId);
        await queryClient.invalidateQueries({ queryKey: ['student-assessments'] });
        await queryClient.invalidateQueries({ queryKey: ['student-assessment-history'] });
        await queryClient.invalidateQueries({ queryKey: ['student-assessment-attempt', attemptId] });
        await queryClient.invalidateQueries({ queryKey: ['student-assessment-attempt-heartbeat', attemptId] });
        navigateToResult(result.attemptId);
      } catch (error) {
        const submittedAttempt = normalizeApiError(error).status === 409 ? await resolveSubmittedAttempt() : null;
        if (submittedAttempt) {
          await queryClient.invalidateQueries({ queryKey: ['student-assessments'] });
          await queryClient.invalidateQueries({ queryKey: ['student-assessment-history'] });
          navigateToResult(submittedAttempt.attemptId);
          return;
        }
        setSubmitLocked(reason === 'timeout');
        setIsSubmitting(false);
        setSaveNotice(null);
        setSubmitErrorMessage(
          getApiErrorMessage(
            error,
            reason === 'timeout'
              ? '自动交卷请求失败，系统仍会继续确认最终状态；如未自动跳转，可点击“重新提交”。'
              : '交卷失败，请点击“重新提交”确认最终状态。'
          )
        );
      }
    },
    [attemptId, detail, navigateToResult, queryClient, resolveSubmittedAttempt, responsesByOrder]
  );

  React.useEffect(() => {
    if (
      !detail ||
      detail.status !== 'IN_PROGRESS' ||
      remainingMs === null ||
      remainingMs > 0 ||
      autoSubmitTriggeredRef.current
    ) {
      return;
    }
    autoSubmitTriggeredRef.current = true;
    setSubmitConfirmOpen(false);
    setSubmitLocked(true);
    void handleSubmit('timeout');
  }, [detail, handleSubmit, remainingMs]);

  React.useEffect(() => {
    if (detail?.status !== 'SUBMITTED') {
      return;
    }
    clearAssessmentDraft(detail.attemptId);
    navigateToResult(detail.attemptId);
  }, [detail?.attemptId, detail?.status, navigateToResult]);

  React.useEffect(() => {
    if (!detail || detail.status !== 'IN_PROGRESS' || submitLocked) {
      return;
    }
    if (skipAutosaveRef.current) {
      skipAutosaveRef.current = false;
      return;
    }
    if (autoSaveTimerRef.current !== null) {
      window.clearTimeout(autoSaveTimerRef.current);
    }
    autoSaveTimerRef.current = window.setTimeout(() => {
      void persistResponses('auto', responsesByOrder).catch(() => undefined);
    }, 1200);
    return () => {
      if (autoSaveTimerRef.current !== null) {
        window.clearTimeout(autoSaveTimerRef.current);
      }
    };
  }, [detail, persistResponses, responsesByOrder, submitLocked]);

  React.useEffect(() => {
    autoSubmitTriggeredRef.current = false;
  }, [attemptId]);

  const updateSingleResponse = React.useCallback((questionOrder: number, value: string) => {
    setResponsesByOrder((current) => {
      const next = { ...current, [questionOrder]: value ? [value] : [] };
      writeAssessmentDraft(attemptId, currentVersionRef.current, next);
      return next;
    });
    setSaveNotice(null);
    setSaveErrorMessage(null);
    setSubmitErrorMessage(null);
  }, [attemptId]);

  const toggleMultipleResponse = React.useCallback((questionOrder: number, value: string) => {
    setResponsesByOrder((current) => {
      const existing = current[questionOrder] || [];
      const next = existing.includes(value) ? existing.filter((item) => item !== value) : [...existing, value];
      const merged = { ...current, [questionOrder]: next };
      writeAssessmentDraft(attemptId, currentVersionRef.current, merged);
      return merged;
    });
    setSaveNotice(null);
    setSaveErrorMessage(null);
    setSubmitErrorMessage(null);
  }, [attemptId]);

  const updateFillBlankResponse = React.useCallback((questionOrder: number, value: string) => {
    setResponsesByOrder((current) => {
      const next = { ...current, [questionOrder]: value.trim() ? [value] : [] };
      writeAssessmentDraft(attemptId, currentVersionRef.current, next);
      return next;
    });
    setSaveNotice(null);
    setSaveErrorMessage(null);
    setSubmitErrorMessage(null);
  }, [attemptId]);

  const renderQuestionBody = (question: AssessmentAttemptQuestionVO) => {
    const responses = responsesByOrder[question.questionOrder] || [];
    if (question.questionType === 'FILL_BLANK') {
      const inputId = `assessment-answer-${question.questionOrder}`;
      return (
        <label htmlFor={inputId} className="block space-y-2">
          <span className="text-sm font-semibold text-slate-700 dark:text-white/70">填写答案</span>
          <textarea
            id={inputId}
            value={responses[0] || ''}
            onChange={(event) => updateFillBlankResponse(question.questionOrder, event.target.value)}
            rows={5}
            maxLength={1000}
            disabled={!canEdit}
            className="w-full min-w-0 rounded-2xl border border-slate-200 bg-white/75 px-4 py-3 text-base disabled:opacity-70 sm:rounded-3xl dark:border-white/10 dark:bg-white/5"
            placeholder="请输入答案"
          />
        </label>
      );
    }

    return (
      <div className="grid gap-3">
        {question.options.map((option) => {
          const checked = responses.includes(option.key);
          return (
            <label
              key={option.key}
              onClick={(event) => {
                if (!canEdit) {
                  return;
                }
                if (event.target instanceof HTMLInputElement) {
                  return;
                }
                event.preventDefault();
                if (question.questionType === 'SINGLE_CHOICE') {
                  updateSingleResponse(question.questionOrder, option.key);
                } else {
                  toggleMultipleResponse(question.questionOrder, option.key);
                }
              }}
              className={`flex min-h-11 min-w-0 items-start gap-3 rounded-2xl border px-4 py-4 text-sm transition-all motion-reduce:transition-none ${
                checked
                  ? 'border-primary/30 bg-primary/10 text-slate-900 dark:text-white'
                  : 'border-slate-200/70 bg-white/70 text-slate-600 dark:border-white/10 dark:bg-white/5 dark:text-white/60'
              } ${canEdit ? 'cursor-pointer' : 'cursor-not-allowed opacity-80'}`}
            >
              <input
                type={question.questionType === 'SINGLE_CHOICE' ? 'radio' : 'checkbox'}
                name={`question-${question.questionOrder}`}
                value={option.key}
                checked={checked}
                disabled={!canEdit}
                className="mt-1 shrink-0"
                onChange={() => {
                  if (question.questionType === 'SINGLE_CHOICE') {
                    updateSingleResponse(question.questionOrder, option.key);
                  } else {
                    toggleMultipleResponse(question.questionOrder, option.key);
                  }
                }}
              />
              <div className="min-w-0">
                <div className="font-semibold">{option.key}</div>
                <div className="mt-1 break-words">{option.label}</div>
              </div>
            </label>
          );
        })}
      </div>
    );
  };

  if (!isValidAttemptId) {
    return (
      <div className="min-w-0 rounded-2xl border border-amber-500/20 bg-amber-500/10 p-4 text-amber-800 sm:rounded-3xl sm:p-6 md:p-8 dark:text-amber-200">
        <div className="text-xl font-black sm:text-2xl">测评链接无效</div>
        <p className="mt-3 text-sm">答卷编号必须是正整数，请返回任务列表重新进入。</p>
        <Link to="/assessments" className="mt-5 inline-flex w-full items-center justify-center rounded-full border border-amber-500/30 px-4 py-2 text-sm font-bold sm:w-auto">
          返回任务列表
        </Link>
      </div>
    );
  }

  return (
    <div className="page-stack pb-20">
      <PageHeader
        eyebrow="通用测评"
        title={detail?.paperTitle || '测评作答'}
        subtitle={detail ? `${detail.className} · 整卷时长 ${detail.durationMinutes} 分钟 · 截止 ${formatDateTime(detail.expiresAt)}` : '正在加载测评内容'}
        actions={
          <div className="page-actions">
            <Link to="/assessments" className="inline-flex items-center justify-center rounded-full border border-slate-200 px-4 py-3 text-sm dark:border-white/10">
              返回任务列表
            </Link>
            <button
              type="button"
              disabled={!canEdit || isSaving || isSubmitting}
              onClick={() => void persistResponses('manual', responsesByOrder)}
              className="inline-flex items-center justify-center rounded-full border border-slate-200 px-4 py-3 text-sm font-bold text-primary disabled:opacity-60 dark:border-white/10"
            >
              <Save size={14} className="mr-2 inline-block" />
              保存答案
            </button>
            <button
              type="button"
              disabled={!detail || detail.status !== 'IN_PROGRESS' || isSubmitting}
              onClick={() => {
                if (remainingMs !== null && remainingMs <= 0) {
                  void handleSubmit('timeout');
                  return;
                }
                setSubmitConfirmOpen(true);
              }}
              className="btn-liquid inline-flex items-center justify-center px-5 py-3 text-white disabled:opacity-60"
            >
              <Send size={14} className="mr-2 inline-block" />
              {submitErrorMessage ? '重新提交' : '交卷'}
            </button>
          </div>
        }
      />

      {(saveNotice || saveErrorMessage || submitErrorMessage) && (
        submitErrorMessage ? (
          <FeedbackState
            kind="retry"
            compact
            title="答卷未能提交"
            description={submitErrorMessage}
            primaryAction={{ label: '重试提交', onClick: () => void handleSubmit('manual'), disabled: isSubmitting }}
            impact="失败请求不会重复计入结果，当前答卷仍保留在页面中。"
            nextStep="检查答案后点击“重新提交”；成功前请不要关闭当前页面。"
          />
        ) : saveErrorMessage ? (
          <FeedbackState
            kind="retry"
            compact
            title="答案未能保存"
            description={saveErrorMessage}
            primaryAction={{ label: '重试保存', onClick: () => void persistResponses('manual', responsesByOrder), disabled: isSaving || isSubmitting }}
            impact="服务器未确认本轮答案变更；上一次已保存的答案仍然安全。"
            nextStep="点击“保存答案”重试，成功前请留在当前页面。"
          />
        ) : isSubmitting ? (
          <FeedbackState
            kind="saving"
            compact
            title="正在提交答卷"
            description={saveNotice ?? '系统正在提交答卷，请不要关闭页面。'}
          />
        ) : isSaving ? (
          <FeedbackState
            kind="saving"
            compact
            title="正在保存答案"
            description={saveNotice ?? '系统正在保存当前答案。'}
          />
        ) : (
          <FeedbackState
            kind="saved"
            compact
            title="答案已保存"
            description={saveNotice ?? '服务器已确认保存当前答案。'}
          />
        )
      )}

      {detailQuery.error && (
        <FeedbackState
          kind="retry"
          title="测评内容未能加载"
          description={getApiErrorMessage(detailQuery.error)}
          impact="加载失败不会修改或覆盖此前保存的答案。"
          nextStep="点击重试加载；如果持续失败，请返回测评列表稍后再试。"
          primaryAction={{ label: '重试加载', onClick: () => void detailQuery.refetch() }}
        />
      )}

      {detailQuery.isLoading && (
        <FeedbackState
          kind="loading"
          title="正在加载测评内容"
          description="系统正在读取答卷、题目和当前保存位置。"
          impact="当前仅读取数据，不会提交或覆盖答案。"
          nextStep="请稍等，题目准备好后会直接显示。"
        />
      )}

      {detail && currentQuestion && (
        <>
          <SessionProgressHeader
            icon={<Clock3 size={16} />}
            label="题目进度"
            currentItem={currentQuestion.questionOrder}
            answeredItems={answeredCountFromLocal || answeredCount}
            totalItems={detail.questionCount}
            savedState={isSubmitting ? 'saving' : isConflictMessage(saveErrorMessage) || isConflictMessage(saveNotice) ? 'conflict' : saveErrorMessage ? 'error' : isSaving ? 'saving' : lastSavedAt || saveNotice ? 'saved' : 'idle'}
            savedAtLabel={lastSavedAt ? formatDateTime(lastSavedAt) : null}
            remainingLabel={remainingMs === null ? '--:--' : formatRemaining(remainingMs)}
            remainingMs={remainingMs}
            onExit={() => navigate('/assessments')}
            exitLabel="退出测评"
            exitDisabled={isSaving || isSubmitting}
            gradientClassName="bg-[hsl(var(--progress))]"
          />
          <div className="grid min-w-0 gap-4 sm:gap-6 xl:grid-cols-[minmax(0,16rem)_minmax(0,1fr)] xl:gap-8">
          <aside className="min-w-0 space-y-5 rounded-2xl liquid-glass-panel p-4 sm:rounded-3xl sm:p-6 md:p-8">
            <div className="rounded-2xl border border-slate-200/70 bg-white/75 px-4 py-4 dark:border-white/10 dark:bg-white/5">
              <SectionEyebrow>剩余时间</SectionEyebrow>
              <div className={`mt-3 text-2xl font-black sm:text-3xl ${remainingMs !== null && remainingMs <= 5 * 60 * 1000 ? 'text-rose-500' : 'text-slate-900 dark:text-white'}`}>
                {remainingMs === null ? '--:--' : formatRemaining(remainingMs)}
              </div>
              <div className="mt-2 break-words text-sm text-slate-500 dark:text-white/45">
                <Clock3 size={14} className="mr-2 inline-block" />
                最后保存 {formatDateTime(lastSavedAt)}
              </div>
            </div>

            <div className="grid grid-cols-5 gap-2 sm:grid-cols-6 xl:grid-cols-4">
              {orderedQuestions.map((question) => {
                const answered = hasResponses(responsesByOrder[question.questionOrder]);
                const selected = question.questionOrder === currentQuestion.questionOrder;
                return (
                  <button
                    key={question.questionOrder}
                    type="button"
                    aria-current={selected ? 'step' : undefined}
                    aria-label={`第 ${question.questionOrder} 题，${selected ? '当前题' : answered ? '已作答' : '未作答'}`}
                    onClick={() => setSelectedQuestionOrder(question.questionOrder)}
                    className={`min-h-11 rounded-2xl px-2 py-3 text-sm font-bold transition-all motion-reduce:transition-none ${
                      selected
                        ? 'bg-primary text-white'
                        : answered
                          ? 'border border-emerald-500/20 bg-emerald-500/10 text-emerald-700 dark:text-emerald-300'
                          : 'border border-slate-200 bg-white/75 text-slate-600 dark:border-white/10 dark:bg-white/5 dark:text-white/60'
                    }`}
                  >
                    {question.questionOrder}
                  </button>
                );
              })}
            </div>

            <div className="space-y-2 break-words text-sm text-slate-500 dark:text-white/45">
              <div>已作答：{answeredCountFromLocal || answeredCount} / {detail.questionCount}</div>
              <div>总分：{detail.totalScore}</div>
              <div>开始时间：{formatDateTime(detail.startedAt)}</div>
            </div>

            {detail.instructionsText && (
              <div className="rounded-2xl border border-dashed border-slate-200/80 px-4 py-4 text-sm text-slate-600 dark:border-white/10 dark:text-white/60">
                {detail.instructionsText}
              </div>
            )}
          </aside>

          <section className="min-w-0 space-y-6 rounded-2xl liquid-glass-panel p-4 sm:rounded-3xl sm:p-6 md:p-8">
            <div className="flex flex-wrap items-start justify-between gap-4">
              <div className="min-w-0">
                <SectionEyebrow>
                  第 {currentQuestion.questionOrder} 题 / 共 {detail.questionCount} 题
                </SectionEyebrow>
                <div ref={questionTitleRef} tabIndex={-1} className="mt-3 break-words text-2xl font-black text-slate-900 outline-none sm:text-3xl dark:text-white">
                  {assessmentQuestionTypeLabel(currentQuestion.questionType)}
                </div>
              </div>
              <StatusBadge label={`${currentQuestion.score} 分`} tone="neutral" className="px-4 py-2 text-sm" />
            </div>

            <div className="rounded-2xl border border-slate-200/70 bg-white/75 p-4 sm:p-5 dark:border-white/10 dark:bg-white/5">
              <div className="break-words text-base font-semibold leading-8 text-slate-900 sm:text-lg dark:text-white">{currentQuestion.stemText}</div>
              {currentQuestion.promptText && (
                <div className="mt-3 break-words text-sm text-slate-500 dark:text-white/45">{currentQuestion.promptText}</div>
              )}
            </div>

            {renderQuestionBody(currentQuestion)}

            <div className="flex flex-wrap justify-between gap-3">
              <button
                type="button"
                disabled={selectedQuestionOrder <= 1}
                onClick={() => setSelectedQuestionOrder((current) => Math.max(1, current - 1))}
                className="inline-flex min-w-0 flex-1 items-center justify-center rounded-full border border-slate-200 px-4 py-3 text-sm font-bold disabled:opacity-40 sm:flex-none dark:border-white/10"
              >
                <ChevronLeft size={16} className="mr-1 inline-block" />
                上一题
              </button>
              <button
                type="button"
                disabled={selectedQuestionOrder >= detail.questionCount}
                onClick={() => setSelectedQuestionOrder((current) => Math.min(detail.questionCount, current + 1))}
                className="inline-flex min-w-0 flex-1 items-center justify-center rounded-full border border-slate-200 px-4 py-3 text-sm font-bold disabled:opacity-40 sm:flex-none dark:border-white/10"
              >
                下一题
                <ChevronRight size={16} className="ml-1 inline-block" />
              </button>
            </div>

            {!canEdit && detail.status === 'IN_PROGRESS' && (
              <div className="rounded-2xl border border-amber-500/20 bg-amber-500/10 px-4 py-4 text-sm text-amber-700 dark:text-amber-300">
                答卷已锁定，正在等待最终交卷结果。此时不能再修改答案。
              </div>
            )}
            {remainingMs !== null && remainingMs <= 5 * 60 * 1000 && remainingMs > 0 && (
              <div className="rounded-2xl border border-amber-500/20 bg-amber-500/10 px-4 py-4 text-sm text-amber-700 dark:text-amber-300">
                倒计时已进入最后 5 分钟，请尽快检查并交卷。
              </div>
            )}
            {remainingMs !== null && remainingMs <= 0 && (
              <div className="rounded-2xl border border-rose-500/20 bg-rose-500/10 px-4 py-4 text-sm text-rose-600 dark:text-rose-300">
                测评已到时限。{submitErrorMessage ? '自动交卷请求已失败，后台仍会继续补交；如未自动跳转，可点击“重新提交”。' : '系统正在自动交卷。'}
              </div>
            )}
            {answeredCountFromLocal === detail.questionCount && detail.questionCount > 0 && canEdit && (
              <div className="rounded-2xl border border-emerald-500/20 bg-emerald-500/10 px-4 py-4 text-sm text-emerald-600 dark:text-emerald-300">
                <CheckCircle2 size={14} className="mr-2 inline-block" />
                所有题目均已填写，可以直接交卷。
              </div>
            )}
          </section>
          </div>
        </>
      )}

      <ConfirmationDialog
        open={Boolean(submitConfirmOpen && detail?.status === 'IN_PROGRESS')}
        title={unansweredQuestionOrders.length > 0
          ? `你还有 ${unansweredQuestionOrders.length} 题未作答，确认提交？`
          : '所有题目已作答，确认提交？'}
        description="系统将提交当前答卷并结束本次作答。"
        safety="提交后答卷会被锁定，不能继续修改；已保存答案会随本次答卷一起提交。"
        nextStep="建议先检查未作答题目；确认答案完整后再提交。"
        confirmLabel="确认交卷并锁定"
        cancelLabel="继续检查"
        pending={isSubmitting}
        pendingTitle="正在提交答卷"
        pendingDescription="交卷请求已经提交，请留在当前页面等待结果。"
        onCancel={() => setSubmitConfirmOpen(false)}
        onConfirm={() => {
          void handleSubmit('manual');
        }}
        details={unansweredQuestionOrders.length > 0 ? (
              <div className="mt-5">
                <div className="text-sm font-bold text-slate-900 dark:text-white">未作答题号</div>
                <div className="mt-3 flex flex-wrap gap-2">
                  {unansweredQuestionOrders.map((questionOrder) => (
                    <button
                      key={questionOrder}
                      type="button"
                      onClick={() => {
                        setSelectedQuestionOrder(questionOrder);
                        setSubmitConfirmOpen(false);
                      }}
                      className="rounded-full border border-amber-500/30 bg-amber-500/10 px-3 py-2 text-sm font-bold text-amber-700 dark:text-amber-300"
                    >
                      第 {questionOrder} 题
                    </button>
                  ))}
                </div>
              </div>
        ) : undefined}
      />
    </div>
  );
};

export default StudentAssessmentAttemptPage;
