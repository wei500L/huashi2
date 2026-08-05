import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AlertTriangle, ArrowRight, BookOpen, Brain, Clock3, FileText, Flame, RefreshCw, Target } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { ChartCard } from '@/components/common/ChartCard';
import { PageHeader, SectionEyebrow, StatCard, StatusBadge } from '@/components/common';
import { TrainingModeSummaryCard } from '@/components/common/TrainingModeSummaryCard';
import { OnboardingTour, useOnboardingTour } from '@/features/onboarding';
import { getApiErrorMessage, normalizeApiError } from '@/lib/api';
import { buildDiagnosisHref } from '@/lib/diagnosis-launch';
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
import type { StudentAchievementBadgeVO, StudentAnalyticsOverviewVO, StudentAssessmentSummaryVO } from '@/lib/contracts';
import { useAuthStore } from '@/store';
import type { LucideIcon } from 'lucide-react';
import type { TFunction } from 'i18next';

const ACHIEVEMENT_META: Record<
  string,
  {
    icon: LucideIcon;
    iconClassName: string;
    progressClassName: string;
    progressType: 'days' | 'count' | 'accuracy' | 'mastery';
  }
> = {
  LOGIN_STREAK: {
    icon: Flame,
    iconClassName: 'bg-amber-500/15 text-amber-500',
    progressClassName: 'from-amber-400 via-orange-500 to-rose-500',
    progressType: 'days',
  },
  DIAGNOSIS_FINISHER: {
    icon: Brain,
    iconClassName: 'bg-sky-500/15 text-sky-500',
    progressClassName: 'from-sky-400 via-cyan-500 to-blue-500',
    progressType: 'count',
  },
  TRAINING_EXPERT: {
    icon: Target,
    iconClassName: 'bg-emerald-500/15 text-emerald-500',
    progressClassName: 'from-emerald-400 via-teal-500 to-cyan-500',
    progressType: 'accuracy',
  },
  VOCAB_MASTER: {
    icon: BookOpen,
    iconClassName: 'bg-ai/15 text-ai',
    progressClassName: 'from-ai via-progress to-info',
    progressType: 'mastery',
  },
};

function resolveAssessmentDashboardAction(item: StudentAssessmentSummaryVO, now: number, t: TFunction) {
  const startsAt = item.startsAt ? new Date(item.startsAt).getTime() : null;
  const dueAt = item.dueAt ? new Date(item.dueAt).getTime() : null;
  if (item.attemptStatus === 'SUBMITTED' && item.attemptId) {
    if (item.releaseStatus === 'PENDING') {
      return {
        label: `结果将于 ${formatDateTime(item.resultAvailableAt)} 公布`,
        disabled: true,
        to: '/assessments',
      };
    }
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

function resolveAchievementProgressLabel(badge: StudentAchievementBadgeVO, t: TFunction) {
  const meta = ACHIEVEMENT_META[badge.code];
  switch (meta?.progressType) {
    case 'days':
      return t('ui.achievements.progressDays', { current: badge.progressValue, target: badge.targetValue });
    case 'accuracy':
      return t('ui.achievements.progressAccuracy', { current: badge.progressValue, target: badge.targetValue });
    case 'mastery':
      return t('ui.achievements.progressMastery', { current: badge.progressValue, target: badge.targetValue });
    default:
      return t('ui.achievements.progressCount', { current: badge.progressValue, target: badge.targetValue });
  }
}

function resolveGoalProgress(current: number, target?: number | null): number {
  if (!target || target <= 0) {
    return 0;
  }
  return Math.max(0, Math.min(100, Math.round((current / target) * 100)));
}

function formatGoalPercent(value?: number | null): string {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return '--';
  }
  return `${value.toFixed(1)}%`;
}

const DashboardPage: React.FC = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const user = useAuthStore((state) => state.user);
  const [dailyTrainingTargetInput, setDailyTrainingTargetInput] = React.useState('');
  const [weeklyAccuracyTargetInput, setWeeklyAccuracyTargetInput] = React.useState('');
  const [goalErrorMessage, setGoalErrorMessage] = React.useState<string | null>(null);
  const [goalSuccessMessage, setGoalSuccessMessage] = React.useState<string | null>(null);
  const [isGoalDirty, setIsGoalDirty] = React.useState(false);

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
  const learningGoalMutation = useMutation({
    mutationFn: (payload: { dailyTrainingTarget?: number | null; weeklyAccuracyTarget?: number | null }) =>
      studentService.updateLearningGoals(payload),
    onSuccess: (learningGoal) => {
      setGoalErrorMessage(null);
      setGoalSuccessMessage(t('ui.messages.learningGoalsSaved'));
      setIsGoalDirty(false);
      setDailyTrainingTargetInput(learningGoal.dailyTrainingTarget ? String(learningGoal.dailyTrainingTarget) : '');
      setWeeklyAccuracyTargetInput(learningGoal.weeklyAccuracyTarget ? String(learningGoal.weeklyAccuracyTarget) : '');
      queryClient.setQueryData<StudentAnalyticsOverviewVO | undefined>(['student-overview'], (current) =>
        current ? { ...current, learningGoal } : current
      );
    },
    onError: (error) => {
      setGoalSuccessMessage(null);
      setGoalErrorMessage(getApiErrorMessage(error, t('ui.errors.learningGoalsSaveFailed')));
    },
  });

  const overview = overviewQuery.data;
  const achievementWall = overview?.achievementWall;
  const learningGoal = overview?.learningGoal;
  const topCards = overview?.cards.slice(0, 4) ?? [];
  const dailyGoalProgress = resolveGoalProgress(learningGoal?.dailyTrainingCompletedToday ?? 0, learningGoal?.dailyTrainingTarget);
  const weeklyGoalProgress = resolveGoalProgress(learningGoal?.weeklyAccuracyCurrent ?? 0, learningGoal?.weeklyAccuracyTarget);
  const dailyGoalStatus = !learningGoal?.dailyTrainingTarget
    ? t('ui.labels.goalUnset')
    : learningGoal.dailyTrainingRemaining === 0
      ? t('ui.labels.goalCompleted')
      : t('ui.meta.wordsRemaining', { count: learningGoal.dailyTrainingRemaining });
  const weeklyGoalStatus = !learningGoal?.weeklyAccuracyTarget
    ? t('ui.labels.goalUnset')
    : learningGoal.weeklyAccuracyDelta >= 0
      ? t('ui.meta.accuracyAhead', { count: learningGoal.weeklyAccuracyDelta.toFixed(1) })
      : t('ui.meta.accuracyGap', { count: Math.abs(learningGoal.weeklyAccuracyDelta).toFixed(1) });
  const now = Date.now();
  const dueReviewItems = (reviewScheduleQuery.data || []).filter((item) => new Date(item.dueAt).getTime() <= now);
  const nextDueReviewItem = dueReviewItems[0] || reviewScheduleQuery.data?.[0] || null;
  const studentOnboarding = useOnboardingTour({
    tourId: 'student-dashboard',
    userId: user?.id,
  });
  const onboardingSteps = React.useMemo(
    () => [
      {
        id: 'quick-start',
        selector: '[data-onboarding="student-dashboard-quick-start"]',
        title: t('ui.onboarding.studentDashboard.quickStart.title'),
        description: t('ui.onboarding.studentDashboard.quickStart.description'),
        placement: 'bottom' as const,
      },
      {
        id: 'learning-goals',
        selector: '[data-onboarding="student-dashboard-learning-goals"]',
        title: t('ui.onboarding.studentDashboard.learningGoals.title'),
        description: t('ui.onboarding.studentDashboard.learningGoals.description'),
        placement: 'top' as const,
      },
      {
        id: 'recommended-plan',
        selector: '[data-onboarding="student-dashboard-recommended-plan"]',
        title: t('ui.onboarding.studentDashboard.recommendedPlan.title'),
        description: t('ui.onboarding.studentDashboard.recommendedPlan.description'),
        placement: 'left' as const,
      },
      {
        id: 'review-schedule',
        selector: '[data-onboarding="student-dashboard-review-schedule"]',
        title: t('ui.onboarding.studentDashboard.reviewSchedule.title'),
        description: t('ui.onboarding.studentDashboard.reviewSchedule.description'),
        placement: 'top' as const,
      },
    ],
    [t]
  );

  React.useEffect(() => {
    if (!learningGoal || isGoalDirty) {
      return;
    }
    setDailyTrainingTargetInput(learningGoal.dailyTrainingTarget ? String(learningGoal.dailyTrainingTarget) : '');
    setWeeklyAccuracyTargetInput(learningGoal.weeklyAccuracyTarget ? String(learningGoal.weeklyAccuracyTarget) : '');
  }, [isGoalDirty, learningGoal]);

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

  const handleLearningGoalSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const dailyValue = dailyTrainingTargetInput.trim();
    const weeklyValue = weeklyAccuracyTargetInput.trim();
    const hasDailyTarget = dailyValue !== '';
    const hasWeeklyTarget = weeklyValue !== '';
    const parsedDailyTrainingTarget = Number(dailyValue);
    const parsedWeeklyAccuracyTarget = Number(weeklyValue);
    const dailyTrainingTarget = hasDailyTarget ? parsedDailyTrainingTarget : null;
    const weeklyAccuracyTarget = hasWeeklyTarget ? parsedWeeklyAccuracyTarget : null;

    if (hasDailyTarget && (!Number.isInteger(parsedDailyTrainingTarget) || parsedDailyTrainingTarget < 1 || parsedDailyTrainingTarget > 500)) {
      setGoalSuccessMessage(null);
      setGoalErrorMessage(t('ui.validation.dailyTrainingGoalRange'));
      return;
    }
    if (hasWeeklyTarget && (!Number.isInteger(parsedWeeklyAccuracyTarget) || parsedWeeklyAccuracyTarget < 1 || parsedWeeklyAccuracyTarget > 100)) {
      setGoalSuccessMessage(null);
      setGoalErrorMessage(t('ui.validation.weeklyAccuracyGoalRange'));
      return;
    }

    setGoalErrorMessage(null);
    await learningGoalMutation.mutateAsync({
      dailyTrainingTarget,
      weeklyAccuracyTarget,
    });
  };

  return (
    <div className="space-y-10 pb-20">
      <PageHeader
        eyebrow={t('shell.nav.dashboard')}
        title={t('dashboard.title')}
        subtitle={t('dashboard.subtitle')}
        actions={
          <div data-onboarding="student-dashboard-quick-start">
            <button onClick={() => navigate(buildDiagnosisHref({ source: 'dashboard' }))} className="btn-liquid px-6 py-3 text-white">
              {t('common.actions.startDiagnosis')}
            </button>
          </div>
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

      <section className="rounded-[2.8rem] border border-amber-500/20 bg-[linear-gradient(135deg,rgba(251,191,36,0.12),rgba(249,115,22,0.08))] p-8 shadow-[0_24px_80px_rgba(245,158,11,0.12)]">
        <div className="flex flex-col gap-6 lg:flex-row lg:items-start lg:justify-between">
          <div className="max-w-3xl">
            <div className="flex items-center gap-3">
              <SectionEyebrow className="text-amber-600 dark:text-amber-300">到期复习提醒</SectionEyebrow>
              <StatusBadge label={`${dueReviewItems.length} 项到期`} tone={dueReviewItems.length > 0 ? 'warning' : 'info'} />
            </div>
            <div className="mt-4 text-3xl font-black text-slate-900 dark:text-white">
              {dueReviewItems.length > 0 ? '先把今天到期的复习清掉，再继续新训练。' : '当前没有逾期复习，继续保持节奏。'}
            </div>
            <div className="mt-3 text-sm leading-7 text-slate-600 dark:text-white/65">
              错题本共 {wrongBookQuery.data?.length || 0} 组，待复习计划 {reviewScheduleQuery.data?.length || 0} 项。
              {nextDueReviewItem ? ` 当前最该处理的是 ${nextDueReviewItem.englishWord} / ${nextDueReviewItem.frenchWord}。` : ''}
            </div>
          </div>
          <div className="flex flex-wrap gap-3">
            {nextDueReviewItem ? (
              <button
                type="button"
                onClick={() =>
                  navigate(
                    buildTrainingHref({
                      mode: nextDueReviewItem.reviewMode,
                      source: 'dashboard-review-reminder',
                      lexicalPairId: nextDueReviewItem.lexicalPairId,
                      wrongBookId: nextDueReviewItem.wrongBookId,
                      reviewScheduleId: nextDueReviewItem.reviewScheduleId,
                    })
                  )
                }
                className="btn-liquid px-5 py-3 text-white"
              >
                处理首个到期复习
              </button>
            ) : null}
            <button type="button" onClick={() => navigate('/errors')} className="rounded-full border border-slate-200/80 px-5 py-3 text-sm font-bold dark:border-white/10">
              查看错题与复习计划
            </button>
          </div>
        </div>
      </section>

      <section className="liquid-glass-panel rounded-[2.8rem] p-8">
        <div className="mb-6 flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <SectionEyebrow>{t('ui.sections.achievementWall')}</SectionEyebrow>
            <div className="mt-3 text-2xl font-black text-slate-900 dark:text-white">
              {t('ui.achievements.summary', {
                count: achievementWall?.unlockedCount ?? 0,
                total: achievementWall?.totalCount ?? 0,
              })}
            </div>
            <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-500 dark:text-white/45">
              {t('ui.achievements.subtitle')}
            </p>
          </div>
          <StatusBadge
            label={t('ui.achievements.unlockedMeta', {
              count: achievementWall?.unlockedCount ?? 0,
              total: achievementWall?.totalCount ?? 0,
            })}
            tone={(achievementWall?.unlockedCount ?? 0) > 0 ? 'success' : 'info'}
          />
        </div>

        {overviewQuery.isLoading ? (
          <div className="text-sm text-slate-500 dark:text-white/45">{t('ui.achievements.loading')}</div>
        ) : !achievementWall?.badges?.length ? (
          <div className="text-sm text-slate-500 dark:text-white/45">{t('ui.achievements.empty')}</div>
        ) : (
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            {achievementWall.badges.map((badge) => {
              const meta = ACHIEVEMENT_META[badge.code] ?? ACHIEVEMENT_META.DIAGNOSIS_FINISHER;
              const Icon = meta.icon;
              const progressPercent = badge.targetValue > 0 ? Math.min(100, Math.round((badge.progressValue / badge.targetValue) * 100)) : 0;
              return (
                <article
                  key={badge.code}
                  className={`rounded-[1.8rem] border p-5 transition-all ${
                    badge.unlocked
                      ? 'border-slate-200/80 bg-white/70 shadow-[0_24px_60px_rgba(15,23,42,0.08)] dark:border-white/10 dark:bg-white/8'
                      : 'border-dashed border-slate-300/90 bg-slate-50/80 dark:border-white/10 dark:bg-white/[0.03]'
                  }`}
                >
                  <div className="flex items-start justify-between gap-4">
                    <div className={`inline-flex rounded-[1.2rem] p-3 ${meta.iconClassName}`}>
                      <Icon size={18} />
                    </div>
                    <StatusBadge label={badge.unlocked ? t('ui.achievements.unlocked') : t('ui.achievements.inProgress')} tone={badge.unlocked ? 'success' : 'warning'} />
                  </div>

                  <div className="mt-5">
                    <div className="text-lg font-black text-slate-900 dark:text-white">
                      {t(`ui.achievements.badges.${badge.code}.title`)}
                    </div>
                    <p className="mt-2 min-h-[66px] text-sm leading-6 text-slate-500 dark:text-white/45">
                      {t(`ui.achievements.badges.${badge.code}.description`)}
                    </p>
                  </div>

                  <div className="mt-5">
                    <div className="mb-2 flex items-center justify-between gap-4 text-xs text-slate-400 dark:text-white/35">
                      <span>{resolveAchievementProgressLabel(badge, t)}</span>
                      <span>{progressPercent}%</span>
                    </div>
                    <div className="h-2 overflow-hidden rounded-full bg-slate-200/80 dark:bg-white/10">
                      <div className={`h-full rounded-full bg-gradient-to-r ${meta.progressClassName}`} style={{ width: `${progressPercent}%` }} />
                    </div>
                  </div>

                  <div className="mt-4 text-xs text-slate-400 dark:text-white/35">
                    {badge.unlocked && badge.awardedAt
                      ? t('ui.achievements.awardedAt', { time: formatDateTime(badge.awardedAt) })
                      : t('ui.achievements.nextMilestone')}
                  </div>
                </article>
              );
            })}
          </div>
        )}
      </section>

      <section data-onboarding="student-dashboard-learning-goals" className="liquid-glass-panel rounded-[2.8rem] p-8">
        <div className="mb-6 flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <SectionEyebrow>{t('ui.sections.learningGoals')}</SectionEyebrow>
            <div className="mt-3 text-2xl font-black text-slate-900 dark:text-white">{t('ui.labels.learningGoalsTitle')}</div>
            <p className="mt-2 max-w-3xl text-sm leading-6 text-slate-500 dark:text-white/45">{t('ui.labels.learningGoalsDescription')}</p>
          </div>
          <div className="text-sm text-slate-400 dark:text-white/35">
            {learningGoal?.updatedAt ? t('ui.meta.goalUpdatedAt', { time: formatDateTime(learningGoal.updatedAt) }) : t('ui.labels.goalOptionalHint')}
          </div>
        </div>

        <div className="grid gap-8 xl:grid-cols-[1.1fr_0.9fr]">
          <div className="grid gap-4 md:grid-cols-2">
            <article className="rounded-[1.8rem] border border-slate-200/80 bg-white/70 p-5 dark:border-white/10 dark:bg-white/5">
              <SectionEyebrow className="text-xs">{t('ui.fields.dailyTrainingGoal')}</SectionEyebrow>
              <div className="mt-3 flex items-end justify-between gap-4">
                <div>
                  <div className="text-3xl font-black text-slate-900 dark:text-white">
                    {learningGoal?.dailyTrainingCompletedToday ?? 0}
                    <span className="ml-2 text-base font-bold text-slate-400 dark:text-white/35">
                      / {learningGoal?.dailyTrainingTarget ?? '--'}
                    </span>
                  </div>
                  <div className="mt-2 text-sm text-slate-500 dark:text-white/45">{t('ui.fields.todayTrainingCount')}</div>
                </div>
                <StatusBadge
                  label={dailyGoalStatus}
                  tone={!learningGoal?.dailyTrainingTarget ? 'info' : learningGoal.dailyTrainingRemaining === 0 ? 'success' : 'warning'}
                />
              </div>
              <div className="mt-5">
                <div className="mb-2 flex items-center justify-between text-xs text-slate-400 dark:text-white/35">
                  <span>{t('ui.labels.goalProgress')}</span>
                  <span>{dailyGoalProgress}%</span>
                </div>
                <div className="h-2 overflow-hidden rounded-full bg-slate-200/80 dark:bg-white/10">
                  <div className="h-full rounded-full bg-gradient-to-r from-cyan-400 via-sky-500 to-blue-500" style={{ width: `${dailyGoalProgress}%` }} />
                </div>
              </div>
            </article>

            <article className="rounded-[1.8rem] border border-slate-200/80 bg-white/70 p-5 dark:border-white/10 dark:bg-white/5">
              <SectionEyebrow className="text-xs">{t('ui.fields.weeklyAccuracyGoal')}</SectionEyebrow>
              <div className="mt-3 flex items-end justify-between gap-4">
                <div>
                  <div className="text-3xl font-black text-slate-900 dark:text-white">
                    {formatGoalPercent(learningGoal?.weeklyAccuracyCurrent)}
                    <span className="ml-2 text-base font-bold text-slate-400 dark:text-white/35">
                      / {learningGoal?.weeklyAccuracyTarget ? `${learningGoal.weeklyAccuracyTarget}%` : '--'}
                    </span>
                  </div>
                  <div className="mt-2 text-sm text-slate-500 dark:text-white/45">{t('ui.fields.weeklyAccuracyCurrent')}</div>
                </div>
                <StatusBadge
                  label={weeklyGoalStatus}
                  tone={!learningGoal?.weeklyAccuracyTarget ? 'info' : (learningGoal.weeklyAccuracyDelta ?? 0) >= 0 ? 'success' : 'warning'}
                />
              </div>
              <div className="mt-5">
                <div className="mb-2 flex items-center justify-between text-xs text-slate-400 dark:text-white/35">
                  <span>{t('ui.labels.goalProgress')}</span>
                  <span>{weeklyGoalProgress}%</span>
                </div>
                <div className="h-2 overflow-hidden rounded-full bg-slate-200/80 dark:bg-white/10">
                  <div className="h-full rounded-full bg-gradient-to-r from-emerald-400 via-teal-500 to-cyan-500" style={{ width: `${weeklyGoalProgress}%` }} />
                </div>
              </div>
            </article>

            {!learningGoal?.configured && !overviewQuery.isLoading && (
              <div className="rounded-[1.8rem] border border-dashed border-slate-300 bg-white/60 p-5 text-sm text-slate-500 dark:border-white/10 dark:bg-white/[0.03] dark:text-white/45 md:col-span-2">
                {t('ui.labels.learningGoalsEmpty')}
              </div>
            )}
          </div>

          <form className="rounded-[1.8rem] border border-slate-200/80 bg-white/70 p-6 dark:border-white/10 dark:bg-white/5" onSubmit={handleLearningGoalSubmit}>
            <div className="space-y-5">
              <label className="block">
                <div className="mb-2 text-sm font-bold text-slate-700 dark:text-white/70">{t('ui.fields.dailyTrainingGoal')}</div>
                <input
                  type="number"
                  min={1}
                  max={500}
                  step={1}
                  value={dailyTrainingTargetInput}
                  onChange={(event) => {
                    setIsGoalDirty(true);
                    setGoalErrorMessage(null);
                    setGoalSuccessMessage(null);
                    setDailyTrainingTargetInput(event.target.value);
                  }}
                  placeholder={t('ui.placeholders.dailyTrainingGoal')}
                  className="w-full rounded-2xl border border-slate-200 bg-white/75 px-4 py-3 outline-none focus:border-primary/50 dark:border-white/10 dark:bg-slate-950/40"
                />
              </label>

              <label className="block">
                <div className="mb-2 text-sm font-bold text-slate-700 dark:text-white/70">{t('ui.fields.weeklyAccuracyGoal')}</div>
                <input
                  type="number"
                  min={1}
                  max={100}
                  step={1}
                  value={weeklyAccuracyTargetInput}
                  onChange={(event) => {
                    setIsGoalDirty(true);
                    setGoalErrorMessage(null);
                    setGoalSuccessMessage(null);
                    setWeeklyAccuracyTargetInput(event.target.value);
                  }}
                  placeholder={t('ui.placeholders.weeklyAccuracyGoal')}
                  className="w-full rounded-2xl border border-slate-200 bg-white/75 px-4 py-3 outline-none focus:border-primary/50 dark:border-white/10 dark:bg-slate-950/40"
                />
              </label>

              <div className="text-sm leading-6 text-slate-500 dark:text-white/45">{t('ui.labels.goalOptionalHint')}</div>

              <div className="flex flex-wrap items-center gap-3 pt-2">
                <button type="submit" disabled={learningGoalMutation.isPending} className="btn-liquid px-6 py-3 text-white disabled:opacity-60">
                  {learningGoalMutation.isPending ? t('ui.actions.savingGoals') : t('ui.actions.saveGoals')}
                </button>
                {goalSuccessMessage && (
                  <div className="rounded-2xl border border-emerald-500/20 bg-emerald-500/5 px-4 py-3 text-sm text-emerald-600 dark:text-emerald-400">
                    {goalSuccessMessage}
                  </div>
                )}
                {goalErrorMessage && (
                  <div className="rounded-2xl border border-rose-500/20 bg-rose-500/5 px-4 py-3 text-sm text-rose-500">
                    {goalErrorMessage}
                  </div>
                )}
              </div>
            </div>
          </form>
        </div>
      </section>

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

        <section data-onboarding="student-dashboard-recommended-plan" className="liquid-glass-panel rounded-[2.5rem] p-8">
          <SectionEyebrow className="mb-4">{t('ui.sections.recommendedPlan')}</SectionEyebrow>
          {recommendedPlanQuery.data ? (
            <div className="space-y-4">
              <TrainingModeSummaryCard mode={recommendedPlanQuery.data.priorityMode} />
              <p className="text-sm leading-6 text-slate-500 dark:text-white/45">{recommendedPlanQuery.data.recommendationReason}</p>
              <div className="space-y-3">
                {recommendedPlanQuery.data.suggestedSessions.map((session) => (
                  <TrainingModeSummaryCard
                    key={session.mode}
                    onClick={() =>
                      navigate(
                        buildTrainingHref({
                          mode: session.mode,
                          source: 'dashboard',
                          diagnosisSummaryId: recommendedPlanQuery.data?.sourceDiagnosisSummaryId,
                        })
                      )
                    }
                    mode={session.mode}
                    count={session.count}
                    className="w-full"
                  />
                ))}
              </div>
            </div>
          ) : planError?.status === 409 ? (
            <div className="space-y-3">
              <div className="text-lg font-black text-slate-900 dark:text-white">{t('ui.labels.noPlanTitle')}</div>
              <p className="text-sm leading-6 text-slate-500 dark:text-white/45">{t('ui.labels.noPlanDescription')}</p>
              <button type="button" onClick={() => navigate(buildDiagnosisHref({ source: 'dashboard-no-plan' }))} className="btn-liquid px-5 py-3 text-white">
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
            <div className="flex items-center gap-3">
              <SectionEyebrow>{t('ui.sections.wrongBook')}</SectionEyebrow>
              <StatusBadge label={`${wrongBookQuery.data?.length || 0}`} tone="info" />
            </div>
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

        <section data-onboarding="student-dashboard-review-schedule" className="liquid-glass-panel rounded-[2.5rem] p-8">
          <div className="mb-6 flex items-center justify-between gap-3">
            <div className="flex items-center gap-3">
              <Brain size={16} className="text-primary" />
              <SectionEyebrow>{t('ui.sections.reviewSchedule')}</SectionEyebrow>
              <StatusBadge label={`${dueReviewItems.length}`} tone={dueReviewItems.length > 0 ? 'warning' : 'info'} />
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

      <OnboardingTour
        open={studentOnboarding.isOpen}
        steps={onboardingSteps}
        onComplete={studentOnboarding.complete}
      />
    </div>
  );
};

export default DashboardPage;
