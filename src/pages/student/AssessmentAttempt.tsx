import React from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { CheckCircle2, ChevronLeft, ChevronRight, Clock3, Save, Send } from 'lucide-react';
import { useBeforeUnload, useBlocker } from 'react-router';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { PageHeader } from '@/components/common';
import { getApiErrorMessage } from '@/lib/api';
import { formatDateTime } from '@/lib/format';
import { assessmentService } from '@/lib/services';
import type { AssessmentAttemptDetailVO, AssessmentAttemptQuestionVO } from '@/lib/contracts';

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

function questionTypeLabel(questionType: string) {
  switch (questionType) {
    case 'SINGLE_CHOICE':
      return '单选题';
    case 'MULTIPLE_CHOICE':
      return '多选题';
    case 'FILL_BLANK':
      return '填空题';
    default:
      return questionType;
  }
}

function hasResponses(responses?: string[]) {
  return !!responses?.map((item) => item.trim()).filter(Boolean).length;
}

const StudentAssessmentAttemptPage: React.FC = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const params = useParams<{ attemptId: string }>();
  const attemptId = Number(params.attemptId);
  const [selectedQuestionOrder, setSelectedQuestionOrder] = React.useState(1);
  const [responsesByOrder, setResponsesByOrder] = React.useState<Record<number, string[]>>({});
  const [answeredCount, setAnsweredCount] = React.useState(0);
  const [lastSavedAt, setLastSavedAt] = React.useState<string | null>(null);
  const [saveMessage, setSaveMessage] = React.useState<string | null>(null);
  const [saveErrorMessage, setSaveErrorMessage] = React.useState<string | null>(null);
  const [submitErrorMessage, setSubmitErrorMessage] = React.useState<string | null>(null);
  const [clientNow, setClientNow] = React.useState(Date.now());
  const [serverOffsetMs, setServerOffsetMs] = React.useState(0);
  const hydratedAttemptIdRef = React.useRef<number | null>(null);
  const allowNavigationRef = React.useRef(false);
  const autoSubmitTriggeredRef = React.useRef(false);

  const detailQuery = useQuery({
    queryKey: ['student-assessment-attempt', attemptId],
    queryFn: ({ signal }) => assessmentService.getStudentAttempt(attemptId, { signal }),
    enabled: Number.isFinite(attemptId),
    retry: false,
    refetchInterval: (query) => (query.state.data?.status === 'IN_PROGRESS' ? 30000 : false),
  });

  React.useEffect(() => {
    if (!detailQuery.data) {
      return;
    }
    setServerOffsetMs(new Date(detailQuery.data.serverTime).getTime() - Date.now());
    if (hydratedAttemptIdRef.current === detailQuery.data.attemptId) {
      return;
    }
    hydratedAttemptIdRef.current = detailQuery.data.attemptId;
    setResponsesByOrder(buildInitialResponses(detailQuery.data));
    setAnsweredCount(detailQuery.data.answeredCount);
    setLastSavedAt(detailQuery.data.lastSavedAt || null);
    const firstUnanswered = detailQuery.data.questions.find((question) => !question.answered)?.questionOrder;
    setSelectedQuestionOrder(firstUnanswered || detailQuery.data.questions[0]?.questionOrder || 1);
  }, [detailQuery.data]);

  React.useEffect(() => {
    const timer = window.setInterval(() => setClientNow(Date.now()), 1000);
    return () => window.clearInterval(timer);
  }, []);

  const detail = detailQuery.data;
  const active = detail?.status === 'IN_PROGRESS';
  const currentServerNow = clientNow + serverOffsetMs;
  const remainingMs = detail ? new Date(detail.expiresAt).getTime() - currentServerNow : null;
  const orderedQuestions = detail?.questions || [];
  const currentQuestion = orderedQuestions.find((question) => question.questionOrder === selectedQuestionOrder) || orderedQuestions[0];
  const answeredCountFromLocal = orderedQuestions.filter((question) => hasResponses(responsesByOrder[question.questionOrder])).length;

  React.useEffect(() => {
    if (detail?.status !== 'SUBMITTED') {
      return;
    }
    allowNavigationRef.current = true;
    navigate(`/assessments/attempts/${detail.attemptId}/result`, { replace: true });
  }, [detail?.attemptId, detail?.status, navigate]);

  const saveMutation = useMutation({
    mutationFn: () => assessmentService.saveStudentResponses(attemptId, buildSavePayload(detail as AssessmentAttemptDetailVO, responsesByOrder)),
    onSuccess: async (progress) => {
      setAnsweredCount(progress.answeredCount);
      setLastSavedAt(progress.lastSavedAt || null);
      setSaveMessage('答案已保存。');
      setSaveErrorMessage(null);
      setSubmitErrorMessage(null);
      await queryClient.invalidateQueries({ queryKey: ['student-assessments'] });
    },
    onError: (error) => {
      setSaveMessage(null);
      setSaveErrorMessage(getApiErrorMessage(error, '保存答案失败'));
    },
  });

  const submitMutation = useMutation({
    mutationFn: () => assessmentService.submitStudentAttempt(attemptId),
    onSuccess: async (result) => {
      await queryClient.invalidateQueries({ queryKey: ['student-assessments'] });
      await queryClient.invalidateQueries({ queryKey: ['student-assessment-attempt', attemptId] });
      allowNavigationRef.current = true;
      navigate(`/assessments/attempts/${result.attemptId}/result`, { replace: true });
    },
    onError: (error) => {
      setSubmitErrorMessage(getApiErrorMessage(error, '提交测评失败'));
    },
  });

  const autoSave = React.useCallback(async () => {
    if (!detail || detail.status !== 'IN_PROGRESS') {
      return;
    }
    try {
      const progress = await assessmentService.saveStudentResponses(attemptId, buildSavePayload(detail, responsesByOrder));
      setAnsweredCount(progress.answeredCount);
      setLastSavedAt(progress.lastSavedAt || null);
      setSaveErrorMessage(null);
    } catch (error) {
      setSaveErrorMessage(getApiErrorMessage(error, '自动保存失败'));
    }
  }, [attemptId, detail, responsesByOrder]);

  const handleSubmit = React.useCallback(async () => {
    if (!detail) {
      return;
    }
    setSubmitErrorMessage(null);
    setSaveMessage(null);
    try {
      await assessmentService.saveStudentResponses(attemptId, buildSavePayload(detail, responsesByOrder));
      await submitMutation.mutateAsync();
    } catch (error) {
      setSubmitErrorMessage(getApiErrorMessage(error, '提交测评失败'));
    }
  }, [attemptId, detail, responsesByOrder, submitMutation]);

  React.useEffect(() => {
    if (!detail || detail.status !== 'IN_PROGRESS' || remainingMs === null || remainingMs > 0 || autoSubmitTriggeredRef.current) {
      return;
    }
    autoSubmitTriggeredRef.current = true;
    void handleSubmit();
  }, [detail, handleSubmit, remainingMs]);

  const blocker = useBlocker(() => active && !allowNavigationRef.current);

  React.useEffect(() => {
    if (blocker.state !== 'blocked') {
      return;
    }
    const shouldLeave = window.confirm('当前测评仍在进行中。离开页面前会尝试自动保存，确认离开吗？');
    if (shouldLeave) {
      allowNavigationRef.current = true;
      void autoSave().finally(() => {
        blocker.proceed();
        window.setTimeout(() => {
          allowNavigationRef.current = false;
        }, 0);
      });
      return;
    }
    blocker.reset();
  }, [autoSave, blocker]);

  useBeforeUnload(
    React.useCallback(
      (event) => {
        if (!active || allowNavigationRef.current) {
          return;
        }
        event.preventDefault();
        event.returnValue = '当前测评仍在进行中。';
        void autoSave();
      },
      [active, autoSave]
    )
  );

  React.useEffect(() => {
    if (!active) {
      return;
    }
    const handleVisibilityChange = () => {
      if (document.hidden) {
        void autoSave();
      }
    };
    document.addEventListener('visibilitychange', handleVisibilityChange);
    return () => document.removeEventListener('visibilitychange', handleVisibilityChange);
  }, [active, autoSave]);

  const updateSingleResponse = React.useCallback((questionOrder: number, value: string) => {
    setResponsesByOrder((current) => ({ ...current, [questionOrder]: value ? [value] : [] }));
    setSaveMessage(null);
    setSaveErrorMessage(null);
    setSubmitErrorMessage(null);
  }, []);

  const toggleMultipleResponse = React.useCallback((questionOrder: number, value: string) => {
    setResponsesByOrder((current) => {
      const existing = current[questionOrder] || [];
      const next = existing.includes(value) ? existing.filter((item) => item !== value) : [...existing, value];
      return { ...current, [questionOrder]: next };
    });
    setSaveMessage(null);
    setSaveErrorMessage(null);
    setSubmitErrorMessage(null);
  }, []);

  const updateFillBlankResponse = React.useCallback((questionOrder: number, value: string) => {
    setResponsesByOrder((current) => ({ ...current, [questionOrder]: value.trim() ? [value] : [] }));
    setSaveMessage(null);
    setSaveErrorMessage(null);
    setSubmitErrorMessage(null);
  }, []);

  const renderQuestionBody = (question: AssessmentAttemptQuestionVO) => {
    const responses = responsesByOrder[question.questionOrder] || [];
    if (question.questionType === 'FILL_BLANK') {
      return (
        <textarea
          value={responses[0] || ''}
          onChange={(event) => updateFillBlankResponse(question.questionOrder, event.target.value)}
          rows={5}
          className="w-full rounded-[1.8rem] border border-slate-200 bg-white/75 px-4 py-3 text-base dark:border-white/10 dark:bg-white/5"
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
              className={`flex cursor-pointer items-start gap-3 rounded-[1.4rem] border px-4 py-4 text-sm transition-all ${
                checked
                  ? 'border-primary/30 bg-primary/10 text-slate-900 dark:text-white'
                  : 'border-slate-200/70 bg-white/70 text-slate-600 dark:border-white/10 dark:bg-white/5 dark:text-white/60'
              }`}
            >
              <input
                type={question.questionType === 'SINGLE_CHOICE' ? 'radio' : 'checkbox'}
                name={`question-${question.questionOrder}`}
                checked={checked}
                onChange={() =>
                  question.questionType === 'SINGLE_CHOICE'
                    ? updateSingleResponse(question.questionOrder, option.key)
                    : toggleMultipleResponse(question.questionOrder, option.key)
                }
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
        title={detail?.paperTitle || '测评作答'}
        subtitle={detail ? `${detail.className} · 整卷时长 ${detail.durationMinutes} 分钟 · 截止 ${formatDateTime(detail.expiresAt)}` : '正在加载测评内容'}
        actions={
          <div className="flex flex-wrap gap-3">
            <Link to="/assessments" className="rounded-full border border-slate-200 px-4 py-3 text-sm dark:border-white/10">
              返回任务列表
            </Link>
            <button
              type="button"
              disabled={!detail || detail.status !== 'IN_PROGRESS' || saveMutation.isPending}
              onClick={() => saveMutation.mutate()}
              className="rounded-full border border-slate-200 px-4 py-3 text-sm font-bold text-primary disabled:opacity-60 dark:border-white/10"
            >
              <Save size={14} className="inline-block mr-2" />
              保存答案
            </button>
            <button
              type="button"
              disabled={!detail || detail.status !== 'IN_PROGRESS' || submitMutation.isPending}
              onClick={() => void handleSubmit()}
              className="btn-liquid px-5 py-3 text-white disabled:opacity-60"
            >
              <Send size={14} className="inline-block mr-2" />
              交卷
            </button>
          </div>
        }
      />

      {(saveMessage || saveErrorMessage || submitErrorMessage) && (
        <div
          className={`rounded-[1.8rem] px-5 py-4 text-sm ${
            submitErrorMessage || saveErrorMessage
              ? 'border border-rose-500/20 bg-rose-500/5 text-rose-500'
              : 'border border-emerald-500/20 bg-emerald-500/5 text-emerald-600 dark:text-emerald-400'
          }`}
        >
          {submitErrorMessage || saveErrorMessage || saveMessage}
        </div>
      )}

      {detailQuery.error && (
        <div className="rounded-[2rem] border border-rose-500/20 bg-rose-500/5 p-6 text-rose-500">
          {detailQuery.error.message}
        </div>
      )}

      {detailQuery.isLoading && (
        <div className="rounded-[2.2rem] liquid-glass-panel p-8 text-sm text-slate-500 dark:text-white/45">
          正在加载测评内容...
        </div>
      )}

      {detail && currentQuestion && (
        <div className="grid gap-8 xl:grid-cols-[280px_1fr]">
          <aside className="rounded-[2.4rem] liquid-glass-panel p-6 md:p-8 space-y-5">
            <div className="rounded-[1.6rem] border border-slate-200/70 bg-white/75 px-4 py-4 dark:border-white/10 dark:bg-white/5">
              <div className="text-[10px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">timer</div>
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

          <section className="rounded-[2.4rem] liquid-glass-panel p-6 md:p-8 space-y-6">
            <div className="flex flex-wrap items-start justify-between gap-4">
              <div>
                <div className="text-[10px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">
                  Question {currentQuestion.questionOrder} / {detail.questionCount}
                </div>
                <div className="mt-3 text-3xl font-black text-slate-900 dark:text-white">{questionTypeLabel(currentQuestion.questionType)}</div>
              </div>
              <div className="rounded-full border border-slate-200/70 px-4 py-2 text-sm text-slate-500 dark:border-white/10 dark:text-white/45">
                {currentQuestion.score} 分
              </div>
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

            {remainingMs !== null && remainingMs <= 5 * 60 * 1000 && (
              <div className="rounded-[1.6rem] border border-amber-500/20 bg-amber-500/10 px-4 py-4 text-sm text-amber-700 dark:text-amber-300">
                倒计时已进入最后 5 分钟，请尽快检查并交卷。
              </div>
            )}
            {remainingMs !== null && remainingMs <= 0 && (
              <div className="rounded-[1.6rem] border border-rose-500/20 bg-rose-500/10 px-4 py-4 text-sm text-rose-600 dark:text-rose-300">
                测评已到时限，系统正在自动交卷。
              </div>
            )}
            {answeredCountFromLocal === detail.questionCount && detail.questionCount > 0 && (
              <div className="rounded-[1.6rem] border border-emerald-500/20 bg-emerald-500/10 px-4 py-4 text-sm text-emerald-600 dark:text-emerald-300">
                <CheckCircle2 size={14} className="mr-2 inline-block" />
                所有题目均已填写，可以直接交卷。
              </div>
            )}
          </section>
        </div>
      )}
    </div>
  );
};

export default StudentAssessmentAttemptPage;
