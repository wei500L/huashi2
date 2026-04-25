import React from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { CheckCircle2, ChevronLeft, ChevronRight, Clock3, Save, Send } from 'lucide-react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { PageHeader, SectionEyebrow, StatusBadge } from '@/components/common';
import { getApiErrorMessage, normalizeApiError } from '@/lib/api';
import { useBodyScrollLock, useDialogAccessibility } from '@/lib/a11y';
import { assessmentQuestionTypeLabel, formatDateTime } from '@/lib/format';
import { assessmentService } from '@/lib/services';
import type { AssessmentAttemptDetailVO, AssessmentAttemptQuestionVO } from '@/lib/contracts';
import { clearAssessmentDraft, readAssessmentDraft, writeAssessmentDraft } from '@/features/assessment/draftStorage';
import { enqueueSerializedTask } from '@/features/assessment/saveQueue';
import { useLeaveProtection } from '@/features/session-runtime/useLeaveProtection';

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

function buildSavePayload(detail: AssessmentAttemptDetailVO, responsesByOrder: Record<number, string[]>) {
  return {
    responses: detail.questions.map((question) => ({
      questionOrder: question.questionOrder,
      responses: responsesByOrder[question.questionOrder] || [],
    })),
  };
}

function hasResponses(responses?: string[]) {
  return !!responses?.map((item) => item.trim()).filter(Boolean).length;
}

function mergeDraftResponses(detail: AssessmentAttemptDetailVO) {
  const serverResponses = buildInitialResponses(detail);
  const draft = readAssessmentDraft(detail.attemptId);
  if (!draft) {
    return { responsesByOrder: serverResponses, restored: false };
  }
  const draftUpdatedAt = new Date(draft.updatedAt).getTime();
  const serverSavedAt = detail.lastSavedAt ? new Date(detail.lastSavedAt).getTime() : Number.NEGATIVE_INFINITY;
  if (!Number.isFinite(draftUpdatedAt) || draftUpdatedAt <= serverSavedAt) {
    return { responsesByOrder: serverResponses, restored: false };
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
  return { responsesByOrder: merged, restored };
}

type PersistMode = 'manual' | 'auto' | 'background';

const StudentAssessmentAttemptPage: React.FC = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const params = useParams<{ attemptId: string }>();
  const attemptId = Number(params.attemptId);
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
  const submitDialogRef = React.useRef<HTMLDivElement | null>(null);
  const submitCancelButtonRef = React.useRef<HTMLButtonElement | null>(null);

  const detailQuery = useQuery({
    queryKey: ['student-assessment-attempt', attemptId],
    queryFn: ({ signal }) => assessmentService.getStudentAttempt(attemptId, { signal }),
    enabled: Number.isFinite(attemptId),
    retry: false,
  });

  const heartbeatQuery = useQuery({
    queryKey: ['student-assessment-attempt-heartbeat', attemptId],
    queryFn: ({ signal }) => assessmentService.getStudentAttemptHeartbeat(attemptId, { signal }),
    enabled: Number.isFinite(attemptId) && detailQuery.data?.attemptId === attemptId,
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
    setResponsesByOrder(hydratedResponses.responsesByOrder);
    setAnsweredCount(detailQuery.data.answeredCount);
    setLastSavedAt(detailQuery.data.lastSavedAt || null);
    setSelectedQuestionOrder(detailQuery.data.questions.find((question) => !question.answered)?.questionOrder || detailQuery.data.questions[0]?.questionOrder || 1);
    setSaveNotice(hydratedResponses.restored ? '已恢复本地草稿。' : null);
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

  useBodyScrollLock(submitConfirmOpen);
  useDialogAccessibility({
    open: submitConfirmOpen,
    containerRef: submitDialogRef,
    initialFocusRef: submitCancelButtonRef,
    onClose: () => setSubmitConfirmOpen(false),
  });

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
          const payload = buildSavePayload(detail, snapshot);
          const progress = options?.keepalive
            ? await assessmentService.saveStudentResponsesKeepalive(attemptId, payload)
            : await assessmentService.saveStudentResponses(attemptId, payload);

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
        await persistResponses('manual', responsesByOrder, { ignoreLock: true, silentSuccess: true });
        const result = await assessmentService.submitStudentAttempt(attemptId);
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
        setSubmitLocked(false);
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
    [attemptId, detail, navigateToResult, persistResponses, queryClient, resolveSubmittedAttempt, responsesByOrder]
  );

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
      writeAssessmentDraft(attemptId, next);
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
      writeAssessmentDraft(attemptId, merged);
      return merged;
    });
    setSaveNotice(null);
    setSaveErrorMessage(null);
    setSubmitErrorMessage(null);
  }, [attemptId]);

  const updateFillBlankResponse = React.useCallback((questionOrder: number, value: string) => {
    setResponsesByOrder((current) => {
      const next = { ...current, [questionOrder]: value.trim() ? [value] : [] };
      writeAssessmentDraft(attemptId, next);
      return next;
    });
    setSaveNotice(null);
    setSaveErrorMessage(null);
    setSubmitErrorMessage(null);
  }, [attemptId]);

  const renderQuestionBody = (question: AssessmentAttemptQuestionVO) => {
    const responses = responsesByOrder[question.questionOrder] || [];
    if (question.questionType === 'FILL_BLANK') {
      return (
        <textarea
          value={responses[0] || ''}
          onChange={(event) => updateFillBlankResponse(question.questionOrder, event.target.value)}
          rows={5}
          disabled={!canEdit}
          className="w-full rounded-[1.8rem] border border-slate-200 bg-white/75 px-4 py-3 text-base disabled:opacity-70 dark:border-white/10 dark:bg-white/5"
          placeholder="请输入答案"
        />
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
                event.preventDefault();
                if (question.questionType === 'SINGLE_CHOICE') {
                  updateSingleResponse(question.questionOrder, option.key);
                } else {
                  toggleMultipleResponse(question.questionOrder, option.key);
                }
              }}
              className={`flex items-start gap-3 rounded-[1.4rem] border px-4 py-4 text-sm transition-all ${
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
                readOnly
              />
              <div>
                <div className="font-semibold">{option.key}</div>
                <div className="mt-1">{option.label}</div>
              </div>
            </label>
          );
        })}
      </div>
    );
  };

  return (
    <div className="space-y-8 pb-20">
      <PageHeader
        eyebrow="通用测评"
        title={detail?.paperTitle || '测评作答'}
        subtitle={detail ? `${detail.className} · 整卷时长 ${detail.durationMinutes} 分钟 · 截止 ${formatDateTime(detail.expiresAt)}` : '正在加载测评内容'}
        actions={
          <div className="flex flex-wrap gap-3">
            <Link to="/assessments" className="rounded-full border border-slate-200 px-4 py-3 text-sm dark:border-white/10">
              返回任务列表
            </Link>
            <button
              type="button"
              disabled={!canEdit || isSaving || isSubmitting}
              onClick={() => void persistResponses('manual', responsesByOrder)}
              className="rounded-full border border-slate-200 px-4 py-3 text-sm font-bold text-primary disabled:opacity-60 dark:border-white/10"
            >
              <Save size={14} className="inline-block mr-2" />
              保存答案
            </button>
            <button
              type="button"
              disabled={!detail || detail.status !== 'IN_PROGRESS' || isSubmitting}
              onClick={() => setSubmitConfirmOpen(true)}
              className="btn-liquid px-5 py-3 text-white disabled:opacity-60"
            >
              <Send size={14} className="inline-block mr-2" />
              {submitLocked && submitErrorMessage ? '重新提交' : '交卷'}
            </button>
          </div>
        }
      />

      {(saveNotice || saveErrorMessage || submitErrorMessage) && (
        <div
          className={`rounded-[1.8rem] px-5 py-4 text-sm ${
            submitErrorMessage || saveErrorMessage
              ? 'border border-rose-500/20 bg-rose-500/5 text-rose-500'
              : isSubmitting || isSaving
                ? 'border border-sky-500/20 bg-sky-500/5 text-sky-700 dark:text-sky-300'
                : 'border border-emerald-500/20 bg-emerald-500/5 text-emerald-600 dark:text-emerald-400'
          }`}
        >
          {submitErrorMessage || saveErrorMessage || saveNotice}
        </div>
      )}

      {detailQuery.error && (
        <div className="rounded-[2rem] border border-rose-500/20 bg-rose-500/5 p-6 text-rose-500">
          {getApiErrorMessage(detailQuery.error)}
        </div>
      )}

      {detailQuery.isLoading && (
        <div className="rounded-[2.2rem] liquid-glass-panel p-8 text-sm text-slate-500 dark:text-white/45">
          正在加载测评内容...
        </div>
      )}

      {detail && currentQuestion && (
        <div className="grid gap-8 xl:grid-cols-[280px_1fr]">
          <aside className="space-y-5 rounded-[2.4rem] liquid-glass-panel p-6 md:p-8">
            <div className="rounded-[1.6rem] border border-slate-200/70 bg-white/75 px-4 py-4 dark:border-white/10 dark:bg-white/5">
              <SectionEyebrow>剩余时间</SectionEyebrow>
              <div className={`mt-3 text-3xl font-black ${remainingMs !== null && remainingMs <= 5 * 60 * 1000 ? 'text-rose-500' : 'text-slate-900 dark:text-white'}`}>
                {remainingMs === null ? '--:--' : formatRemaining(remainingMs)}
              </div>
              <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                <Clock3 size={14} className="mr-2 inline-block" />
                最后保存 {formatDateTime(lastSavedAt)}
              </div>
            </div>

            <div className="grid grid-cols-4 gap-2">
              {orderedQuestions.map((question) => {
                const answered = hasResponses(responsesByOrder[question.questionOrder]);
                const selected = question.questionOrder === currentQuestion.questionOrder;
                return (
                  <button
                    key={question.questionOrder}
                    type="button"
                    onClick={() => setSelectedQuestionOrder(question.questionOrder)}
                    className={`rounded-2xl px-3 py-3 text-sm font-bold transition-all ${
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

            <div className="space-y-2 text-sm text-slate-500 dark:text-white/45">
              <div>已作答：{answeredCountFromLocal || answeredCount} / {detail.questionCount}</div>
              <div>总分：{detail.totalScore}</div>
              <div>开始时间：{formatDateTime(detail.startedAt)}</div>
            </div>

            {detail.instructionsText && (
              <div className="rounded-[1.6rem] border border-dashed border-slate-200/80 px-4 py-4 text-sm text-slate-600 dark:border-white/10 dark:text-white/60">
                {detail.instructionsText}
              </div>
            )}
          </aside>

          <section className="space-y-6 rounded-[2.4rem] liquid-glass-panel p-6 md:p-8">
            <div className="flex flex-wrap items-start justify-between gap-4">
              <div>
                <SectionEyebrow>
                  第 {currentQuestion.questionOrder} 题 / 共 {detail.questionCount} 题
                </SectionEyebrow>
                <div className="mt-3 text-3xl font-black text-slate-900 dark:text-white">{assessmentQuestionTypeLabel(currentQuestion.questionType)}</div>
              </div>
              <StatusBadge label={`${currentQuestion.score} 分`} tone="neutral" className="px-4 py-2 text-sm" />
            </div>

            <div className="rounded-[1.8rem] border border-slate-200/70 bg-white/75 p-5 dark:border-white/10 dark:bg-white/5">
              <div className="text-lg font-semibold leading-8 text-slate-900 dark:text-white">{currentQuestion.stemText}</div>
              {currentQuestion.promptText && (
                <div className="mt-3 text-sm text-slate-500 dark:text-white/45">{currentQuestion.promptText}</div>
              )}
            </div>

            {renderQuestionBody(currentQuestion)}

            <div className="flex flex-wrap justify-between gap-3">
              <button
                type="button"
                disabled={selectedQuestionOrder <= 1}
                onClick={() => setSelectedQuestionOrder((current) => Math.max(1, current - 1))}
                className="rounded-full border border-slate-200 px-4 py-3 text-sm font-bold disabled:opacity-40 dark:border-white/10"
              >
                <ChevronLeft size={16} className="inline-block mr-1" />
                上一题
              </button>
              <button
                type="button"
                disabled={selectedQuestionOrder >= detail.questionCount}
                onClick={() => setSelectedQuestionOrder((current) => Math.min(detail.questionCount, current + 1))}
                className="rounded-full border border-slate-200 px-4 py-3 text-sm font-bold disabled:opacity-40 dark:border-white/10"
              >
                下一题
                <ChevronRight size={16} className="inline-block ml-1" />
              </button>
            </div>

            {!canEdit && detail.status === 'IN_PROGRESS' && (
              <div className="rounded-[1.6rem] border border-amber-500/20 bg-amber-500/10 px-4 py-4 text-sm text-amber-700 dark:text-amber-300">
                答卷已锁定，正在等待最终交卷结果。此时不能再修改答案。
              </div>
            )}
            {remainingMs !== null && remainingMs <= 5 * 60 * 1000 && remainingMs > 0 && (
              <div className="rounded-[1.6rem] border border-amber-500/20 bg-amber-500/10 px-4 py-4 text-sm text-amber-700 dark:text-amber-300">
                倒计时已进入最后 5 分钟，请尽快检查并交卷。
              </div>
            )}
            {remainingMs !== null && remainingMs <= 0 && (
              <div className="rounded-[1.6rem] border border-rose-500/20 bg-rose-500/10 px-4 py-4 text-sm text-rose-600 dark:text-rose-300">
                测评已到时限。{submitErrorMessage ? '自动交卷请求已失败，后台仍会继续补交；如未自动跳转，可点击“重新提交”。' : '系统正在自动交卷。'}
              </div>
            )}
            {answeredCountFromLocal === detail.questionCount && detail.questionCount > 0 && canEdit && (
              <div className="rounded-[1.6rem] border border-emerald-500/20 bg-emerald-500/10 px-4 py-4 text-sm text-emerald-600 dark:text-emerald-300">
                <CheckCircle2 size={14} className="mr-2 inline-block" />
                所有题目均已填写，可以直接交卷。
              </div>
            )}
          </section>
        </div>
      )}

      {submitConfirmOpen && detail?.status === 'IN_PROGRESS' && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/45 px-4 backdrop-blur-sm">
          <div
            ref={submitDialogRef}
            role="dialog"
            aria-modal="true"
            aria-labelledby="assessment-submit-title"
            className="w-full max-w-xl rounded-[2rem] border border-slate-200/80 bg-white/96 p-6 shadow-[0_24px_80px_rgba(15,23,42,0.2)] dark:border-white/10 dark:bg-slate-950/96"
          >
            <SectionEyebrow>交卷确认</SectionEyebrow>
            <div id="assessment-submit-title" className="mt-3 text-2xl font-black text-slate-900 dark:text-white">
              {unansweredQuestionOrders.length > 0
                ? `你还有 ${unansweredQuestionOrders.length} 题未作答，确认提交？`
                : '所有题目已作答，确认提交？'}
            </div>
            <div className="mt-3 text-sm leading-7 text-slate-500 dark:text-white/50">
              提交后将锁定答卷，不能继续修改。建议先检查未完成题目，再决定是否立即交卷。
            </div>
            {unansweredQuestionOrders.length > 0 && (
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
            )}
            <div className="mt-6 flex flex-wrap justify-end gap-3">
              <button
                ref={submitCancelButtonRef}
                type="button"
                onClick={() => setSubmitConfirmOpen(false)}
                className="rounded-full border border-slate-200 px-4 py-3 text-sm font-bold dark:border-white/10"
              >
                继续检查
              </button>
              <button
                type="button"
                onClick={() => {
                  setSubmitConfirmOpen(false);
                  void handleSubmit('manual');
                }}
                className="btn-liquid px-5 py-3 text-white"
              >
                确认交卷
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default StudentAssessmentAttemptPage;
