import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { AlertTriangle, ArrowRight, BookOpen, CheckCircle2, FileText, Target, XCircle } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Link, useParams } from 'react-router-dom';
import { PageHeader, SectionEyebrow, StatusBadge } from '@/components/common';
import { getApiErrorMessage } from '@/lib/api';
import { assessmentQuestionTypeLabel, formatDateTime } from '@/lib/format';
import { assessmentService } from '@/lib/services';
import type { AppChartOption } from '@/lib/echarts';
import { ChartCard } from '@/components/common/ChartCard';
import type { AssessmentAttemptResultQuestionVO } from '@/lib/contracts';

function buildAssessmentProgressOption(questions: AssessmentAttemptResultQuestionVO[]): AppChartOption {
  let answered = 0;
  let correct = 0;
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: '8%', right: '5%', top: '10%', bottom: '18%', containLabel: true },
    xAxis: {
      type: 'category',
      name: '题号 / Question',
      data: questions.map((question) => String(question.questionOrder)),
      axisLabel: { color: '#94a3b8' },
    },
    yAxis: {
      type: 'value',
      name: '累计正确率 / Accuracy',
      min: 0,
      max: 1,
      axisLabel: { color: '#94a3b8', formatter: (value: number) => `${Math.round(value * 100)}%` },
    },
    series: [
      {
        name: '累计正确率 / Cumulative accuracy',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        data: questions.map((question) => {
          answered += 1;
          if (question.correct) correct += 1;
          return correct / answered;
        }),
      },
    ],
  };
}

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
  const result = resultQuery.data;
  const scorePercent = result && result.scoreVisible && result.questionCount ? Math.round(((result.correctCount ?? 0) / result.questionCount) * 100) : null;
  const incorrectQuestions = result?.answerReviewVisible ? result.questions.filter((question) => question.correct === false) : [];
  const dimensions = result?.scoreVisible ? result.metricSnapshot?.dimensions || [] : [];

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

      {result?.releaseStatus === 'AVAILABLE' && (
        <>
          <section className="grid gap-6 xl:grid-cols-[1.25fr_0.75fr]" aria-labelledby="result-conclusion">
            <div className="rounded-[2.4rem] bg-slate-950 p-8 text-white shadow-xl dark:bg-white/10">
              <SectionEyebrow className="text-white/55">{t('ui.pages.assessmentResult.conclusionEyebrow')}</SectionEyebrow>
              <h2 id="result-conclusion" className="mt-3 max-w-3xl text-3xl font-black leading-tight break-words">
                {result.aiAnalysis?.performanceOverview || (scorePercent === null ? t('ui.pages.assessmentResult.conclusionFallback') : t('ui.pages.assessmentResult.conclusionScore', { percent: scorePercent }))}
              </h2>
              <p className="mt-4 max-w-2xl text-sm leading-7 text-white/65">{t('ui.pages.assessmentResult.conclusionNote')}</p>
              <div className="mt-7 flex flex-wrap gap-3">
                <Link to="/errors" className="btn-liquid inline-flex items-center gap-2 px-5 py-3 text-white"><BookOpen size={16} />{t('ui.pages.assessmentResult.openErrors')}<ArrowRight size={15} /></Link>
                <Link to="/analytics" className="inline-flex items-center gap-2 rounded-full border border-white/20 px-5 py-3 text-sm font-bold text-white/85">{t('ui.pages.assessmentResult.viewTrends')}</Link>
              </div>
            </div>
            <div className="rounded-[2.4rem] liquid-glass-panel p-8">
              <SectionEyebrow>{t('ui.pages.assessmentResult.scoreEyebrow')}</SectionEyebrow>
              {result.scoreVisible ? (
                <>
                  <div className="mt-3 text-6xl font-black text-slate-900 dark:text-white">{result.objectiveScore ?? 0}<span className="text-2xl text-slate-400"> / {result.totalScore ?? 0}</span></div>
                  <div className="mt-3 text-lg font-bold text-primary">{scorePercent ?? 0}% 正确率</div>
                </>
              ) : <div className="mt-5 text-xl font-bold text-slate-500 dark:text-white/60">成绩将在发布后显示</div>}
              <div className="mt-6 grid gap-3 text-sm text-slate-500 dark:text-white/45">
                <div>已作答 {result.answeredCount} / {result.questionCount}</div>
                <div>提交于 {formatDateTime(result.submittedAt)}</div>
              </div>
            </div>
          </section>

          <section className="rounded-[2.4rem] liquid-glass-panel p-8" aria-labelledby="result-evidence">
            <div className="flex flex-wrap items-end justify-between gap-4"><div><SectionEyebrow>{t('ui.pages.assessmentResult.evidenceEyebrow')}</SectionEyebrow><h2 id="result-evidence" className="mt-2 text-2xl font-black">{t('ui.pages.assessmentResult.evidenceTitle')}</h2></div><div className="text-sm text-slate-500 dark:text-white/45">{t('ui.pages.assessmentResult.scoringVersion', { version: result.metricSnapshot?.scoringVersion || '—' })}</div></div>
            <div className="mt-6 grid gap-4 md:grid-cols-2 xl:grid-cols-4">
              {dimensions.map((dimension) => <div key={dimension.code} className="rounded-[1.6rem] border border-slate-200/70 bg-white/60 p-4 dark:border-white/10 dark:bg-white/5"><div className="text-sm font-bold break-words">{dimension.code}</div><div className="mt-2 text-3xl font-black">{dimension.accuracy == null ? '—' : `${Math.round(dimension.accuracy * 100)}%`}</div><div className="mt-1 text-xs text-slate-500 dark:text-white/45">{dimension.correctCount} / {dimension.itemCount} 题</div></div>)}
              <div className="rounded-[1.6rem] border border-slate-200/70 bg-white/60 p-4 dark:border-white/10 dark:bg-white/5"><div className="text-sm font-bold">{t('ui.pages.assessmentResult.medianReactionTime')}</div><div className="mt-2 text-3xl font-black">{result.metricSnapshot?.reactionTime.medianMs == null ? '—' : `${Math.round(result.metricSnapshot.reactionTime.medianMs)} ms`}</div><div className="mt-1 text-xs text-slate-500 dark:text-white/45">{t('ui.pages.assessmentResult.sampleCount', { count: result.metricSnapshot?.reactionTime.sampleCount ?? 0 })}</div></div>
            </div>
            {!!result.qualityFlags?.length && <div className="mt-5 flex items-start gap-3 rounded-[1.4rem] border border-amber-500/20 bg-amber-500/10 p-4 text-sm text-amber-800 dark:text-amber-200"><AlertTriangle size={17} className="mt-0.5 shrink-0" /><span className="break-words">{t('ui.pages.assessmentResult.qualityFlags', { flags: result.qualityFlags.join('、') })}</span></div>}
          </section>

          <section className="grid gap-6 xl:grid-cols-2" aria-labelledby="result-weaknesses">
            <div className="rounded-[2.4rem] liquid-glass-panel p-8"><SectionEyebrow>{t('ui.pages.assessmentResult.weaknessEyebrow')}</SectionEyebrow><h2 id="result-weaknesses" className="mt-2 text-2xl font-black">{t('ui.pages.assessmentResult.weaknessTitle')}</h2>{result.aiAnalysis?.risks?.length ? <ul className="mt-5 space-y-3">{result.aiAnalysis.risks.map((risk) => <li key={risk} className="flex gap-3 text-sm leading-7 text-slate-600 dark:text-white/65"><Target size={16} className="mt-1 shrink-0 text-rose-500" /><span className="break-words">{risk}</span></li>)}</ul> : <p className="mt-5 text-sm text-slate-500 dark:text-white/45">{t('ui.pages.assessmentResult.noRisks')}</p>}</div>
            <div className="rounded-[2.4rem] liquid-glass-panel p-8"><div className="flex items-center justify-between gap-3"><SectionEyebrow>{t('ui.pages.assessmentResult.itemEvidence')}</SectionEyebrow><span className="text-sm font-bold text-rose-500">{t('ui.pages.assessmentResult.reviewCount', { count: incorrectQuestions.length })}</span></div>{result.answerReviewVisible ? (incorrectQuestions.length ? <div className="mt-5 space-y-3">{incorrectQuestions.slice(0, 5).map((question) => <div key={question.answerId} className="rounded-[1.4rem] border border-rose-500/15 bg-rose-500/5 p-4"><div className="font-bold break-words">第 {question.questionOrder} 题 · {question.stemText}</div><div className="mt-2 text-sm text-slate-500 dark:text-white/45 break-words">你的答案：{question.responses.join(' / ') || '未作答'}</div></div>)}</div> : <p className="mt-5 text-sm text-slate-500 dark:text-white/45">{t('ui.pages.assessmentResult.noMistakes')}</p>) : <p className="mt-5 text-sm text-slate-500 dark:text-white/45">{t('ui.pages.assessmentResult.reviewNotOpen')}</p>}</div>
          </section>

          <ChartCard title={t('ui.pages.assessmentResult.trendTitle')} description={t('ui.pages.assessmentResult.trendDescription')} option={buildAssessmentProgressOption(result.questions)} isEmpty={!result.questions.length} emptyState={{ title: t('ui.pages.assessmentResult.trendEmptyTitle'), description: t('ui.pages.assessmentResult.trendEmptyDescription') }} />

          <section className="rounded-[2.4rem] border border-primary/20 bg-primary/5 p-8" aria-labelledby="result-actions"><SectionEyebrow>{t('ui.pages.assessmentResult.actionsEyebrow')}</SectionEyebrow><h2 id="result-actions" className="mt-2 text-2xl font-black">{t('ui.pages.assessmentResult.actionsTitle')}</h2><div className="mt-5 grid gap-3 md:grid-cols-3">{(result.aiAnalysis?.recommendations || [t('ui.pages.assessmentResult.defaultRecommendation1'), t('ui.pages.assessmentResult.defaultRecommendation2'), t('ui.pages.assessmentResult.defaultRecommendation3')]).map((recommendation, index) => <div key={`${index}-${recommendation}`} className="rounded-[1.5rem] border border-primary/15 bg-white/70 p-4 dark:bg-white/5"><div className="text-xs font-black uppercase tracking-[0.22em] text-primary">0{index + 1}</div><p className="mt-3 text-sm leading-7 break-words">{recommendation}</p></div>)}</div><div className="mt-6 flex flex-wrap gap-3"><Link to="/training" className="btn-liquid inline-flex items-center gap-2 px-5 py-3 text-white">{t('ui.pages.assessmentResult.startTargetedTraining)}<ArrowRight size={15} /></Link><Link to="/errors" className="inline-flex items-center gap-2 rounded-full border border-primary/25 px-5 py-3 text-sm font-bold text-primary">{t('ui.pages.assessmentResult.scheduleReview')}</Link></div></section>

          {result.instructionsText && (
            <div className="rounded-[2rem] border border-dashed border-slate-200/80 px-5 py-4 text-sm text-slate-600 dark:border-white/10 dark:text-white/60">
              {result.instructionsText}
            </div>
          )}

          {result.answerReviewVisible && <section aria-labelledby="result-review"><SectionEyebrow className="mb-4">{t('ui.pages.assessmentResult.detailedReview')}</SectionEyebrow><h2 id="result-review" className="sr-only">{t('ui.pages.assessmentResult.detailedReview')}</h2><div className="space-y-5">
            {result.questions.map((question) => (
              <div key={question.answerId} className="rounded-[2.4rem] liquid-glass-panel p-6 md:p-8 space-y-5">
                <div className="flex flex-wrap items-start justify-between gap-4">
                  <div>
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
                              <div className="mt-1 break-words">{option.label}</div>
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
                    <div className="break-words rounded-[1.4rem] border border-dashed border-slate-200/80 px-4 py-4 text-sm text-slate-600 dark:border-white/10 dark:text-white/60">
                    {question.explanationText}
                  </div>
                )}
              </div>
            ))}
          </div></section>}
        </>
      )}
    </div>
  );
};

export default StudentAssessmentResultPage;
