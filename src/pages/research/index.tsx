import React from 'react';
import { CheckCircle2, ChevronLeft, ChevronRight, Clock3, Save, Send } from 'lucide-react';
import { useParams } from 'react-router-dom';
import { getApiErrorMessage, normalizeApiError } from '@/lib/api';
import type {
  PublicAssessmentAttemptVO,
  PublicAssessmentMetadataVO,
  PublicAssessmentQuestionVO,
  PublicAssessmentResultVO,
} from '@/lib/contracts';
import { publicAssessmentService } from '@/lib/services';

type ResponsesByOrder = Record<number, string[]>;
type JustificationsByOrder = Record<number, string>;

const hasResponse = (values?: string[]) => Boolean(values?.some((value) => value.trim().length > 0));

function hydrateResponses(attempt: PublicAssessmentAttemptVO) {
  const responses: ResponsesByOrder = {};
  const justifications: JustificationsByOrder = {};
  attempt.questions.forEach((question) => {
    responses[question.questionOrder] = question.responses || [];
    justifications[question.questionOrder] = question.justificationText || '';
  });
  return { responses, justifications };
}

const PublicQuestion: React.FC<{
  question: PublicAssessmentQuestionVO;
  responses: string[];
  justification: string;
  disabled: boolean;
  onResponsesChange: (responses: string[]) => void;
  onJustificationChange: (value: string) => void;
}> = ({ question, responses, justification, disabled, onResponsesChange, onJustificationChange }) => {
  const type = question.questionType;

  if (type === 'INSTRUCTION') {
    return <p className="whitespace-pre-wrap leading-8 text-slate-700 dark:text-white/70">{question.promptText || question.stemText}</p>;
  }

  if (type === 'SINGLE_CHOICE' || type === 'INFORMED_CONSENT' || type === 'TRUE_FALSE_WITH_JUSTIFICATION') {
    return (
      <div className="space-y-3">
        {question.options.map((option) => (
          <label key={option.key} className="flex cursor-pointer gap-3 rounded-2xl border border-slate-200 p-4 dark:border-white/10">
            <input
              type="radio"
              name={`question-${question.questionOrder}`}
              value={option.key}
              checked={responses[0] === option.key}
              disabled={disabled}
              onChange={() => onResponsesChange([option.key])}
              className="mt-1"
            />
            <span>{option.label}</span>
          </label>
        ))}
        {type === 'TRUE_FALSE_WITH_JUSTIFICATION' && responses[0] === 'F' ? (
          <label className="block pt-2 text-sm font-semibold text-slate-700 dark:text-white/70">
            请说明判断为错误的原因
            <textarea
              value={justification}
              disabled={disabled}
              onChange={(event) => onJustificationChange(event.target.value)}
              rows={4}
              className="mt-2 w-full rounded-2xl border border-slate-200 bg-white p-4 font-normal outline-none focus:border-primary dark:border-white/10 dark:bg-white/5"
            />
          </label>
        ) : null}
      </div>
    );
  }

  if (type === 'MULTIPLE_CHOICE') {
    return (
      <div className="space-y-3">
        {question.options.map((option) => {
          const selected = responses.includes(option.key);
          return (
            <label key={option.key} className="flex cursor-pointer gap-3 rounded-2xl border border-slate-200 p-4 dark:border-white/10">
              <input
                type="checkbox"
                value={option.key}
                checked={selected}
                disabled={disabled}
                onChange={() => onResponsesChange(selected ? responses.filter((value) => value !== option.key) : [...responses, option.key])}
                className="mt-1"
              />
              <span>{option.label}</span>
            </label>
          );
        })}
      </div>
    );
  }

  return (
    <input
      type={type === 'NUMBER' ? 'number' : 'text'}
      value={responses[0] || ''}
      disabled={disabled}
      onChange={(event) => onResponsesChange(event.target.value ? [event.target.value] : [])}
      className="w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 outline-none focus:border-primary dark:border-white/10 dark:bg-white/5"
      placeholder="请输入答案"
    />
  );
};

const PublicResult: React.FC<{ result: PublicAssessmentResultVO }> = ({ result }) => (
  <main className="mx-auto min-h-screen max-w-4xl px-5 py-12">
    <section className="rounded-[2rem] border border-emerald-200 bg-white p-7 shadow-sm dark:border-emerald-400/20 dark:bg-slate-900 md:p-10">
      <CheckCircle2 className="h-12 w-12 text-emerald-500" />
      <h1 className="mt-5 text-3xl font-black text-slate-900 dark:text-white">问卷已提交</h1>
      <p className="mt-2 text-slate-500">感谢参与“{result.paperTitle}”。重复打开此链接会返回同一份结果。</p>
      {result.scoreVisible ? (
        <div className="mt-7 rounded-3xl bg-slate-50 p-6 dark:bg-white/5">
          <div className="text-sm text-slate-500">规则评分</div>
          <div className="mt-1 text-4xl font-black text-primary">
            {result.objectiveScore ?? '—'}{result.totalScore != null ? ` / ${result.totalScore}` : ''}
          </div>
        </div>
      ) : null}
      {result.qualityFlags?.length ? (
        <div className="mt-6 rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800">
          数据质量提醒：{result.qualityFlags.join('、')}
        </div>
      ) : null}
      <div className="mt-7">
        <h2 className="text-lg font-bold text-slate-900 dark:text-white">AI 解读</h2>
        {result.aiAnalysis ? (
          <p className="mt-2 whitespace-pre-wrap text-sm leading-7 text-slate-600 dark:text-white/60">
            {result.aiAnalysis.performanceOverview}
          </p>
        ) : (
          <p className="mt-2 text-sm text-slate-500">分析状态：{result.aiAnalysisStatus || 'PENDING'}。稍后重新打开页面可查看更新。</p>
        )}
      </div>
    </section>
  </main>
);

const ResearchParticipantPage: React.FC = () => {
  const { releaseCode = '' } = useParams<{ releaseCode: string }>();
  const normalizedReleaseCode = releaseCode.trim();
  const [metadata, setMetadata] = React.useState<PublicAssessmentMetadataVO | null>(null);
  const [attempt, setAttempt] = React.useState<PublicAssessmentAttemptVO | null>(null);
  const [result, setResult] = React.useState<PublicAssessmentResultVO | null>(null);
  const [participationCode, setParticipationCode] = React.useState('');
  const [responsesByOrder, setResponsesByOrder] = React.useState<ResponsesByOrder>({});
  const [justificationsByOrder, setJustificationsByOrder] = React.useState<JustificationsByOrder>({});
  const [selectedIndex, setSelectedIndex] = React.useState(0);
  const [loading, setLoading] = React.useState(true);
  const [verifying, setVerifying] = React.useState(false);
  const [saving, setSaving] = React.useState(false);
  const [submitting, setSubmitting] = React.useState(false);
  const [errorMessage, setErrorMessage] = React.useState<string | null>(null);
  const [saveMessage, setSaveMessage] = React.useState<string | null>(null);
  const [dirtyRevision, setDirtyRevision] = React.useState(0);
  const hydratedRef = React.useRef(false);
  const currentVersionRef = React.useRef(1);

  const applyAttempt = React.useCallback((nextAttempt: PublicAssessmentAttemptVO) => {
    const hydrated = hydrateResponses(nextAttempt);
    currentVersionRef.current = nextAttempt.version;
    setAttempt(nextAttempt);
    setResponsesByOrder(hydrated.responses);
    setJustificationsByOrder(hydrated.justifications);
    setDirtyRevision(0);
    hydratedRef.current = true;
  }, []);

  React.useEffect(() => {
    if (!normalizedReleaseCode) {
      setErrorMessage('公开问卷链接无效。');
      setLoading(false);
      return;
    }
    const controller = new AbortController();
    const load = async () => {
      setLoading(true);
      setErrorMessage(null);
      try {
        const nextMetadata = await publicAssessmentService.getMetadata(normalizedReleaseCode, { signal: controller.signal });
        setMetadata(nextMetadata);
        try {
          const restoredAttempt = await publicAssessmentService.getAttempt(normalizedReleaseCode, { signal: controller.signal });
          applyAttempt(restoredAttempt);
          if (restoredAttempt.status === 'SUBMITTED') {
            setResult(await publicAssessmentService.getResult(normalizedReleaseCode, { signal: controller.signal }));
          }
        } catch (error) {
          if (normalizeApiError(error).status !== 401 && normalizeApiError(error).status !== 403) {
            throw error;
          }
        }
      } catch (error) {
        if (!controller.signal.aborted) setErrorMessage(getApiErrorMessage(error, '无法加载公开问卷。'));
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    };
    void load();
    return () => controller.abort();
  }, [applyAttempt, normalizedReleaseCode]);

  const buildResponses = React.useCallback(
    () =>
      (attempt?.questions || []).map((question) => ({
        questionOrder: question.questionOrder,
        responses: responsesByOrder[question.questionOrder] || [],
        justificationText: justificationsByOrder[question.questionOrder] || null,
      })),
    [attempt?.questions, justificationsByOrder, responsesByOrder]
  );

  const attemptId = attempt?.attemptId;
  const attemptStatus = attempt?.status;

  React.useEffect(() => {
    if (!attemptId || attemptStatus !== 'IN_PROGRESS' || !hydratedRef.current || dirtyRevision === 0) return;
    const timer = window.setTimeout(async () => {
      setSaving(true);
      setSaveMessage(null);
      try {
        const progress = await publicAssessmentService.saveResponses(normalizedReleaseCode, {
          responses: buildResponses(),
          baseVersion: currentVersionRef.current,
        });
        currentVersionRef.current = progress.version;
        setAttempt((current) => current ? { ...current, version: progress.version, answeredCount: progress.answeredCount, lastSavedAt: progress.lastSavedAt } : current);
        setSaveMessage('已自动保存');
      } catch (error) {
        setSaveMessage(getApiErrorMessage(error, '自动保存失败，请重试。'));
      } finally {
        setSaving(false);
      }
    }, 900);
    return () => window.clearTimeout(timer);
  }, [attemptId, attemptStatus, buildResponses, dirtyRevision, normalizedReleaseCode]);

  React.useEffect(() => {
    if (!attempt || attempt.status !== 'IN_PROGRESS') return;
    const report = () => {
      if (document.hidden || !document.hasFocus()) return;
      const question = attempt.questions[selectedIndex];
      if (!question) return;
      void publicAssessmentService.recordTiming(normalizedReleaseCode, {
        questionOrder: question.questionOrder,
        activeDurationMs: 15_000,
        eventId: crypto.randomUUID(),
      }).catch(() => undefined);
    };
    const timer = window.setInterval(report, 15_000);
    return () => window.clearInterval(timer);
  }, [attempt, normalizedReleaseCode, selectedIndex]);

  const verify = async (event: React.FormEvent) => {
    event.preventDefault();
    setVerifying(true);
    setErrorMessage(null);
    try {
      const session = await publicAssessmentService.verifyCode(normalizedReleaseCode, {
        participationCode: participationCode.trim().toUpperCase(),
      });
      applyAttempt(session.attempt);
      if (session.attempt.status === 'SUBMITTED') {
        setResult(await publicAssessmentService.getResult(normalizedReleaseCode));
      }
    } catch (error) {
      setErrorMessage(getApiErrorMessage(error, '参与码验证失败。'));
    } finally {
      setVerifying(false);
    }
  };

  const submit = async () => {
    if (!attempt || submitting) return;
    setSubmitting(true);
    setErrorMessage(null);
    try {
      await publicAssessmentService.submit(normalizedReleaseCode, {
        responses: buildResponses(),
        baseVersion: currentVersionRef.current,
        reason: 'MANUAL',
      });
      setResult(await publicAssessmentService.getResult(normalizedReleaseCode));
    } catch (error) {
      setErrorMessage(getApiErrorMessage(error, '提交失败，请检查必答题后重试。'));
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <div className="flex min-h-screen items-center justify-center text-slate-500">正在加载问卷…</div>;
  if (result) return <PublicResult result={result} />;

  if (!attempt) {
    return (
      <main className="mx-auto flex min-h-screen max-w-xl items-center px-5 py-12">
        <section className="w-full rounded-[2rem] border border-slate-200 bg-white p-7 shadow-sm dark:border-white/10 dark:bg-slate-900 md:p-10">
          <div className="text-sm font-black tracking-[0.2em] text-primary">LEXI-BRIDGE RESEARCH</div>
          <h1 className="mt-4 text-3xl font-black text-slate-900 dark:text-white">{metadata?.title || '公开研究问卷'}</h1>
          <p className="mt-3 leading-7 text-slate-500">{metadata?.description || metadata?.instructionsText || '请输入研究人员提供的一次性参与码。'}</p>
          <div className="mt-5 flex gap-4 text-sm text-slate-500">
            <span>{metadata?.questionCount ?? '—'} 题</span>
            <span>{metadata?.durationMinutes ?? '—'} 分钟</span>
          </div>
          <form onSubmit={verify} className="mt-8">
            <label className="text-sm font-bold text-slate-700 dark:text-white/70" htmlFor="participation-code">参与码</label>
            <input
              id="participation-code"
              value={participationCode}
              onChange={(event) => setParticipationCode(event.target.value.toUpperCase())}
              placeholder="XXXX-XXXX-XXXX"
              autoComplete="one-time-code"
              className="mt-2 w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 font-mono tracking-widest outline-none focus:border-primary dark:border-white/10 dark:bg-white/5"
            />
            {errorMessage ? <p className="mt-3 text-sm text-rose-600">{errorMessage}</p> : null}
            <button type="submit" disabled={verifying || !participationCode.trim()} className="btn-liquid mt-6 w-full px-5 py-3 text-white disabled:opacity-50">
              {verifying ? '正在验证…' : '验证并进入问卷'}
            </button>
          </form>
        </section>
      </main>
    );
  }

  const currentQuestion = attempt.questions[selectedIndex];
  const answeredCount = attempt.questions.filter((question) => hasResponse(responsesByOrder[question.questionOrder])).length;
  if (!currentQuestion) return <div className="p-8 text-center text-slate-500">问卷暂无可作答题目。</div>;

  return (
    <main className="min-h-screen bg-slate-50 px-4 py-6 dark:bg-slate-950 md:py-10">
      <div className="mx-auto max-w-4xl">
        <header className="mb-5 flex flex-wrap items-center justify-between gap-3 rounded-3xl bg-white p-5 shadow-sm dark:bg-slate-900">
          <div>
            <div className="text-sm font-bold text-primary">{attempt.paperTitle}</div>
            <div className="mt-1 text-xs text-slate-500">已答 {answeredCount} / {attempt.questionCount}</div>
          </div>
          <div className="flex items-center gap-2 text-xs text-slate-500">
            {saving ? <Clock3 className="h-4 w-4 animate-pulse" /> : <Save className="h-4 w-4" />}
            {saveMessage || (attempt.lastSavedAt ? '答卷已恢复' : '自动保存已开启')}
          </div>
        </header>

        <section className="rounded-[2rem] bg-white p-6 shadow-sm dark:bg-slate-900 md:p-9">
          {currentQuestion.sectionTitle ? <div className="text-xs font-black tracking-wider text-primary">{currentQuestion.sectionTitle}</div> : null}
          {currentQuestion.sharedMaterial ? <div className="mt-4 whitespace-pre-wrap rounded-2xl bg-slate-50 p-5 leading-7 dark:bg-white/5">{currentQuestion.sharedMaterial}</div> : null}
          <div className="mt-5 text-sm font-bold text-slate-400">第 {currentQuestion.questionOrder} 题</div>
          <h1 className="mt-2 whitespace-pre-wrap text-xl font-bold leading-8 text-slate-900 dark:text-white">{currentQuestion.stemText}</h1>
          {currentQuestion.promptText && currentQuestion.questionType !== 'INSTRUCTION' ? <p className="mt-3 whitespace-pre-wrap text-slate-500">{currentQuestion.promptText}</p> : null}
          <div className="mt-7">
            <PublicQuestion
              question={currentQuestion}
              responses={responsesByOrder[currentQuestion.questionOrder] || []}
              justification={justificationsByOrder[currentQuestion.questionOrder] || ''}
              disabled={submitting}
              onResponsesChange={(responses) => {
                setResponsesByOrder((current) => ({ ...current, [currentQuestion.questionOrder]: responses }));
                setDirtyRevision((value) => value + 1);
              }}
              onJustificationChange={(value) => {
                setJustificationsByOrder((current) => ({ ...current, [currentQuestion.questionOrder]: value }));
                setDirtyRevision((revision) => revision + 1);
              }}
            />
          </div>
          {errorMessage ? <p className="mt-5 text-sm text-rose-600">{errorMessage}</p> : null}
          <footer className="mt-8 flex items-center justify-between gap-3 border-t border-slate-100 pt-5 dark:border-white/10">
            <button type="button" disabled={selectedIndex === 0} onClick={() => setSelectedIndex((value) => Math.max(0, value - 1))} className="inline-flex items-center gap-2 rounded-xl px-4 py-2 text-sm font-bold disabled:opacity-30">
              <ChevronLeft className="h-4 w-4" />上一题
            </button>
            {selectedIndex < attempt.questions.length - 1 ? (
              <button type="button" onClick={() => setSelectedIndex((value) => Math.min(attempt.questions.length - 1, value + 1))} className="btn-liquid inline-flex items-center gap-2 px-4 py-2 text-sm text-white">
                下一题<ChevronRight className="h-4 w-4" />
              </button>
            ) : (
              <button type="button" disabled={submitting} onClick={() => void submit()} className="btn-liquid inline-flex items-center gap-2 px-5 py-2 text-sm text-white disabled:opacity-50">
                <Send className="h-4 w-4" />{submitting ? '正在提交…' : '提交问卷'}
              </button>
            )}
          </footer>
        </section>
      </div>
    </main>
  );
};

export default ResearchParticipantPage;
