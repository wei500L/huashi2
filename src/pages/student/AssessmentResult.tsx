import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { CheckCircle2, FileText, XCircle } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Link, useParams } from 'react-router-dom';
import { PageHeader, SectionEyebrow, StatusBadge } from '@/components/common';
import { getApiErrorMessage } from '@/lib/api';
import { assessmentQuestionTypeLabel, formatDateTime } from '@/lib/format';
import { assessmentService } from '@/lib/services';

const StudentAssessmentResultPage: React.FC = () => {
  const { t } = useTranslation();
  const params = useParams<{ attemptId: string }>();
  const attemptId = Number(params.attemptId);
  const isValidAttemptId = Number.isSafeInteger(attemptId) && attemptId > 0;
  const resultQuery = useQuery({
    queryKey: ['student-assessment-result', attemptId],
    queryFn: ({ signal }) => assessmentService.getStudentAttemptResult(attemptId, { signal }),
    enabled: isValidAttemptId,
    retry: false,
    refetchInterval: (query) => query.state.data?.releaseStatus === 'PENDING' ? 15000 : false,
  });

  if (!isValidAttemptId) {
    return (
      <div className="rounded-[2.4rem] border border-amber-500/20 bg-amber-500/10 p-8 text-amber-800 dark:text-amber-200">
        <div className="text-2xl font-black">结果链接无效</div>
        <p className="mt-3 text-sm">答卷编号必须是正整数，请返回任务列表重新进入。</p>
        <Link to="/assessments" className="mt-5 inline-flex rounded-full border border-amber-500/30 px-4 py-2 text-sm font-bold">
          返回任务列表
        </Link>
      </div>
    );
  }

  return (
    <div className="space-y-8 pb-20">
      <PageHeader
        eyebrow={t('ui.sections.assessments')}
        title={resultQuery.data?.paperTitle || t('ui.pages.assessmentResult.fallbackTitle')}
        subtitle={resultQuery.data ? `${resultQuery.data.className} · ${t('ui.meta.submittedAt', { time: formatDateTime(resultQuery.data.submittedAt) })}` : t('ui.labels.loadingAssessmentResult')}
        actions={
          <Link to="/assessments" className="rounded-full border border-slate-200 px-4 py-3 text-sm dark:border-white/10">
            {t('ui.pages.assessmentResult.backLinkLabel')}
          </Link>
        }
      />

      {resultQuery.error && (
        <div className="rounded-[2rem] border border-rose-500/20 bg-rose-500/5 p-6 text-rose-500">
          <div>{getApiErrorMessage(resultQuery.error)}</div>
          <button type="button" onClick={() => void resultQuery.refetch()} className="mt-4 rounded-full border border-rose-500/30 px-4 py-2 text-sm font-bold">
            重试加载
          </button>
        </div>
      )}

      {resultQuery.isLoading && (
        <div className="rounded-[2.2rem] liquid-glass-panel p-8 text-sm text-slate-500 dark:text-white/45">
          {t('ui.labels.loadingAssessmentResult')}
        </div>
      )}

      {resultQuery.data?.releaseStatus === 'PENDING' && (
        <section className="rounded-[2.4rem] border border-amber-500/20 bg-amber-500/10 p-8 text-amber-800 dark:text-amber-200">
          <div className="text-2xl font-black">答卷已提交，结果待公布</div>
          <p className="mt-3 text-sm leading-7">
            本次测评将在 {formatDateTime(resultQuery.data.resultAvailableAt)} 公布成绩、正确答案和解析。
          </p>
        </section>
      )}

      {resultQuery.data?.releaseStatus === 'AVAILABLE' && (
        <>
          <div className="grid gap-6 md:grid-cols-4">
            <div className="rounded-[2rem] liquid-glass-panel p-6">
              <SectionEyebrow>{t('ui.meta.objectiveScore')}</SectionEyebrow>
              <div className="mt-3 text-4xl font-black text-slate-900 dark:text-white">{resultQuery.data.totalScore ?? 0}</div>
              <div className="mt-2 text-sm text-slate-500 dark:text-white/45">{t('ui.meta.objectiveScore')}</div>
            </div>
            <div className="rounded-[2rem] liquid-glass-panel p-6">
              <SectionEyebrow>{t('ui.meta.correctRate')}</SectionEyebrow>
              <div className="mt-3 text-4xl font-black text-slate-900 dark:text-white">
                {resultQuery.data.questionCount ? Math.round(((resultQuery.data.correctCount ?? 0) / resultQuery.data.questionCount) * 100) : 0}%
              </div>
              <div className="mt-2 text-sm text-slate-500 dark:text-white/45">{t('ui.meta.correctRate')}</div>
            </div>
            <div className="rounded-[2rem] liquid-glass-panel p-6">
              <SectionEyebrow>{t('ui.meta.answeredQuestionCount')}</SectionEyebrow>
              <div className="mt-3 text-4xl font-black text-slate-900 dark:text-white">{resultQuery.data.answeredCount}</div>
              <div className="mt-2 text-sm text-slate-500 dark:text-white/45">{t('ui.meta.answeredQuestionCount')}</div>
            </div>
            <div className="rounded-[2rem] liquid-glass-panel p-6">
              <SectionEyebrow>{t('ui.meta.startedAnsweringAt')}</SectionEyebrow>
              <div className="mt-3 text-xl font-black text-slate-900 dark:text-white">{formatDateTime(resultQuery.data.startedAt)}</div>
              <div className="mt-2 text-sm text-slate-500 dark:text-white/45">{t('ui.meta.startedAnsweringAt')}</div>
            </div>
          </div>

          {resultQuery.data.instructionsText && (
            <div className="rounded-[2rem] border border-dashed border-slate-200/80 px-5 py-4 text-sm text-slate-600 dark:border-white/10 dark:text-white/60">
              {resultQuery.data.instructionsText}
            </div>
          )}

          <div className="space-y-5">
            {resultQuery.data.questions.map((question) => (
              <div key={question.answerId} className="rounded-[2.4rem] liquid-glass-panel p-6 md:p-8 space-y-5">
                <div className="flex flex-wrap items-start justify-between gap-4">
                  <div>
                    <SectionEyebrow>{`${t('ui.meta.progress', { current: question.questionOrder, total: resultQuery.data.questions.length })} · ${assessmentQuestionTypeLabel(question.questionType)}`}</SectionEyebrow>
                    <div className="mt-3 text-xl font-black text-slate-900 dark:text-white">{question.stemText}</div>
                    {question.promptText && <div className="mt-2 text-sm text-slate-500 dark:text-white/45">{question.promptText}</div>}
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
                  <div className="grid gap-3 md:grid-cols-2">
                    <div className="rounded-[1.4rem] border border-slate-200/70 bg-white/70 px-4 py-4 text-sm text-slate-600 dark:border-white/10 dark:bg-white/5 dark:text-white/60">
                      <div className="font-semibold text-slate-900 dark:text-white">{t('ui.meta.yourAnswer')}</div>
                      <div className="mt-2">{question.responses.join(' / ') || t('ui.meta.unanswered')}</div>
                    </div>
                    <div className="rounded-[1.4rem] border border-emerald-500/20 bg-emerald-500/10 px-4 py-4 text-sm text-emerald-700 dark:text-emerald-300">
                      <div className="font-semibold">{t('ui.meta.correctAnswer')}</div>
                      <div className="mt-2">{question.correctAnswers.join(' / ') || t('ui.meta.notReturned')}</div>
                    </div>
                  </div>
                )}

                <div className="grid gap-3 md:grid-cols-3 text-sm">
                  <div className="rounded-[1.4rem] border border-slate-200/70 bg-white/70 px-4 py-3 text-slate-600 dark:border-white/10 dark:bg-white/5 dark:text-white/60">
                    {t('ui.meta.objectiveScore')}：{question.scoreAwarded ?? 0} / {question.score}
                  </div>
                  <div className="rounded-[1.4rem] border border-slate-200/70 bg-white/70 px-4 py-3 text-slate-600 dark:border-white/10 dark:bg-white/5 dark:text-white/60">
                    {t('ui.meta.yourAnswer')}：{question.responses.join(' / ') || t('ui.meta.unanswered')}
                  </div>
                  <div className="rounded-[1.4rem] border border-emerald-500/20 bg-emerald-500/10 px-4 py-3 text-emerald-700 dark:text-emerald-300">
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

export default StudentAssessmentResultPage;
