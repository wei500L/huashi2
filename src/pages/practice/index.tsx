import React from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import {
  ArrowLeft,
  BookOpenCheck,
  Check,
  CheckCircle2,
  ChevronRight,
  GraduationCap,
  History,
  Lightbulb,
  Loader2,
  RefreshCw,
  Sparkles,
  XCircle,
} from 'lucide-react';
import { PageHeader, PanelSkeleton } from '@/components/common';
import { ConfirmationDialog } from '@/components/common/ConfirmationDialog';
import { FadeContent } from '@/components/common/FadeContent';
import { getApiErrorMessage } from '@/lib/api';
import { aiService, practiceService } from '@/lib/services';
import { formatDateTime, formatPercentValue } from '@/lib/format';
import { useBodyScrollLock, useDialogAccessibility } from '@/lib/a11y';
import type {
  AiGuidanceResponseVO,
  PracticeBankVO,
  PracticeQuestionVO,
  PracticeQuestionTutorVO,
  PracticeResultQuestionVO,
} from '@/lib/contracts';

type Phase = 'boot' | 'home' | 'session' | 'result' | 'history';

const QUESTION_TYPE_LABELS: Record<string, string> = {
  SINGLE_CHOICE: '单项选择',
  TRUE_FALSE: '判断题',
  SPELLING: '单词拼写',
  MULTIPLE_CHOICE: '多项选择',
  FILL_BLANK: '填空',
};

const SAVE_DRAFT_DELAY_MS = 900;

function PracticeTutoringPanel({ tutoring, busy, onRetry }: {
  tutoring: AiGuidanceResponseVO | null;
  busy: boolean;
  onRetry: () => void;
}) {
  const { t } = useTranslation();
  if (busy && !tutoring) {
    return (
      <div className="rounded-[2rem] border border-sky-500/20 bg-sky-500/5 p-6 text-sm text-sky-800 dark:text-sky-200" aria-live="polite">
        <div className="flex items-center gap-3">
          <Loader2 size={18} className="animate-spin" />
          {t('practice.tutoringPending')}
        </div>
      </div>
    );
  }
  if (!tutoring) {
    return (
      <div className="rounded-[2rem] border border-slate-200/70 p-6 text-sm text-slate-500 dark:border-white/10 dark:text-white/45">
        {t('practice.tutoringEmpty')}
        <button type="button" onClick={onRetry} className="ml-3 inline-flex items-center gap-1 rounded-full border border-slate-200 px-3 py-1.5 text-xs font-bold dark:border-white/10">
          <RefreshCw size={12} /> {t('practice.tutoringRetry')}
        </button>
      </div>
    );
  }
  const isFallback = tutoring.generationSource === 'RULE_FALLBACK';
  return (
    <div className="space-y-4">
      {isFallback ? (
        <div className="rounded-[1.4rem] border border-amber-500/20 bg-amber-500/5 px-4 py-2.5 text-xs text-amber-700 dark:text-amber-300">
          {t('practice.tutoringFallback')}
        </div>
      ) : null}
      <p className="text-base leading-7 text-slate-800 dark:text-white/85">{tutoring.explanation}</p>

      {tutoring.recommendationPath?.length ? (
        <div className="rounded-[1.6rem] border border-slate-200/70 bg-white/60 p-4 dark:border-white/10 dark:bg-white/5">
          <div className="text-sm font-bold text-slate-900 dark:text-white">{t('practice.nextStep')}</div>
          <ol className="mt-3 space-y-3">
            {tutoring.recommendationPath.map((item, index) => (
              <li key={index} className="flex items-start gap-3 text-sm leading-6">
                <span className="mt-0.5 flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-primary/10 text-xs font-black text-primary">
                  {String(index + 1).padStart(2, '0')}
                </span>
                <span className="min-w-0 text-slate-600 dark:text-white/60">
                  <span className="font-bold text-slate-900 dark:text-white">{item.title}</span>
                  {item.reason ? <span> - {item.reason}</span> : null}
                </span>
              </li>
            ))}
          </ol>
        </div>
      ) : null}

      {tutoring.focusLexicalPairs?.length ? (
        <div className="rounded-[1.6rem] border border-slate-200/70 bg-white/60 p-4 dark:border-white/10 dark:bg-white/5">
          <div className="text-sm font-bold text-slate-900 dark:text-white">{t('practice.focusWords')}</div>
          <div className="mt-3 space-y-2">
            {tutoring.focusLexicalPairs.map((pair) => (
              <div key={`${pair.lexicalPairId}-${pair.frenchWord}`} className="flex flex-wrap items-baseline gap-x-3 gap-y-1 text-sm">
                <span className="font-black text-slate-900 dark:text-white">{pair.englishWord} / {pair.frenchWord}</span>
                {pair.chineseGloss ? <span className="text-slate-500 dark:text-white/45">{pair.chineseGloss}</span> : null}
                <span className="text-xs text-slate-400 dark:text-white/30">{pair.focusReason}</span>
              </div>
            ))}
          </div>
        </div>
      ) : null}

      {tutoring.diagnosisInsight ? (
        <div className="grid gap-3">
          {[
            [t('diagnosis.strengths'), tutoring.diagnosisInsight.strengths, 'border-emerald-500/20 bg-emerald-500/5'] as const,
            [t('diagnosis.weaknesses'), tutoring.diagnosisInsight.weaknesses, 'border-rose-500/20 bg-rose-500/5'] as const,
            [t('diagnosis.suggestions'), tutoring.diagnosisInsight.suggestions, 'border-sky-500/20 bg-sky-500/5'] as const,
          ].map(([title, items, tone]) => (
            <div key={title} className={`rounded-[1.4rem] border p-4 ${tone}`}>
              <div className="text-sm font-bold text-slate-900 dark:text-white">{title}</div>
              <ul className="mt-2 space-y-1.5 text-sm leading-6 text-slate-600 dark:text-white/60">
                {items.map((item) => <li key={item}>{item}</li>)}
              </ul>
            </div>
          ))}
        </div>
      ) : null}

      {tutoring.teacherNote ? (
        <div className="rounded-[1.6rem] border border-slate-200/70 bg-white/60 p-4 dark:border-white/10 dark:bg-white/5">
          <div className="text-sm font-bold text-slate-900 dark:text-white">{t('diagnosis.teacherNote')}</div>
          <div className="mt-2 text-sm leading-6 text-slate-500 dark:text-white/45">{tutoring.teacherNote}</div>
        </div>
      ) : null}
    </div>
  );
}

function PracticeQuestionTutor({ tutor, loading, onRequest }: {
  tutor: PracticeQuestionTutorVO | null;
  loading: boolean;
  onRequest: () => void;
}) {
  const { t } = useTranslation();
  if (loading) {
    return (
      <div className="mt-3 rounded-[1.4rem] border border-sky-500/20 bg-sky-500/5 p-4 text-sm text-sky-800 dark:text-sky-200" aria-live="polite">
        <div className="flex items-center gap-2">
          <Loader2 size={15} className="animate-spin" /> {t('practice.aiTutorLoading')}
        </div>
      </div>
    );
  }
  if (!tutor) {
    return (
      <button
        type="button"
        onClick={onRequest}
        className="mt-3 inline-flex items-center gap-2 rounded-full border border-primary/30 bg-primary/5 px-4 py-2 text-sm font-bold text-primary"
      >
        <Sparkles size={15} /> {t('practice.aiTutorButton')}
      </button>
    );
  }
  return (
    <FadeContent contentKey={`practice-tutor-${tutor.questionOrder}-${tutor.generationSource}`} className="mt-3">
      <div className="rounded-[1.4rem] border border-primary/15 bg-primary/[0.04] p-4 dark:bg-primary/[0.06]">
        {tutor.generationSource === 'RULE_FALLBACK' ? (
          <div className="mb-2 inline-flex items-center gap-1 rounded-full border border-amber-500/20 bg-amber-500/5 px-2.5 py-1 text-[10px] font-bold uppercase tracking-wider text-amber-600 dark:text-amber-300">
            {t('practice.aiTutorFallback')}
          </div>
        ) : null}
        <p className="whitespace-pre-line text-sm leading-7 text-slate-800 dark:text-white/85">{tutor.explanation}</p>
        {tutor.commonMistake ? (
          <div className="mt-3 text-sm">
            <span className="font-bold text-slate-900 dark:text-white">{t('practice.commonMistake')}：</span>
            <span className="text-slate-600 dark:text-white/60">{tutor.commonMistake}</span>
          </div>
        ) : null}
        {tutor.memoryTip ? (
          <div className="mt-2 flex items-start gap-2 text-sm">
            <Lightbulb size={15} className="mt-0.5 shrink-0 text-amber-500" />
            <span className="text-slate-600 dark:text-white/60">
              <span className="font-bold text-slate-900 dark:text-white">{t('practice.memoryTip')}：</span>{tutor.memoryTip}
            </span>
          </div>
        ) : null}
        {tutor.relatedWords.length ? (
          <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
            <span className="font-bold text-slate-900 dark:text-white">{t('practice.relatedWords')}：</span>
            {tutor.relatedWords.join('、')}
          </div>
        ) : null}
      </div>
    </FadeContent>
  );
}

const PracticePage: React.FC = () => {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const [phase, setPhase] = React.useState<Phase>('boot');
  const [sessionId, setSessionId] = React.useState<number | null>(null);
  const bankCode = 'LEXIBRIDGE_FF4_V2';
  const [sectionCode, setSectionCode] = React.useState<string | null>(null);
  const [resumeCandidate, setResumeCandidate] = React.useState<{ sessionId: number; sectionCode: string | null } | null>(null);
  const [abandonConfirmOpen, setAbandonConfirmOpen] = React.useState(false);
  const abandonDialogRef = React.useRef<HTMLDivElement | null>(null);
  const abandonConfirmButtonRef = React.useRef<HTMLButtonElement | null>(null);
  useBodyScrollLock(abandonConfirmOpen);
  useDialogAccessibility({
    open: abandonConfirmOpen,
    containerRef: abandonDialogRef,
    initialFocusRef: abandonConfirmButtonRef,
    onClose: () => setAbandonConfirmOpen(false),
  });

  const banksQuery = useQuery({
    queryKey: ['practice-banks'],
    queryFn: ({ signal }) => practiceService.listBanks({ signal }),
  });
  const bank = banksQuery.data?.[0] as PracticeBankVO | undefined;

  const historyQuery = useQuery({
    queryKey: ['practice-history', 1, 6],
    queryFn: ({ signal }) => practiceService.listHistory({ pageNo: 1, pageSize: 6 }, { signal }),
  });

  React.useEffect(() => {
    if (phase !== 'boot' || !historyQuery.data) {
      return;
    }
    const inProgress = historyQuery.data.records.find((record) => record.status === 'IN_PROGRESS');
    if (inProgress) {
      setResumeCandidate({ sessionId: inProgress.sessionId, sectionCode: inProgress.sectionCode });
    }
    setPhase('home');
  }, [historyQuery.data, phase]);

  const startMutation = useMutation({
    mutationFn: (payload: { bankCode: string; sectionCode?: string | null; targetWords?: string[] }) =>
      practiceService.startSession(payload),
    onSuccess: (created) => {
      setSessionId(created.sessionId);
      setSectionCode(created.sectionCode);
      setPhase('session');
      void queryClient.invalidateQueries({ queryKey: ['practice-history'] });
    },
  });

  const abandonMutation = useMutation({
    mutationFn: (id: number) => practiceService.abandon(id),
    onSuccess: () => {
      setAbandonConfirmOpen(false);
      setResumeCandidate(null);
      setSessionId(null);
      setPhase('home');
      void queryClient.invalidateQueries({ queryKey: ['practice-history'] });
    },
  });

  const goHome = () => {
    setSessionId(null);
    setResumeCandidate(null);
    setPhase('home');
    void queryClient.invalidateQueries({ queryKey: ['practice-history'] });
  };

  if (phase === 'boot') {
    return (
      <div className="mx-auto max-w-5xl">
        <PanelSkeleton className="min-h-[360px]" />
      </div>
    );
  }

  if (phase === 'history') {
    return (
      <>
        <PracticeHistoryView onBack={goHome} />
        <AbandonDialog
          open={abandonConfirmOpen}
          pending={abandonMutation.isPending}
          onCancel={() => setAbandonConfirmOpen(false)}
          onConfirm={() => {
            if (sessionId != null) {
              void abandonMutation.mutate(sessionId);
            } else if (resumeCandidate) {
              void abandonMutation.mutate(resumeCandidate.sessionId);
            }
          }}
        />
      </>
    );
  }

  if (phase === 'session' && sessionId != null) {
    return (
      <>
        <PracticeSessionView
          sessionId={sessionId}
          onAbandon={() => setAbandonConfirmOpen(true)}
          onComplete={() => { setResumeCandidate(null); setPhase('result'); }}
        />
        <AbandonDialog
          open={abandonConfirmOpen}
          pending={abandonMutation.isPending}
          onCancel={() => setAbandonConfirmOpen(false)}
          onConfirm={() => {
            if (sessionId != null) {
              void abandonMutation.mutate(sessionId);
            } else if (resumeCandidate) {
              void abandonMutation.mutate(resumeCandidate.sessionId);
            }
          }}
        />
      </>
    );
  }

  if (phase === 'result' && sessionId != null) {
    return (
      <>
        <PracticeResultView
          sessionId={sessionId}
          sectionCode={sectionCode}
          starting={startMutation.isPending}
          onRetake={() => startMutation.mutate({ bankCode, sectionCode })}
          onRetakeWrongWords={(targetWords) => startMutation.mutate({ bankCode, sectionCode: null, targetWords })}
          onBackHome={goHome}
        />
        <AbandonDialog
          open={abandonConfirmOpen}
          pending={abandonMutation.isPending}
          onCancel={() => setAbandonConfirmOpen(false)}
          onConfirm={() => {
            if (sessionId != null) {
              void abandonMutation.mutate(sessionId);
            } else if (resumeCandidate) {
              void abandonMutation.mutate(resumeCandidate.sessionId);
            }
          }}
        />
      </>
    );
  }

  const recent = (historyQuery.data?.records || []).filter((record) => record.status !== 'IN_PROGRESS').slice(0, 3);

  return (
    <div className="page-stack">
      <PageHeader title={t('practice.homeTitle')} subtitle={t('practice.homeSubtitle')} />

      {startMutation.error ? (
        <div className="rounded-2xl border border-rose-500/20 bg-rose-500/5 px-4 py-3 text-sm text-rose-500 sm:rounded-[2rem] sm:px-6 sm:py-4" role="alert">
          {getApiErrorMessage(startMutation.error)}
        </div>
      ) : null}

      {resumeCandidate ? (
        <section className="rounded-2xl border border-sky-500/20 bg-sky-500/5 px-4 py-4 text-sm sm:rounded-[2rem] sm:px-6">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div className="flex items-center gap-3 text-sky-800 dark:text-sky-200">
              <BookOpenCheck size={18} />
              <div>
                <div className="font-bold">{t('practice.resumeInProgress')}</div>
                <div className="text-xs opacity-70">{t('practice.resumeHint')}</div>
              </div>
            </div>
            <div className="flex flex-wrap gap-2">
              <button
                type="button"
                onClick={() => { setSessionId(resumeCandidate.sessionId); setSectionCode(resumeCandidate.sectionCode); setPhase('session'); }}
                className="rounded-full bg-slate-900 px-4 py-2 text-xs font-bold text-white dark:bg-white dark:text-slate-900"
              >
                {t('practice.resume')}
              </button>
              <button
                type="button"
                onClick={() => { setAbandonConfirmOpen(true); }}
                className="rounded-full border border-slate-200 px-4 py-2 text-xs font-bold dark:border-white/10"
              >
                {t('practice.abandon')}
              </button>
            </div>
          </div>
        </section>
      ) : null}

      {banksQuery.isLoading ? (
        <PanelSkeleton className="min-h-[320px]" />
      ) : bank ? (
        <section className="rounded-2xl sm:rounded-3xl liquid-glass-panel p-4 sm:p-6 md:p-8 edge-light">
          <div className="flex items-center justify-between gap-4">
            <div>
              <div className="text-[10px] uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">
                {t('practice.bankLabel')}
              </div>
              <h2 className="type-section-title mt-2 text-slate-900 dark:text-white">{bank.name}</h2>
              <p className="mt-2 text-sm leading-6 text-slate-500 dark:text-white/45">{bank.description}</p>
            </div>
            <GraduationCap size={28} className="shrink-0 text-primary" />
          </div>

          <div className="mt-6 grid min-w-0 gap-4 md:grid-cols-2 xl:grid-cols-3">
            <button
              type="button"
              onClick={() => startMutation.mutate({ bankCode: bank.bankCode })}
              disabled={startMutation.isPending || !!resumeCandidate}
              className="group rounded-[1.6rem] border border-primary/25 bg-primary/[0.05] p-5 text-left transition hover:bg-primary/10 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <div className="flex items-center justify-between">
                <span className="font-black text-primary">{t('practice.allSections')}</span>
                <ChevronRight size={16} className="text-primary" />
              </div>
              <p className="mt-2 text-xs leading-5 text-slate-500 dark:text-white/45">{t('practice.allSectionsDescription')}</p>
              <div className="mt-4 text-xs font-bold text-slate-700 dark:text-white/70">
                {t('practice.sectionQuestionCount', { count: bank.totalQuestionCount })}
              </div>
            </button>
            {bank.sections.map((section) => (
              <button
                key={section.sectionCode}
                type="button"
                onClick={() => startMutation.mutate({ bankCode: bank.bankCode, sectionCode: section.sectionCode })}
                disabled={startMutation.isPending || !!resumeCandidate}
                className="group rounded-[1.6rem] border border-slate-200/80 bg-white/60 p-5 text-left transition hover:border-primary/30 hover:bg-white dark:border-white/10 dark:bg-white/[0.03] disabled:cursor-not-allowed disabled:opacity-50"
              >
                <div className="flex items-center justify-between gap-2">
                  <span className="font-black text-slate-900 dark:text-white">{section.title}</span>
                  <ChevronRight size={16} className="shrink-0 text-slate-300 transition group-hover:translate-x-0.5 group-hover:text-primary dark:text-white/20" />
                </div>
                <p className="mt-2 line-clamp-2 text-xs leading-5 text-slate-500 dark:text-white/45">{section.description}</p>
                <div className="mt-4 flex flex-wrap items-center gap-2 text-xs">
                  <span className="font-bold text-slate-700 dark:text-white/70">
                    {t('practice.sectionQuestionCount', { count: section.questionCount })}
                  </span>
                  <span className="rounded-full border border-slate-200 px-2 py-0.5 text-[10px] text-slate-400 dark:border-white/10 dark:text-white/30">
                    {section.constructCodes.map((code) => code.replace('FF4_', '')).join(' / ')}
                  </span>
                </div>
              </button>
            ))}
          </div>
        </section>
      ) : (
        <div className="rounded-2xl border border-rose-500/20 bg-rose-500/5 p-4 text-rose-500 sm:rounded-[2rem] sm:p-6" role="alert">
          {banksQuery.error ? getApiErrorMessage(banksQuery.error) : '题库暂未初始化。'}
        </div>
      )}

      <section className="rounded-2xl liquid-glass-panel p-4 sm:rounded-3xl sm:p-6 md:p-8">
        <div className="mb-4 flex items-center justify-between gap-3">
          <div className="text-[10px] uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">
            {t('practice.recentTitle')}
          </div>
          <button type="button" onClick={() => setPhase('history')} className="text-xs font-bold text-primary">
            {t('practice.historyLink')}
          </button>
        </div>
        {recent.length ? (
          <div className="grid min-w-0 gap-3 md:grid-cols-3">
            {recent.map((record) => (
              <button
                key={record.sessionId}
                type="button"
                onClick={() => { setSessionId(record.sessionId); setSectionCode(record.sectionCode); setPhase('result'); }}
                className="rounded-[1.4rem] border border-slate-200/70 bg-white/60 p-4 text-left dark:border-white/10 dark:bg-white/5"
              >
                <div className="flex items-center justify-between gap-2 text-sm">
                  <span className="font-black text-slate-900 dark:text-white">
                    {record.sectionCode ? record.sectionCode.replace('FF4_', '') : t('practice.allSections')}
                  </span>
                  <span className="font-bold text-primary">{formatPercentValue(record.percentage)}</span>
                </div>
                <div className="mt-2 text-xs text-slate-500 dark:text-white/45">
                  {t('practice.answered')} {record.answeredCount} / {record.totalCount} · {formatDateTime(record.startedAt)}
                </div>
              </button>
            ))}
          </div>
        ) : (
          <div className="py-6 text-center text-sm text-slate-500 dark:text-white/45">{t('practice.recentEmpty')}</div>
        )}
      </section>

      <AbandonDialog
        open={abandonConfirmOpen}
        pending={abandonMutation.isPending}
        onCancel={() => setAbandonConfirmOpen(false)}
        onConfirm={() => {
          if (sessionId != null) {
            void abandonMutation.mutate(sessionId);
          } else if (resumeCandidate) {
            void abandonMutation.mutate(resumeCandidate.sessionId);
          }
        }}
      />
    </div>
  );
};

const AbandonDialog: React.FC<{
  open: boolean;
  pending: boolean;
  onCancel: () => void;
  onConfirm: () => void;
}> = ({ open, pending, onCancel, onConfirm }) => {
  const { t } = useTranslation();
  return (
    <ConfirmationDialog
      open={open}
      title={t('practice.abandonTitle')}
      description={t('practice.abandonDescription')}
      safety={t('practice.abandonDescription')}
      nextStep=""
      confirmLabel={t('practice.abandonConfirm')}
      cancelLabel={t('common.cancel')}
      pending={pending}
      onCancel={onCancel}
      onConfirm={onConfirm}
    />
  );
};

const PracticeSessionView: React.FC<{
  sessionId: number;
  onAbandon: () => void;
  onComplete: () => void;
}> = ({ sessionId, onAbandon, onComplete }) => {
  const { t } = useTranslation();
  const sessionQuery = useQuery({
    queryKey: ['practice-session', sessionId],
    queryFn: ({ signal }) => practiceService.getSession(sessionId, { signal }),
    retry: false,
  });
  const [answers, setAnswers] = React.useState<Record<number, string[]>>({});
  const [hints, setHints] = React.useState<Record<number, string | null>>({});
  const [spellingBusy, setSpellingBusy] = React.useState<Record<number, boolean>>({});
  const [spellingMessages, setSpellingMessages] = React.useState<Record<number, string | null>>({});
  const [confirmOpen, setConfirmOpen] = React.useState(false);
  const [errorMessage, setErrorMessage] = React.useState<string | null>(null);
  const confirmDialogRef = React.useRef<HTMLDivElement | null>(null);
  const confirmButtonRef = React.useRef<HTMLButtonElement | null>(null);
  const draftTimerRef = React.useRef<number | null>(null);
  const answersRef = React.useRef<Record<number, string[]>>({});
  const hydratedRef = React.useRef(false);

  useBodyScrollLock(confirmOpen);
  useDialogAccessibility({
    open: confirmOpen,
    containerRef: confirmDialogRef,
    initialFocusRef: confirmButtonRef,
    onClose: () => setConfirmOpen(false),
  });

  React.useEffect(() => {
    if (!sessionQuery.data || hydratedRef.current) {
      return;
    }
    const restored: Record<number, string[]> = {};
    const restoredHints: Record<number, string | null> = {};
    sessionQuery.data.questions.forEach((question) => {
      restored[question.questionOrder] = question.response?.length ? [...question.response] : [];
      if (question.spellingHintShown && question.spellingHintFirstLetter) {
        restoredHints[question.questionOrder] = question.spellingHintFirstLetter;
      }
    });
    answersRef.current = restored;
    setAnswers(restored);
    setHints((current) => ({ ...current, ...restoredHints }));
    hydratedRef.current = true;
  }, [sessionQuery.data]);

  const updateAnswer = (questionOrder: number, response: string[]) => {
    setAnswers((current) => ({ ...current, [questionOrder]: response }));
  };

  React.useEffect(() => {
    answersRef.current = answers;
  }, [answers]);

  const flushDraft = React.useCallback(async () => {
    const items = Object.entries(answersRef.current)
      .map(([order, response]) => ({ questionOrder: Number(order), response }))
      .filter((item) => item.response.length > 0);
    if (!items.length) {
      return;
    }
    try {
      await practiceService.saveDraft(sessionId, { answers: items });
    } catch {
      // Silent draft save failure; the final submit carries the full answers.
    }
  }, [sessionId]);

  React.useEffect(() => {
    if (!hydratedRef.current) {
      return;
    }
    if (draftTimerRef.current != null) {
      window.clearTimeout(draftTimerRef.current);
    }
    draftTimerRef.current = window.setTimeout(() => {
      void flushDraft();
    }, SAVE_DRAFT_DELAY_MS);
    return () => {
      if (draftTimerRef.current != null) {
        window.clearTimeout(draftTimerRef.current);
      }
    };
  }, [answers, flushDraft]);

  React.useEffect(() => {
    const handleBeforeUnload = () => {
      void flushDraft();
    };
    window.addEventListener('beforeunload', handleBeforeUnload);
    return () => {
      window.removeEventListener('beforeunload', handleBeforeUnload);
      if (draftTimerRef.current != null) {
        window.clearTimeout(draftTimerRef.current);
      }
      void flushDraft();
    };
  }, [flushDraft]);

  const completeMutation = useMutation({
    mutationFn: () =>
      practiceService.complete(sessionId, {
        answers: Object.entries(answers).map(([order, response]) => ({ questionOrder: Number(order), response })),
      }),
    onSuccess: () => {
      setConfirmOpen(false);
      onComplete();
    },
    onError: (error) => {
      setErrorMessage(getApiErrorMessage(error));
      setConfirmOpen(false);
    },
  });

  const checkSpelling = async (question: PracticeQuestionVO) => {
    const candidate = (answers[question.questionOrder] || [])[0] || '';
    if (!candidate.trim() || spellingBusy[question.questionOrder]) {
      return;
    }
    setSpellingBusy((current) => ({ ...current, [question.questionOrder]: true }));
    setSpellingMessages((current) => ({ ...current, [question.questionOrder]: null }));
    try {
      const outcome = await practiceService.checkSpelling(sessionId, { questionOrder: question.questionOrder, candidate });
      if (outcome.correct) {
        setSpellingMessages((current) => ({ ...current, [question.questionOrder]: t('practice.spellingCorrect') }));
      } else {
        setHints((current) => ({ ...current, [question.questionOrder]: outcome.hintFirstLetter }));
        setSpellingMessages((current) => ({
          ...current,
          [question.questionOrder]: t('practice.spellingWrong'),
        }));
      }
    } catch (error) {
      setSpellingMessages((current) => ({ ...current, [question.questionOrder]: getApiErrorMessage(error) }));
    } finally {
      setSpellingBusy((current) => ({ ...current, [question.questionOrder]: false }));
    }
  };

  const questions = sessionQuery.data?.questions || [];
  const answeredCount = questions.filter((question) => (answers[question.questionOrder] || []).some((value) => value.trim())).length;
  const unansweredCount = questions.length - answeredCount;

  if (sessionQuery.isLoading) {
    return (
      <div className="mx-auto max-w-5xl">
        <PanelSkeleton className="min-h-[420px]" />
      </div>
    );
  }
  if (sessionQuery.error || !sessionQuery.data) {
    return (
      <div className="mx-auto max-w-5xl page-stack">
        <PageHeader title={t('practice.runningTitle')} subtitle={t('practice.runningSubtitle')} />
        <div className="rounded-2xl border border-rose-500/20 bg-rose-500/5 p-4 text-rose-500 sm:rounded-[2rem] sm:p-6" role="alert">
          {getApiErrorMessage(sessionQuery.error)}
        </div>
      </div>
    );
  }

  return (
    <div className="page-stack">
      <PageHeader
        title={t('practice.runningTitle')}
        subtitle={t('practice.runningSubtitle')}
        actions={
          <div className="flex flex-wrap items-center gap-3">
            <div className="rounded-full border border-slate-200 px-4 py-2 text-xs font-bold text-slate-500 dark:border-white/10 dark:text-white/60">
              {t('practice.sectionTag')}：{sessionQuery.data.sectionCode ? sessionQuery.data.sectionCode.replace('FF4_', '') : t('practice.allSections')}
            </div>
            <button
              type="button"
              onClick={onAbandon}
              className="rounded-full border border-slate-200 px-4 py-2 text-xs font-bold dark:border-white/10"
            >
              {t('practice.abandon')}
            </button>
          </div>
        }
      />

      {errorMessage ? (
        <div className="rounded-2xl border border-rose-500/20 bg-rose-500/5 p-4 text-sm text-rose-500 sm:rounded-[2rem] sm:p-6" role="alert">
          {errorMessage}
        </div>
      ) : null}
      {unansweredCount > 0 ? (
        <div className="rounded-2xl border border-amber-500/20 bg-amber-500/5 px-4 py-3 text-sm text-amber-700 sm:rounded-[2rem] sm:px-6 dark:text-amber-300">
          {t('practice.unansweredNotice', { count: unansweredCount })}
        </div>
      ) : null}

      <div className="flex items-center justify-between gap-4">
        <div className="text-[10px] uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">
          {t('practice.answered')} {answeredCount} / {questions.length}
        </div>
        <div className="h-2 flex-1 overflow-hidden rounded-full bg-slate-200/70 dark:bg-white/10">
          <div className="h-full rounded-full bg-gradient-to-r from-emerald-500 to-sky-500 transition-all" style={{ width: `${(answeredCount / Math.max(1, questions.length)) * 100}%` }} />
        </div>
      </div>

      <div className="space-y-6">
        {questions.map((question) => {
          const response = answers[question.questionOrder] || [];
          const isSpelling = question.questionType === 'SPELLING';
          return (
            <section key={question.questionOrder} className="rounded-2xl sm:rounded-3xl liquid-glass-panel p-4 sm:p-6 md:p-8 edge-light">
              <div className="flex flex-wrap items-center gap-2">
                <span className="rounded-full bg-primary/10 px-3 py-1 text-[10px] font-black uppercase tracking-[0.16em] text-primary">
                  {t('practice.questionLabel', { current: question.questionOrder, total: questions.length })}
                </span>
                <span className="rounded-full border border-slate-200 px-3 py-1 text-[10px] font-bold uppercase tracking-wider text-slate-400 dark:border-white/10 dark:text-white/30">
                  {QUESTION_TYPE_LABELS[question.questionType] || question.questionType}
                </span>
                {question.sectionCode ? (
                  <span className="rounded-full border border-slate-200 px-3 py-1 text-[10px] font-bold text-slate-400 dark:border-white/10 dark:text-white/30">
                    {question.sectionCode.replace('FF4_', '')}
                  </span>
                ) : null}
              </div>

              <h3 id={`practice-question-${question.questionOrder}`} className="mt-4 text-lg font-black leading-7 text-slate-900 dark:text-white">{question.stemText}</h3>
              {question.promptText ? <p className="mt-2 text-sm leading-6 text-slate-500 dark:text-white/45">{question.promptText}</p> : null}

              {isSpelling ? (
                <div className="mt-6">
                  <div className="flex min-w-0 flex-wrap items-center gap-3">
                    <input
                      type="text"
                      value={response[0] || ''}
                      onChange={(event) => updateAnswer(question.questionOrder, event.target.value ? [event.target.value] : [])}
                      placeholder={t('practice.spellingPlaceholder')}
                      aria-label={t('practice.spellingPlaceholder')}
                      autoComplete="off"
                      autoCorrect="off"
                      spellCheck={false}
                      className="min-w-0 flex-1 rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm font-bold text-slate-900 outline-none focus:border-primary/50 dark:border-white/10 dark:bg-white/5 dark:text-white"
                    />
                    <button
                      type="button"
                      disabled={spellingBusy[question.questionOrder] || !(response[0] || '').trim()}
                      onClick={() => void checkSpelling(question)}
                      className="rounded-full bg-slate-900 px-5 py-3 text-sm font-bold text-white disabled:opacity-50 dark:bg-white dark:text-slate-900"
                    >
                      {spellingBusy[question.questionOrder] ? t('practice.spellingChecking') : t('practice.spellingCheck')}
                    </button>
                  </div>
                  {hints[question.questionOrder] ? (
                    <p className="mt-3 text-sm text-slate-600 dark:text-white/60" role="status">
                      {t('practice.spellingHint', { letter: hints[question.questionOrder] })}
                    </p>
                  ) : (
                    <p className="mt-3 text-xs text-slate-400 dark:text-white/30">{t('practice.spellingHintNote')}</p>
                  )}
                  {spellingMessages[question.questionOrder] ? (
                    <p className="mt-2 text-sm text-sky-700 dark:text-sky-300" role="alert">{spellingMessages[question.questionOrder]}</p>
                  ) : null}
                </div>
              ) : (
                <div
                  className="mt-6 grid gap-3"
                  role="radiogroup"
                  aria-labelledby={`practice-question-${question.questionOrder}`}
                >
                  {question.options.map((option, index) => {
                    const selected = response.includes(option.key);
                    return (
                      <button
                        key={option.key}
                        type="button"
                        role="radio"
                        aria-checked={selected}
                        onClick={() => updateAnswer(question.questionOrder, selected ? [] : [option.key])}
                        className={`flex min-w-0 items-center gap-3 rounded-[1.2rem] border px-4 py-3 text-left text-sm transition ${
                          selected
                            ? 'border-primary/50 bg-primary/10 font-bold text-slate-900 dark:text-white'
                            : 'border-slate-200/80 bg-white/60 text-slate-600 hover:border-primary/30 dark:border-white/10 dark:bg-white/5 dark:text-white/60'
                        }`}
                      >
                        <span className={`flex h-6 w-6 shrink-0 items-center justify-center rounded-full border text-[11px] font-black ${
                          selected ? 'border-primary bg-primary text-white' : 'border-slate-300 text-slate-400 dark:border-white/20'
                        }`}>
                          {String.fromCharCode(65 + index)}
                        </span>
                        <span className="min-w-0 flex-1 break-words">{option.label}</span>
                        {selected ? <Check size={16} className="shrink-0 text-primary" /> : null}
                      </button>
                    );
                  })}
                </div>
              )}
            </section>
          );
        })}
      </div>

      <button
        type="button"
        disabled={completeMutation.isPending}
        onClick={() => setConfirmOpen(true)}
        className="btn-liquid w-full rounded-full px-6 py-4 text-sm font-bold text-white disabled:opacity-60"
      >
        {completeMutation.isPending ? t('practice.submitting') : t('practice.submitAndGrade')}
      </button>

      <ConfirmationDialog
        open={confirmOpen}
        title={t('practice.submitConfirmTitle')}
        description={t('practice.submitConfirmDescription')}
        safety={t('practice.submitConfirmDescription')}
        nextStep=""
        confirmLabel={t('practice.submitAndGrade')}
        cancelLabel={t('common.cancel')}
        pending={completeMutation.isPending}
        onCancel={() => setConfirmOpen(false)}
        onConfirm={() => completeMutation.mutate()}
      />
    </div>
  );
};

const PracticeResultView: React.FC<{
  sessionId: number;
  sectionCode: string | null;
  starting: boolean;
  onRetake: () => void;
  onRetakeWrongWords: (targetWords: string[]) => void;
  onBackHome: () => void;
}> = ({ sessionId, sectionCode, starting, onRetake, onRetakeWrongWords, onBackHome }) => {
  const { t } = useTranslation();
  const resultQuery = useQuery({
    queryKey: ['practice-result', sessionId],
    queryFn: ({ signal }) => practiceService.getResult(sessionId, { signal }),
    retry: false,
  });
  const [tutoring, setTutoring] = React.useState<AiGuidanceResponseVO | null>(null);
  const [tutoringBusy, setTutoringBusy] = React.useState(false);
  const [tutorJobs, setTutorJobs] = React.useState<Record<number, PracticeQuestionTutorVO | 'loading' | 'error'>>({});
  const generationRef = React.useRef(false);

  React.useEffect(() => {
    const result = resultQuery.data;
    if (!result) {
      return;
    }
    const abortController = new AbortController();
    if (result.tutoringJson) {
      try {
        setTutoring((current) => {
          if (current) {
            return current;
          }
          return JSON.parse(result.tutoringJson as string) as AiGuidanceResponseVO;
        });
        setTutoringBusy(false);
        return () => abortController.abort();
      } catch {
        // fall through to regeneration
      }
    }
    if (generationRef.current) {
      return () => abortController.abort();
    }
    generationRef.current = true;
    setTutoringBusy(true);
    aiService.practiceTutoringAsync(result.sessionId, { signal: abortController.signal })
      .then((response) => {
        if (abortController.signal.aborted) {
          return;
        }
        setTutoring(response);
        setTutoringBusy(false);
        void resultQuery.refetch();
      })
      .catch(() => {
        if (abortController.signal.aborted) {
          return;
        }
        generationRef.current = false;
        setTutoringBusy(false);
      });
    return () => abortController.abort();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [resultQuery.data]);

  const requestQuestionTutor = async (questionOrder: number) => {
    setTutorJobs((current) => ({ ...current, [questionOrder]: 'loading' }));
    try {
      const tutor = await aiService.explainPracticeQuestion({ practiceSessionId: sessionId, questionOrder });
      setTutorJobs((current) => ({ ...current, [questionOrder]: tutor }));
    } catch {
      setTutorJobs((current) => ({ ...current, [questionOrder]: 'error' }));
    }
  };

  const result = resultQuery.data;
  if (resultQuery.isLoading) {
    return (
      <div className="mx-auto max-w-5xl">
        <PanelSkeleton className="min-h-[420px]" />
      </div>
    );
  }
  if (resultQuery.error || !result) {
    return (
      <div className="mx-auto max-w-5xl page-stack">
        <PageHeader title={t('practice.resultTitle')} subtitle={t('practice.resultSubtitle')} />
        <div className="rounded-2xl border border-rose-500/20 bg-rose-500/5 p-4 text-rose-500 sm:rounded-[2rem] sm:p-6" role="alert">
          {getApiErrorMessage(resultQuery.error)}
        </div>
      </div>
    );
  }

  const wrongTargetWords = Array.from(new Set(
    result.questions
      .filter((question) => question.correct === false && question.targetWord)
      .map((question) => question.targetWord as string)
  )).slice(0, 30);

  return (
    <div className="page-stack">
      <PageHeader
        title={t('practice.resultTitle')}
        subtitle={t('practice.resultSubtitle')}
        actions={
          <div className="flex flex-wrap items-center gap-3">
            {wrongTargetWords.length > 0 ? (
              <button
                type="button"
                onClick={() => onRetakeWrongWords(wrongTargetWords)}
                disabled={starting}
                className="btn-liquid rounded-full px-5 py-2.5 text-sm font-bold text-white disabled:opacity-60"
              >
                {t('practice.retakeWrongWords')}
              </button>
            ) : null}
            <button
              type="button"
              onClick={onRetake}
              disabled={starting}
              className="rounded-full border border-slate-200 px-5 py-2.5 text-sm font-bold disabled:opacity-60 dark:border-white/10"
            >
              {t('practice.retake')}
            </button>
            <button
              type="button"
              onClick={onBackHome}
              className="inline-flex items-center gap-2 rounded-full border border-slate-200 px-5 py-2.5 text-sm font-bold dark:border-white/10"
            >
              <ArrowLeft size={15} /> {t('practice.backHome')}
            </button>
          </div>
        }
      />

      <section className="rounded-2xl sm:rounded-3xl liquid-glass-panel p-4 sm:p-6 md:p-8 edge-light">
        <div className="flex flex-wrap items-center justify-between gap-6">
          <div className="flex items-center gap-6">
            <div>
              <div className="text-[10px] uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">{t('practice.accuracy')}</div>
              <div className="type-numeric mt-2 text-4xl font-black text-slate-900 dark:text-white">
                {formatPercentValue(result.percentage)}
              </div>
            </div>
            <div className="h-12 w-px bg-slate-200 dark:bg-white/10" />
            <div>
              <div className="text-[10px] uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">{t('practice.answered')}</div>
              <div className="type-numeric mt-2 text-xl font-black text-slate-900 dark:text-white">
                {result.answeredCount}<span className="text-sm text-slate-400"> / {result.totalCount}</span>
              </div>
            </div>
            <div>
              <div className="text-[10px] uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">{t('practice.correct')}</div>
              <div className="type-numeric mt-2 text-xl font-black text-emerald-600 dark:text-emerald-400">{result.correctCount}</div>
            </div>
          </div>
          {sectionCode ? (
            <span className="rounded-full border border-slate-200 px-4 py-2 text-xs font-bold text-slate-500 dark:border-white/10 dark:text-white/60">
              {sectionCode.replace('FF4_', '')}
            </span>
          ) : null}
        </div>
      </section>

      <section className="rounded-2xl liquid-glass-panel p-4 sm:rounded-3xl sm:p-6 md:p-8">
        <div className="mb-4 text-[10px] uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">
          {t('practice.sectionPerformance')}
        </div>
        <div className="grid min-w-0 gap-3 sm:grid-cols-2 xl:grid-cols-4">
          {result.sectionMetrics.filter((metric) => metric.totalCount > 0).map((metric) => (
            <div key={metric.sectionCode} className="rounded-[1.4rem] border border-slate-200/70 bg-white/60 p-4 dark:border-white/10 dark:bg-white/5">
              <div className="text-xs font-black text-slate-900 dark:text-white">{metric.title}</div>
              <div className="mt-2 text-2xl font-black text-slate-900 dark:text-white">{formatPercentValue(metric.percentage)}</div>
              <div className="mt-1 text-xs text-slate-400 dark:text-white/30">
                {metric.correctCount} / {metric.totalCount}
              </div>
            </div>
          ))}
        </div>
      </section>

      <section className="rounded-2xl liquid-glass-panel p-4 sm:rounded-3xl sm:p-6 md:p-8">
        <div className="mb-4 flex items-center gap-2 text-[10px] uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">
          <Sparkles size={14} className="text-primary" />
          {t('practice.tutoringTitle')}
        </div>
        <PracticeTutoringPanel
          tutoring={tutoring}
          busy={tutoringBusy}
          onRetry={() => {
            generationRef.current = false;
            setTutoringBusy(true);
                    aiService.practiceTutoringAsync(sessionId)
              .then((response) => {
                setTutoring(response);
                setTutoringBusy(false);
                void resultQuery.refetch();
              })
              .catch(() => {
                generationRef.current = false;
                setTutoringBusy(false);
                      });
          }}
        />
      </section>

      <section className="rounded-2xl liquid-glass-panel p-4 sm:rounded-3xl sm:p-6 md:p-8">
        <div className="mb-4 text-[10px] uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">
          {t('practice.questionReview')}
        </div>
        <div className="space-y-4">
          {result.questions.map((question) => (
            <PracticeResultQuestionCard
              key={question.questionOrder}
              question={question}
              tutor={tutorJobs[question.questionOrder] && tutorJobs[question.questionOrder] !== 'loading' && tutorJobs[question.questionOrder] !== 'error'
                ? tutorJobs[question.questionOrder] as PracticeQuestionTutorVO
                : null}
              tutorLoading={tutorJobs[question.questionOrder] === 'loading'}
              onRequestTutor={() => void requestQuestionTutor(question.questionOrder)}
            />
          ))}
        </div>
      </section>
    </div>
  );
};

const SPELLING_PATTERN_LABELS: Record<string, string> = {
  ACCENT_ORTHOGRAPHY: '重音/拼写差异',
  REPLACED_LETTER: '字母替换',
  MISSING_LETTER: '缺字母',
  EXTRA_LETTER: '多字母',
  CLOSE: '形近拼写',
  DISTANT: '拼写差异较大',
};

const PracticeResultQuestionCard: React.FC<{
  question: PracticeResultQuestionVO;
  tutor: PracticeQuestionTutorVO | null;
  tutorLoading: boolean;
  onRequestTutor: () => void;
}> = ({ question, tutor, tutorLoading, onRequestTutor }) => {
  const { t } = useTranslation();
  const [showExplanation, setShowExplanation] = React.useState(false);
  const answered = question.correct != null;
  const isCorrect = question.correct === true;
  const outcomeLabel = !answered ? t('practice.skippedLabel') : isCorrect ? t('practice.correctLabel') : t('practice.wrongLabel');

  return (
    <div className={`rounded-[1.6rem] border p-4 ${
      isCorrect
        ? 'border-emerald-500/20 bg-emerald-500/[0.04]'
        : answered
          ? 'border-rose-500/20 bg-rose-500/[0.04]'
          : 'border-slate-200/70 bg-white/60 dark:border-white/10 dark:bg-white/5'
    }`}>
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-2">
            <span className="text-[10px] font-black uppercase tracking-[0.16em] text-slate-400 dark:text-white/30">
              Q{question.questionOrder}
            </span>
            <span className="rounded-full border border-slate-200 px-2 py-0.5 text-[10px] text-slate-400 dark:border-white/10 dark:text-white/30">
              {question.sectionCode?.replace('FF4_', '') || ''}
            </span>
            {question.spellingErrorPattern ? (
              <span className="rounded-full border border-amber-500/20 bg-amber-500/5 px-2 py-0.5 text-[10px] font-bold text-amber-600 dark:text-amber-300">
                {SPELLING_PATTERN_LABELS[question.spellingErrorPattern] || question.spellingErrorPattern}
              </span>
            ) : null}
          </div>
          <div className="mt-2 font-black leading-7 text-slate-900 dark:text-white">{question.stemText}</div>
          {question.targetWord ? <div className="mt-1 text-sm text-slate-400 dark:text-white/30">{question.targetWord}</div> : null}
        </div>
        <div className="flex items-center gap-2">
          {isCorrect ? <CheckCircle2 size={18} className="text-emerald-500" /> : answered ? <XCircle size={18} className="text-rose-500" /> : null}
          <span className={`text-sm font-black ${isCorrect ? 'text-emerald-600 dark:text-emerald-400' : answered ? 'text-rose-500' : 'text-slate-400'}`}>
            {outcomeLabel}
          </span>
        </div>
      </div>

      <div className="mt-3 flex flex-wrap gap-x-6 gap-y-1 text-sm text-slate-600 dark:text-white/60">
        <div>
          <span className="text-slate-400 dark:text-white/30">{t('practice.yourAnswer')}：</span>
          <span className="font-bold">{question.response.length ? question.response.join('、') : '—'}</span>
        </div>
        <div>
          <span className="text-slate-400 dark:text-white/30">{t('practice.correctAnswer')}：</span>
          <span className="font-bold">{question.correctAnswer.join('、')}</span>
        </div>
      </div>

      {question.explanation ? (
        <>
          <button
            type="button"
            aria-expanded={showExplanation}
            onClick={() => setShowExplanation((visible) => !visible)}
            className="mt-3 rounded-full border border-slate-200 px-4 py-2 text-xs font-bold text-primary dark:border-white/10"
          >
            {showExplanation ? t('training.hideExplanation') : t('practice.explanation')}
          </button>
          {showExplanation ? (
            <div className="mt-3 whitespace-pre-line rounded-[1.2rem] border border-dashed border-slate-200/80 px-4 py-3 text-sm leading-7 text-slate-600 dark:border-white/10 dark:text-white/60">
              {question.explanation}
              {Object.keys(question.optionExplanations || {}).length ? (
                <ul className="mt-2 space-y-1 text-xs text-slate-500 dark:text-white/45">
                  {Object.entries(question.optionExplanations).map(([key, text]) => (
                    <li key={key}>{key}：{text}</li>
                  ))}
                </ul>
              ) : null}
            </div>
          ) : null}
        </>
      ) : null}

      <PracticeQuestionTutor
        tutor={tutor}
        loading={tutorLoading}
        onRequest={onRequestTutor}
      />
    </div>
  );
};

const PracticeHistoryView: React.FC<{ onBack: () => void }> = ({ onBack }) => {
  const { t } = useTranslation();
  const [pageNo, setPageNo] = React.useState(1);
  const historyQuery = useQuery({
    queryKey: ['practice-history', pageNo, 10],
    queryFn: ({ signal }) => practiceService.listHistory({ pageNo, pageSize: 10 }, { signal }),
  });

  return (
    <div className="page-stack">
      <PageHeader
        title={t('practice.historyTitle')}
        subtitle={t('practice.homeSubtitle')}
        actions={
          <button
            type="button"
            onClick={onBack}
            className="inline-flex items-center gap-2 rounded-full border border-slate-200 px-5 py-2.5 text-sm font-bold dark:border-white/10"
          >
            <ArrowLeft size={15} /> {t('practice.backHome')}
          </button>
        }
      />
      {historyQuery.isLoading ? (
        <PanelSkeleton className="min-h-[240px]" />
      ) : !historyQuery.data?.records.length ? (
        <div className="rounded-[2rem] border border-slate-200/70 p-8 text-center text-sm text-slate-500 dark:border-white/10 dark:text-white/45">
          {t('practice.historyEmpty')}
        </div>
      ) : (
        <>
          <div className="grid min-w-0 gap-3">
            {historyQuery.data.records.map((record) => (
              <div key={record.sessionId} className="flex flex-wrap items-center justify-between gap-3 rounded-[1.6rem] border border-slate-200/70 bg-white/60 px-5 py-4 dark:border-white/10 dark:bg-white/5">
                <div className="min-w-0">
                  <div className="font-black text-slate-900 dark:text-white">
                    {record.sectionCode ? record.sectionCode.replace('FF4_', '') : t('practice.allSections')}
                  </div>
                  <div className="mt-1 text-xs text-slate-400 dark:text-white/30">
                    {formatDateTime(record.startedAt)}
                    {record.status === 'IN_PROGRESS' ? ` · ${t('practice.resumeInProgress')}` : ''}
                  </div>
                </div>
                <div className="flex items-center gap-4">
                  <div className="text-right">
                    <div className="text-xl font-black text-slate-900 dark:text-white">{formatPercentValue(record.percentage)}</div>
                    <div className="text-xs text-slate-400 dark:text-white/30">{record.correctCount} / {record.totalCount}</div>
                  </div>
                  <History size={16} className="text-slate-300 dark:text-white/20" />
                </div>
              </div>
            ))}
          </div>
          <div className="flex items-center justify-center gap-3 pt-2">
            <button
              type="button"
              disabled={pageNo <= 1}
              onClick={() => setPageNo((value) => value - 1)}
              className="rounded-full border border-slate-200 px-4 py-2 text-xs font-bold disabled:opacity-40 dark:border-white/10"
            >
              ←
            </button>
            <span className="text-xs text-slate-400">{pageNo}</span>
            <button
              type="button"
              disabled={pageNo * 10 >= historyQuery.data.total}
              onClick={() => setPageNo((value) => value + 1)}
              className="rounded-full border border-slate-200 px-4 py-2 text-xs font-bold disabled:opacity-40 dark:border-white/10"
            >
              →
            </button>
          </div>
        </>
      )}
    </div>
  );
};

export default PracticePage;
