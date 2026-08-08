import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { CheckCircle2, FileText, XCircle } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Link, useParams } from 'react-router-dom';
import { PageHeader, SectionEyebrow, StatusBadge } from '@/components/common';
import { getApiErrorMessage } from '@/lib/api';
import { assessmentQuestionTypeLabel, formatDateTime } from '@/lib/format';
import { assessmentService } from '@/lib/services';

const TeacherAssessmentAttemptResultPage: React.FC = () => {
  const { t } = useTranslation();
  const params = useParams<{ attemptId: string }>();
  const attemptId = Number(params.attemptId);
  const resultQuery = useQuery({
    queryKey: ['teacher-assessment-attempt-result', attemptId],
    queryFn: ({ signal }) => assessmentService.getTeacherAttemptResult(attemptId, { signal }),
    enabled: Number.isFinite(attemptId),
    retry: false,
  });

  const result = resultQuery.data;

  return (
    <div className="page-stack pb-16">
      <PageHeader
        eyebrow={t('ui.sections.assessments')}
        title={result?.paperTitle || t('ui.fields.student')}
        subtitle={
          result
            ? `${result.studentName} · ${result.className} · ${t('ui.meta.submittedAt', { time: formatDateTime(result.submittedAt) })}`
            : t('ui.labels.loadingAttemptResult')
        }
        actions={
          <div className="page-actions">
            <Link to="/teacher/assessments" className="rounded-full border border-slate-200 px-4 py-3 text-sm dark:border-white/10">
              {t('ui.actions.backToAssessments')}
            </Link>
            {result && (
              <Link
                to={`/teacher/assessments/publishes/${result.publishId}`}
                className="rounded-full border border-slate-200 px-4 py-3 text-sm dark:border-white/10"
              >
                {t('ui.actions.backToPublishDetail')}
              </Link>
            )}
          </div>
        }
      />

      {resultQuery.error && (
        <div className="min-w-0 rounded-2xl border border-rose-500/20 bg-rose-500/5 p-4 text-rose-500 sm:p-6">
          {getApiErrorMessage(resultQuery.error)}
        </div>
      )}

      {resultQuery.isLoading && (
        <div className="min-w-0 rounded-2xl liquid-glass-panel p-4 text-sm text-slate-500 sm:p-6 md:p-8 dark:text-white/45">
          {t('ui.labels.loadingAttemptResult')}
        </div>
      )}

      {result && (
        <>
          <div className="grid min-w-0 grid-cols-2 gap-3 sm:gap-4 md:grid-cols-3 xl:grid-cols-5">
            <div className="min-w-0 rounded-2xl liquid-glass-panel p-4 sm:p-6">
              <SectionEyebrow>{t('ui.fields.student')}</SectionEyebrow>
              <div className="mt-3 break-words text-xl font-black text-slate-900 sm:text-2xl dark:text-white">{result.studentName}</div>
              <div className="mt-2 text-sm text-slate-500 dark:text-white/45">{t('ui.fields.student')}</div>
            </div>
            <div className="min-w-0 rounded-2xl liquid-glass-panel p-4 sm:p-6">
              <SectionEyebrow>{t('ui.meta.objectiveScore')}</SectionEyebrow>
              <div className="mt-3 text-3xl font-black text-slate-900 sm:text-4xl dark:text-white">{result.totalScore}</div>
              <div className="mt-2 text-sm text-slate-500 dark:text-white/45">{t('ui.meta.objectiveScore')}</div>
            </div>
            <div className="min-w-0 rounded-2xl liquid-glass-panel p-4 sm:p-6">
              <SectionEyebrow>{t('ui.meta.correctRate')}</SectionEyebrow>
              <div className="mt-3 text-3xl font-black text-slate-900 sm:text-4xl dark:text-white">
                {result.questionCount ? Math.round((result.correctCount / result.questionCount) * 100) : 0}%
              </div>
              <div className="mt-2 text-sm text-slate-500 dark:text-white/45">{t('ui.meta.correctRate')}</div>
            </div>
            <div className="min-w-0 rounded-2xl liquid-glass-panel p-4 sm:p-6">
              <SectionEyebrow>{t('ui.meta.answeredQuestionCount')}</SectionEyebrow>
              <div className="mt-3 text-3xl font-black text-slate-900 sm:text-4xl dark:text-white">{result.answeredCount}</div>
              <div className="mt-2 text-sm text-slate-500 dark:text-white/45">{t('ui.meta.answeredQuestionCount')}</div>
            </div>
            <div className="min-w-0 col-span-2 rounded-2xl liquid-glass-panel p-4 sm:p-6 md:col-span-1">
              <SectionEyebrow>{t('ui.meta.submitted')}</SectionEyebrow>
              <div className="mt-3 break-words text-base font-black text-slate-900 sm:text-lg dark:text-white">{formatDateTime(result.submittedAt)}</div>
              <div className="mt-2 text-sm text-slate-500 dark:text-white/45">{t('ui.meta.submitted')}</div>
            </div>
          </div>

          {result.instructionsText && (
            <div className="rounded-[2rem] border border-dashed border-slate-200/80 px-5 py-4 text-sm text-slate-600 dark:border-white/10 dark:text-white/60">
              {result.instructionsText}
            </div>
          )}

          <div className="space-y-5">
            {result.questions.map((question) => (
              <div key={question.answerId} className="min-w-0 space-y-5 rounded-2xl liquid-glass-panel p-4 sm:rounded-[2.4rem] sm:p-6 md:p-8">
                <div className="flex min-w-0 flex-col gap-3 sm:flex-row sm:flex-wrap sm:items-start sm:justify-between sm:gap-4">
                  <div className="min-w-0">
                    <SectionEyebrow>{`${t('ui.meta.progress', { current: question.questionOrder, total: result.questions.length })} · ${assessmentQuestionTypeLabel(question.questionType)}`}</SectionEyebrow>
                    <div className="mt-3 break-words text-xl font-black text-slate-900 dark:text-white">{question.stemText}</div>
                    {question.promptText && <div className="mt-2 break-words text-sm text-slate-500 dark:text-white/45">{question.promptText}</div>}
                  </div>
                  <StatusBadge label={question.correct ? t('ui.meta.correct') : t('ui.meta.incorrect')} tone={question.correct ? 'success' : 'danger'} className="px-4 py-2 text-sm" />
                </div>

                {!!question.options.length && (
                  <div className="grid gap-3">
                    {question.options.map((option) => {
                      const selected = question.responses.includes(option.key);
                      const correct = question.correctAnswers.includes(option.key);
                      return (
                        <div
                          key={option.key}
                          className={`rounded-[1.4rem] border px-4 py-4 text-sm ${
                            correct
                              ? 'border-emerald-500/20 bg-emerald-500/10 text-emerald-700 dark:text-emerald-300'
                              : selected
                                ? 'border-rose-500/20 bg-rose-500/5 text-rose-600 dark:text-rose-300'
                                : 'border-slate-200/70 bg-white/70 text-slate-600 dark:border-white/10 dark:bg-white/5 dark:text-white/60'
                          }`}
                        >
                          <div className="flex items-start gap-3">
                            {correct ? <CheckCircle2 size={16} /> : selected ? <XCircle size={16} /> : <FileText size={16} />}
                            <div>
                              <div className="font-semibold">{option.key}</div>
                              <div className="mt-1">{option.label}</div>
                            </div>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                )}

                {!question.options.length && (
                  <div className="grid min-w-0 grid-cols-1 gap-3 md:grid-cols-2">
                    <div className="min-w-0 rounded-[1.4rem] border border-slate-200/70 bg-white/70 px-4 py-4 text-sm text-slate-600 dark:border-white/10 dark:bg-white/5 dark:text-white/60">
                      <div className="font-semibold text-slate-900 dark:text-white">{t('ui.meta.yourAnswer')}</div>
                      <div className="mt-2 break-words">{question.responses.join(' / ') || t('ui.meta.unanswered')}</div>
                    </div>
                    <div className="min-w-0 rounded-[1.4rem] border border-emerald-500/20 bg-emerald-500/10 px-4 py-4 text-sm text-emerald-700 dark:text-emerald-300">
                      <div className="font-semibold">{t('ui.meta.correctAnswer')}</div>
                      <div className="mt-2 break-words">{question.correctAnswers.join(' / ') || t('ui.meta.notReturned')}</div>
                    </div>
                  </div>
                )}

                <div className="grid min-w-0 grid-cols-1 gap-3 text-sm md:grid-cols-3">
                  <div className="min-w-0 break-words rounded-[1.4rem] border border-slate-200/70 bg-white/70 px-4 py-3 text-slate-600 dark:border-white/10 dark:bg-white/5 dark:text-white/60">
                    {t('ui.meta.objectiveScore')}：{question.scoreAwarded ?? 0} / {question.score}
                  </div>
                  <div className="min-w-0 break-words rounded-[1.4rem] border border-slate-200/70 bg-white/70 px-4 py-3 text-slate-600 dark:border-white/10 dark:bg-white/5 dark:text-white/60">
                    {t('ui.meta.yourAnswer')}：{question.responses.join(' / ') || t('ui.meta.unanswered')}
                  </div>
                  <div className="min-w-0 break-words rounded-[1.4rem] border border-emerald-500/20 bg-emerald-500/10 px-4 py-3 text-emerald-700 dark:text-emerald-300">
                    {t('ui.meta.correctAnswer')}：{question.correctAnswers.join(' / ') || t('ui.meta.notReturned')}
                  </div>
                </div>

                {question.explanationText && (
                  <div className="rounded-[1.4rem] border border-dashed border-slate-200/80 px-4 py-4 text-sm text-slate-600 dark:border-white/10 dark:text-white/60">
                    {question.explanationText}
                  </div>
                )}
              </div>
            ))}
          </div>
        </>
      )}
    </div>
  );
};

export default TeacherAssessmentAttemptResultPage;
