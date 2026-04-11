import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { AlertTriangle, ArrowRight, Brain, Clock3, FileText, RefreshCw, Target } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { ChartCard } from '@/components/common/ChartCard';
import { PageHeader, SectionEyebrow, StatCard, StatusBadge } from '@/components/common';
import { getApiErrorMessage, normalizeApiError } from '@/lib/api';
import type { AppChartOption } from '@/lib/echarts';
import {
  assessmentAttemptStatusLabel,
  assessmentAttemptStatusTone,
  buildRadarOption,
  buildTrendOption,
  errorTypeLabel,
  formatDateTime,
  formatMaybePercent,
  formatMs,
  lexicalPairTypeLabel,
  riskLevelLabel,
  trainingModeLabel,
} from '@/lib/format';
import { assessmentService, studentService, trainingService } from '@/lib/services';
import { buildTrainingHref } from '@/lib/training-launch';
import type { StudentAssessmentSummaryVO } from '@/lib/contracts';
import type { TFunction } from 'i18next';

function resolveAssessmentDashboardAction(item: StudentAssessmentSummaryVO, now: number, t: TFunction) {
  const startsAt = item.startsAt ? new Date(item.startsAt).getTime() : null;
  const dueAt = item.dueAt ? new Date(item.dueAt).getTime() : null;
  if (item.attemptStatus === 'SUBMITTED' && item.attemptId) {
    return {
      label: t('ui.actions.viewResult'),
      disabled: false,
      to: `/assessments/attempts/${item.attemptId}/result`,
    };
  }
  if (item.attemptId) {
    return {
      label: t('ui.actions.continueAnswering'),
      disabled: false,
      to: `/assessments/attempts/${item.attemptId}`,
    };
  }
  if (startsAt && startsAt > now) {
    return {
      label: t('ui.meta.notStarted'),
      disabled: true,
      to: '/assessments',
    };
  }
  if (dueAt && dueAt <= now) {
    return {
      label: t('ui.meta.dueAt', { time: formatDateTime(item.dueAt) }),
      disabled: true,
      to: '/assessments',
    };
  }
  return {
    label: t('ui.actions.enterAssessmentCenter'),
    disabled: false,
    to: '/assessments',
  };
}

const DashboardPage: React.FC = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();

  const overviewQuery = useQuery({
    queryKey: ['student-overview'],
    queryFn: ({ signal }) => studentService.getOverview({ signal }),
  });
  const trendsQuery = useQuery({
    queryKey: ['student-trends', '7d'],
    queryFn: ({ signal }) => studentService.getTrends('7d', 'day', { signal }),
  });
  const highRiskPairsQuery = useQuery({
    queryKey: ['student-high-risk-pairs', '30d', 5],
    queryFn: ({ signal }) => studentService.getHighRiskPairs('30d', 5, { signal }),
  });
  const errorDistributionQuery = useQuery({
    queryKey: ['student-error-distribution', '30d'],
    queryFn: ({ signal }) => studentService.getErrorDistribution('30d', { signal }),
  });
  const recommendedPlanQuery = useQuery({
    queryKey: ['recommended-training-plan'],
    queryFn: ({ signal }) => trainingService.getRecommendedPlan(undefined, { signal }),
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
  const assessmentsQuery = useQuery({
    queryKey: ['student-assessments', 'dashboard'],
    queryFn: ({ signal }) => assessmentService.listStudentAssessments({ signal }),
  });

  const overview = overviewQuery.data;
  const topCards = overview?.cards.slice(0, 4) ?? [];
  const now = Date.now();
  const assessmentItems = React.useMemo(() => {
    const items = (assessmentsQuery.data || []).slice();
    items.sort((left, right) => {
      const leftRank = left.attemptStatus === 'IN_PROGRESS' ? 0 : left.attemptStatus === 'SUBMITTED' ? 2 : 1;
      const rightRank = right.attemptStatus === 'IN_PROGRESS' ? 0 : right.attemptStatus === 'SUBMITTED' ? 2 : 1;
      if (leftRank !== rightRank) {
        return leftRank - rightRank;
      }
      return new Date(right.publishedAt).getTime() - new Date(left.publishedAt).getTime();
    });
    return items.slice(0, 3);
  }, [assessmentsQuery.data]);
  const errorDistributionOption: AppChartOption = {
    tooltip: { trigger: 'item' },
    series: [
      {
        type: 'pie',
        radius: ['48%', '76%'],
        data: (errorDistributionQuery.data || []).map((item) => ({ name: item.label, value: item.count })),
      },
    ],
  };

  const planError = recommendedPlanQuery.error ? normalizeApiError(recommendedPlanQuery.error) : null;

  return (
    <div className="space-y-10 pb-20">
      <PageHeader
        eyebrow={t('shell.nav.dashboard')}
        title={t('dashboard.title')}
        subtitle={t('dashboard.subtitle')}
        actions={
          <button onClick={() => navigate('/diagnosis')} className="btn-liquid px-6 py-3 text-white">
            {t('common.actions.startDiagnosis')}
          </button>
        }
      />

      {overviewQuery.error && (
        <div className="rounded-[2rem] border border-rose-500/20 bg-rose-500/5 p-6 text-rose-500">
          {getApiErrorMessage(overviewQuery.error)}
        </div>
      )}

      <section className="liquid-glass-panel rounded-[3rem] p-10 edge-light">
        <div className="flex flex-col items-start justify-between gap-8 lg:flex-row">
          <div className="max-w-3xl">
            <SectionEyebrow>{t('ui.sections.studentProfile')}</SectionEyebrow>
            <h1 className="mt-3 text-4xl font-black tracking-tight text-slate-900 dark:text-white md:text-5xl">
              {overview?.studentName || t('common.loading.initializingSession')}，{t('ui.meta.primaryRisk', { risk: riskLevelLabel(overview?.primaryRiskLevel) })}
            </h1>
            <p className="mt-4 leading-7 text-slate-500 dark:text-white/50">
              {t('ui.meta.recommendedMode', { mode: trainingModeLabel(overview?.recommendedTrainingMode) })}。{t('ui.meta.recentActiveAt', {
                time: formatDateTime(overview?.latestSnapshot.lastActiveAt),
              })}，{t('ui.meta.pendingReviewPairs', {
                count: overview?.latestSnapshot.pendingReviewCount ?? 0,
              })}。
            </p>
          </div>
          <div className="grid min-w-[280px] gap-4 sm:grid-cols-2">
            <div className="rounded-[2rem] border border-slate-200/80 bg-white/60 p-5 dark:border-white/10 dark:bg-white/5">
              <SectionEyebrow className="text-xs">{t('ui.fields.englishLevel')} / {t('ui.fields.frenchLevel')}</SectionEyebrow>
              <div className="mt-3 text-2xl font-black text-slate-900 dark:text-white">
                {overview?.englishLevel || '--'} / {overview?.frenchLevel || '--'}
              </div>
            </div>
            <div className="rounded-[2rem] border border-slate-200/80 bg-white/60 p-5 dark:border-white/10 dark:bg-white/5">
              <SectionEyebrow className="text-xs">{t('ui.fields.averageReactionTime')}</SectionEyebrow>
              <div className="mt-3 text-2xl font-black text-slate-900 dark:text-white">
                {formatMs(overview?.latestSnapshot.recentAvgReactionTimeMs)}
              </div>
            </div>
          </div>
        </div>
      </section>

      <div className="grid grid-cols-1 gap-6 md:grid-cols-2 xl:grid-cols-4">
        {topCards.map((card) => (
          <StatCard
            key={card.key}
            title={card.label}
            value={`${card.value.toFixed(card.unit === '%' ? 0 : 0)}${card.unit || ''}`}
            icon={card.key.includes('risk') ? AlertTriangle : card.key.includes('latency') ? Clock3 : card.key.includes('context') ? RefreshCw : Target}
          />
        ))}
      </div>

      <div className="grid grid-cols-1 gap-8 xl:grid-cols-[1.3fr_0.7fr]">
        <ChartCard
          title={t('ui.charts.trend7d')}
          option={buildTrendOption(trendsQuery.data)}
          loading={trendsQuery.isLoading}
          isEmpty={!trendsQuery.data?.series.length}
        />
        <ChartCard
          title={t('ui.charts.overallAbilityRadar')}
          option={buildRadarOption(overview?.radar)}
          loading={overviewQuery.isLoading}
          isEmpty={!overview?.radar.length}
        />
      </div>

      <div className="grid grid-cols-1 gap-8 xl:grid-cols-[1fr_1fr_0.9fr]">
        <ChartCard
          title={t('ui.sections.errorDistribution')}
          option={errorDistributionOption}
          loading={errorDistributionQuery.isLoading}
          isEmpty={!errorDistributionQuery.data?.length}
        />

        <section className="liquid-glass-panel rounded-[2.5rem] p-8">
          <SectionEyebrow className="mb-6">{t('ui.sections.highRiskPairs')}</SectionEyebrow>
          <div className="space-y-4">
            {(highRiskPairsQuery.data || []).map((item) => (
              <div key={item.lexicalPairId} className="rounded-[1.5rem] border border-slate-200/70 bg-white/60 p-4 dark:border-white/10 dark:bg-white/5">
                <div className="flex items-center justify-between gap-4">
                  <div>
                    <div className="font-black text-slate-900 dark:text-white">{item.englishWord} / {item.frenchWord}</div>
                    <div className="mt-1 text-sm text-slate-500 dark:text-white/45">{lexicalPairTypeLabel(item.lexicalPairType)}</div>
                  </div>
                  <div className="text-right">
                    <div className="font-black text-rose-500">{formatMaybePercent(item.riskScore, 0)}</div>
                    <div className="text-xs text-slate-400 dark:text-white/30">{t('ui.meta.risk')}</div>
                  </div>
                </div>
              </div>
            ))}
            {!highRiskPairsQuery.isLoading && !highRiskPairsQuery.data?.length && (
              <div className="text-sm text-slate-500 dark:text-white/45">{t('ui.labels.noHighRiskPairs')}</div>
            )}
          </div>
        </section>

        <section className="liquid-glass-panel rounded-[2.5rem] p-8">
          <SectionEyebrow className="mb-4">{t('ui.sections.recommendedPlan')}</SectionEyebrow>
          {recommendedPlanQuery.data ? (
            <div className="space-y-4">
              <div className="text-2xl font-black text-slate-900 dark:text-white">{trainingModeLabel(recommendedPlanQuery.data.priorityMode)}</div>
              <p className="text-sm leading-6 text-slate-500 dark:text-white/45">{recommendedPlanQuery.data.recommendationReason}</p>
              <div className="space-y-3">
                {recommendedPlanQuery.data.suggestedSessions.map((session) => (
                  <button
                    key={session.mode}
                    type="button"
                    onClick={() =>
                      navigate(
                        buildTrainingHref({
                          mode: session.mode,
                          source: 'dashboard',
                          diagnosisSummaryId: recommendedPlanQuery.data?.sourceDiagnosisSummaryId,
                        })
                      )
                    }
                    className="w-full rounded-[1.4rem] border border-slate-200/70 p-4 text-left transition-all hover:border-primary/40 dark:border-white/10"
                  >
                    <div className="font-bold text-slate-900 dark:text-white">{session.label}</div>
                    <div className="mt-1 text-sm text-slate-500 dark:text-white/45">{t('training.suggestedQuestionCount', { count: session.count })}</div>
                  </button>
                ))}
              </div>
            </div>
          ) : planError?.status === 409 ? (
            <div className="space-y-3">
              <div className="text-lg font-black text-slate-900 dark:text-white">{t('ui.labels.noPlanTitle')}</div>
              <p className="text-sm leading-6 text-slate-500 dark:text-white/45">{t('ui.labels.noPlanDescription')}</p>
              <button type="button" onClick={() => navigate('/diagnosis')} className="btn-liquid px-5 py-3 text-white">
                {t('ui.actions.goDiagnosisNow')}
              </button>
            </div>
          ) : recommendedPlanQuery.isLoading ? (
            <div className="text-sm text-slate-500 dark:text-white/45">{t('ui.labels.loadingPlan')}</div>
          ) : (
            <div className="text-sm text-rose-500">{planError?.message || t('ui.labels.recommendedPlanFailed')}</div>
          )}
        </section>
      </div>

      <section className="liquid-glass-panel rounded-[2.5rem] p-8">
        <div className="mb-6 flex items-center justify-between gap-4">
          <div>
            <SectionEyebrow>{t('ui.sections.assessments')}</SectionEyebrow>
            <div className="mt-3 text-2xl font-black text-slate-900 dark:text-white">老师发布的整卷任务</div>
            <div className="mt-2 text-sm text-slate-500 dark:text-white/45">这里展示最近需要处理的通用测评，包括继续作答、查看结果和即将开始的整卷任务。</div>
          </div>
          <button type="button" onClick={() => navigate('/assessments')} className="flex items-center gap-2 text-sm font-bold text-primary">
            {t('ui.actions.openAssessmentCenter')} <ArrowRight size={14} />
          </button>
        </div>

        {assessmentsQuery.isLoading ? (
          <div className="text-sm text-slate-500 dark:text-white/45">{t('ui.labels.loadingAssessments')}</div>
        ) : assessmentsQuery.error ? (
          <div className="rounded-[1.6rem] border border-rose-500/20 bg-rose-500/5 px-4 py-3 text-sm text-rose-500">
            {getApiErrorMessage(assessmentsQuery.error)}
          </div>
        ) : !assessmentItems.length ? (
          <div className="rounded-[1.6rem] border border-dashed border-slate-300 bg-white/55 px-4 py-5 text-sm text-slate-500 dark:border-white/15 dark:bg-white/[0.02] dark:text-white/45">
            {t('ui.labels.noAssessments')}
          </div>
        ) : (
          <div className="grid gap-4 xl:grid-cols-3">
            {assessmentItems.map((item) => {
              const action = resolveAssessmentDashboardAction(item, now, t);
              return (
                <div key={item.publishId} className="rounded-[1.8rem] border border-slate-200/70 bg-white/60 p-5 dark:border-white/10 dark:bg-white/5">
                  <div className="inline-flex rounded-2xl bg-primary/10 p-3 text-primary">
                    <FileText size={18} />
                  </div>
                  <div className="mt-4 flex items-start justify-between gap-3">
                    <div>
                      <SectionEyebrow>{item.className}</SectionEyebrow>
                      <div className="mt-2 text-xl font-black text-slate-900 dark:text-white">{item.title}</div>
                    </div>
                    <StatusBadge
                      label={item.attemptStatus ? assessmentAttemptStatusLabel(item.attemptStatus) : t('ui.meta.notStarted')}
                      tone={assessmentAttemptStatusTone(item.attemptStatus)}
                    />
                  </div>
                  <div className="mt-3 grid gap-2 text-sm text-slate-500 dark:text-white/45">
                    <div>{t('ui.meta.startsAt', { time: formatDateTime(item.startsAt) })}</div>
                    <div>{t('ui.meta.dueAt', { time: formatDateTime(item.dueAt) })}</div>
                    <div>{t('ui.meta.progress', { current: item.answeredCount || 0, total: item.questionCount })}</div>
                  </div>
                  {item.instructionsText && (
                    <div className="mt-4 rounded-[1.2rem] border border-dashed border-slate-200/80 px-4 py-3 text-sm text-slate-600 dark:border-white/10 dark:text-white/60">
                      {item.instructionsText}
                    </div>
                  )}
                  <button
                    type="button"
                    disabled={action.disabled}
                    onClick={() => navigate(action.to)}
                    className={`mt-4 rounded-full px-5 py-3 text-sm font-bold ${
                      action.disabled
                        ? 'border border-slate-200 bg-white/70 text-slate-400 dark:border-white/10 dark:bg-white/5 dark:text-white/30'
                        : 'btn-liquid text-white'
                    }`}
                  >
                    {action.label}
                  </button>
                </div>
              );
            })}
          </div>
        )}
      </section>

      <div className="grid grid-cols-1 gap-8 xl:grid-cols-2">
        <section className="liquid-glass-panel rounded-[2.5rem] p-8">
          <div className="mb-6 flex items-center justify-between">
            <SectionEyebrow>{t('ui.sections.wrongBook')}</SectionEyebrow>
            <div className="flex items-center gap-4">
              {!!wrongBookQuery.data?.length && (
                <button
                  type="button"
                  onClick={() =>
                    navigate(
                      buildTrainingHref({
                        mode: wrongBookQuery.data[0].recommendedMode,
                        source: 'dashboard-wrong-book',
                        lexicalPairId: wrongBookQuery.data[0].lexicalPairId,
                        wrongBookId: wrongBookQuery.data[0].wrongBookId,
                      })
                    )
                  }
                  className="text-sm font-bold text-primary"
                >
                  {t('ui.actions.remediateNow')}
                </button>
              )}
              <button type="button" onClick={() => navigate('/errors')} className="flex items-center gap-2 text-sm text-primary">
                {t('ui.actions.viewAll')} <ArrowRight size={14} />
              </button>
            </div>
          </div>
          {wrongBookQuery.isLoading ? (
            <div className="text-sm text-slate-500 dark:text-white/45">{t('ui.labels.loadingWrongBook')}</div>
          ) : wrongBookQuery.error ? (
            <div className="rounded-[1.6rem] border border-rose-500/20 bg-rose-500/5 px-4 py-3 text-sm text-rose-500">
              {getApiErrorMessage(wrongBookQuery.error)}
            </div>
          ) : !wrongBookQuery.data?.length ? (
            <div className="text-sm text-slate-500 dark:text-white/45">{t('ui.labels.noWrongBook')}</div>
          ) : (
            <div className="space-y-4">
              {wrongBookQuery.data.slice(0, 4).map((item) => (
                <div key={item.wrongBookId} className="rounded-[1.4rem] border border-slate-200/70 bg-white/60 p-4 dark:border-white/10 dark:bg-white/5">
                  <div className="flex items-start justify-between gap-4">
                    <div>
                      <div className="font-bold text-slate-900 dark:text-white">{item.englishWord} / {item.frenchWord}</div>
                      <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                        {trainingModeLabel(item.recommendedMode)} · {t('ui.meta.recentWrongCount', {
                          type: errorTypeLabel(item.lastErrorType),
                          count: item.wrongCount,
                        })}
                      </div>
                    </div>
                    <button
                      type="button"
                      onClick={() =>
                        navigate(
                          buildTrainingHref({
                            mode: item.recommendedMode,
                            source: 'dashboard-wrong-book-item',
                            lexicalPairId: item.lexicalPairId,
                            wrongBookId: item.wrongBookId,
                          })
                        )
                      }
                      className="rounded-full border border-slate-200 px-4 py-2 text-sm font-bold text-primary dark:border-white/10"
                    >
                      {t('ui.actions.start')}
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>

        <section className="liquid-glass-panel rounded-[2.5rem] p-8">
          <div className="mb-6 flex items-center justify-between gap-3">
            <div className="flex items-center gap-3">
              <Brain size={16} className="text-primary" />
              <SectionEyebrow>{t('ui.sections.reviewSchedule')}</SectionEyebrow>
            </div>
            {!!reviewScheduleQuery.data?.length && (
              <button
                type="button"
                onClick={() =>
                  navigate(
                    buildTrainingHref({
                      mode: reviewScheduleQuery.data[0].reviewMode,
                      source: 'dashboard-review',
                      lexicalPairId: reviewScheduleQuery.data[0].lexicalPairId,
                      wrongBookId: reviewScheduleQuery.data[0].wrongBookId,
                      reviewScheduleId: reviewScheduleQuery.data[0].reviewScheduleId,
                    })
                  )
                }
                className="text-sm text-primary"
              >
                {t('ui.actions.startReviewNow')}
              </button>
            )}
          </div>
          {reviewScheduleQuery.isLoading ? (
            <div className="text-sm text-slate-500 dark:text-white/45">{t('ui.labels.loadingReviewSchedule')}</div>
          ) : reviewScheduleQuery.error ? (
            <div className="rounded-[1.6rem] border border-rose-500/20 bg-rose-500/5 px-4 py-3 text-sm text-rose-500">
              {getApiErrorMessage(reviewScheduleQuery.error)}
            </div>
          ) : !reviewScheduleQuery.data?.length ? (
            <div className="text-sm text-slate-500 dark:text-white/45">{t('ui.labels.noReviewItems')}</div>
          ) : (
            <div className="space-y-4">
              {reviewScheduleQuery.data.slice(0, 4).map((item) => (
                <div key={item.reviewScheduleId} className="rounded-[1.4rem] border border-slate-200/70 bg-white/60 p-4 dark:border-white/10 dark:bg-white/5">
                  <div className="flex items-center justify-between gap-4">
                    <div>
                      <div className="font-bold text-slate-900 dark:text-white">{item.englishWord} / {item.frenchWord}</div>
                      <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                        {trainingModeLabel(item.reviewMode)} · {t('ui.meta.reviewStage', {
                          stage: item.scheduleStage,
                          days: item.intervalDays,
                        })}
                      </div>
                    </div>
                    <div className="flex flex-col items-end gap-3">
                      <div className="text-sm text-slate-500 dark:text-white/45">{formatDateTime(item.dueAt)}</div>
                      <button
                        type="button"
                        onClick={() =>
                          navigate(
                            buildTrainingHref({
                              mode: item.reviewMode,
                              source: 'dashboard-review-item',
                              lexicalPairId: item.lexicalPairId,
                              wrongBookId: item.wrongBookId,
                              reviewScheduleId: item.reviewScheduleId,
                            })
                          )
                        }
                        className="rounded-full border border-slate-200 px-4 py-2 text-sm font-bold text-primary dark:border-white/10"
                      >
                        {t('ui.actions.start')}
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>
      </div>
    </div>
  );
};

export default DashboardPage;
