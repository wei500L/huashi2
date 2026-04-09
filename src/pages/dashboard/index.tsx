import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { AlertTriangle, ArrowRight, Brain, Clock3, FileText, RefreshCw, Target } from 'lucide-react';
import { ChartCard } from '@/components/common/ChartCard';
import { PageHeader, StatCard } from '@/components/common';
import { getApiErrorMessage, normalizeApiError } from '@/lib/api';
import type { AppChartOption } from '@/lib/echarts';
import {
  assessmentAttemptStatusLabel,
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

function resolveAssessmentDashboardAction(item: StudentAssessmentSummaryVO, now: number) {
  const startsAt = item.startsAt ? new Date(item.startsAt).getTime() : null;
  const dueAt = item.dueAt ? new Date(item.dueAt).getTime() : null;
  if (item.attemptStatus === 'SUBMITTED' && item.attemptId) {
    return {
      label: '查看结果',
      disabled: false,
      to: `/assessments/attempts/${item.attemptId}/result`,
    };
  }
  if (item.attemptId) {
    return {
      label: '继续作答',
      disabled: false,
      to: `/assessments/attempts/${item.attemptId}`,
    };
  }
  if (startsAt && startsAt > now) {
    return {
      label: '未开始',
      disabled: true,
      to: '/assessments',
    };
  }
  if (dueAt && dueAt <= now) {
    return {
      label: '已截止',
      disabled: true,
      to: '/assessments',
    };
  }
  return {
    label: '进入测评中心',
    disabled: false,
    to: '/assessments',
  };
}

const DashboardPage: React.FC = () => {
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
        title="学习总览"
        subtitle="实时聚合学生画像、近期诊断信号和训练建议。"
        actions={
          <button onClick={() => navigate('/diagnosis')} className="btn-liquid px-6 py-3 text-white">
            开始新诊断
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
            <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">学生画像</div>
            <h1 className="mt-3 text-4xl font-black tracking-tight text-slate-900 dark:text-white md:text-5xl">
              {overview?.studentName || '加载中'}，当前主风险为 {riskLevelLabel(overview?.primaryRiskLevel)}
            </h1>
            <p className="mt-4 leading-7 text-slate-500 dark:text-white/50">
              推荐训练模式：{trainingModeLabel(overview?.recommendedTrainingMode)}。最近活跃时间 {formatDateTime(overview?.latestSnapshot.lastActiveAt)}，待复习词对{' '}
              {overview?.latestSnapshot.pendingReviewCount ?? 0} 组。
            </p>
          </div>
          <div className="grid min-w-[280px] gap-4 sm:grid-cols-2">
            <div className="rounded-[2rem] border border-slate-200/80 bg-white/60 p-5 dark:border-white/10 dark:bg-white/5">
              <div className="text-xs uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">英语 / 法语</div>
              <div className="mt-3 text-2xl font-black text-slate-900 dark:text-white">
                {overview?.englishLevel || '--'} / {overview?.frenchLevel || '--'}
              </div>
            </div>
            <div className="rounded-[2rem] border border-slate-200/80 bg-white/60 p-5 dark:border-white/10 dark:bg-white/5">
              <div className="text-xs uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">平均反应时</div>
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
          title="近 7 天趋势"
          option={buildTrendOption(trendsQuery.data)}
          loading={trendsQuery.isLoading}
          isEmpty={!trendsQuery.data?.series.length}
        />
        <ChartCard
          title="总体能力雷达"
          option={buildRadarOption(overview?.radar)}
          loading={overviewQuery.isLoading}
          isEmpty={!overview?.radar.length}
        />
      </div>

      <div className="grid grid-cols-1 gap-8 xl:grid-cols-[1fr_1fr_0.9fr]">
        <ChartCard
          title="错误分布"
          option={errorDistributionOption}
          loading={errorDistributionQuery.isLoading}
          isEmpty={!errorDistributionQuery.data?.length}
        />

        <section className="liquid-glass-panel rounded-[2.5rem] p-8">
          <div className="mb-6 text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">高风险词对</div>
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
                    <div className="text-xs text-slate-400 dark:text-white/30">风险</div>
                  </div>
                </div>
              </div>
            ))}
            {!highRiskPairsQuery.isLoading && !highRiskPairsQuery.data?.length && (
              <div className="text-sm text-slate-500 dark:text-white/45">暂无高风险词对。</div>
            )}
          </div>
        </section>

        <section className="liquid-glass-panel rounded-[2.5rem] p-8">
          <div className="mb-4 text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">推荐训练计划</div>
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
                    <div className="mt-1 text-sm text-slate-500 dark:text-white/45">建议题量 {session.count}</div>
                  </button>
                ))}
              </div>
            </div>
          ) : planError?.status === 409 ? (
            <div className="space-y-3">
              <div className="text-lg font-black text-slate-900 dark:text-white">尚未生成训练计划</div>
              <p className="text-sm leading-6 text-slate-500 dark:text-white/45">先完成一轮诊断，系统才能基于最新 summary 生成训练计划。</p>
              <button type="button" onClick={() => navigate('/diagnosis')} className="btn-liquid px-5 py-3 text-white">
                立即去诊断
              </button>
            </div>
          ) : recommendedPlanQuery.isLoading ? (
            <div className="text-sm text-slate-500 dark:text-white/45">正在拉取推荐计划...</div>
          ) : (
            <div className="text-sm text-rose-500">{planError?.message || '推荐计划加载失败'}</div>
          )}
        </section>
      </div>

      <section className="liquid-glass-panel rounded-[2.5rem] p-8">
        <div className="mb-6 flex items-center justify-between gap-4">
          <div>
            <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">通用测评</div>
            <div className="mt-3 text-2xl font-black text-slate-900 dark:text-white">老师发布的整卷任务</div>
            <div className="mt-2 text-sm text-slate-500 dark:text-white/45">这里展示最近需要处理的通用测评，包括继续作答、查看结果和即将开始的整卷任务。</div>
          </div>
          <button type="button" onClick={() => navigate('/assessments')} className="flex items-center gap-2 text-sm font-bold text-primary">
            打开测评中心 <ArrowRight size={14} />
          </button>
        </div>

        {assessmentsQuery.isLoading ? (
          <div className="text-sm text-slate-500 dark:text-white/45">正在加载测评任务...</div>
        ) : assessmentsQuery.error ? (
          <div className="rounded-[1.6rem] border border-rose-500/20 bg-rose-500/5 px-4 py-3 text-sm text-rose-500">
            {getApiErrorMessage(assessmentsQuery.error)}
          </div>
        ) : !assessmentItems.length ? (
          <div className="rounded-[1.6rem] border border-dashed border-slate-300 bg-white/55 px-4 py-5 text-sm text-slate-500 dark:border-white/15 dark:bg-white/[0.02] dark:text-white/45">
            当前没有待处理的通用测评任务。
          </div>
        ) : (
          <div className="grid gap-4 xl:grid-cols-3">
            {assessmentItems.map((item) => {
              const action = resolveAssessmentDashboardAction(item, now);
              return (
                <div key={item.publishId} className="rounded-[1.8rem] border border-slate-200/70 bg-white/60 p-5 dark:border-white/10 dark:bg-white/5">
                  <div className="inline-flex rounded-2xl bg-primary/10 p-3 text-primary">
                    <FileText size={18} />
                  </div>
                  <div className="mt-4 flex items-start justify-between gap-3">
                    <div>
                      <div className="text-[10px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">{item.className}</div>
                      <div className="mt-2 text-xl font-black text-slate-900 dark:text-white">{item.title}</div>
                    </div>
                    <div className="rounded-full border border-slate-200/70 px-3 py-1 text-xs text-slate-500 dark:border-white/10 dark:text-white/45">
                      {item.attemptStatus ? assessmentAttemptStatusLabel(item.attemptStatus) : '待开始'}
                    </div>
                  </div>
                  <div className="mt-3 grid gap-2 text-sm text-slate-500 dark:text-white/45">
                    <div>开始时间：{formatDateTime(item.startsAt)}</div>
                    <div>截止时间：{formatDateTime(item.dueAt)}</div>
                    <div>进度：{item.answeredCount || 0} / {item.questionCount}</div>
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
            <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">错题本</div>
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
                  立即纠错
                </button>
              )}
              <button type="button" onClick={() => navigate('/errors')} className="flex items-center gap-2 text-sm text-primary">
                查看全部 <ArrowRight size={14} />
              </button>
            </div>
          </div>
          {wrongBookQuery.isLoading ? (
            <div className="text-sm text-slate-500 dark:text-white/45">正在加载错题...</div>
          ) : wrongBookQuery.error ? (
            <div className="rounded-[1.6rem] border border-rose-500/20 bg-rose-500/5 px-4 py-3 text-sm text-rose-500">
              {getApiErrorMessage(wrongBookQuery.error)}
            </div>
          ) : !wrongBookQuery.data?.length ? (
            <div className="text-sm text-slate-500 dark:text-white/45">当前没有错题记录。</div>
          ) : (
            <div className="space-y-4">
              {wrongBookQuery.data.slice(0, 4).map((item) => (
                <div key={item.wrongBookId} className="rounded-[1.4rem] border border-slate-200/70 bg-white/60 p-4 dark:border-white/10 dark:bg-white/5">
                  <div className="flex items-start justify-between gap-4">
                    <div>
                      <div className="font-bold text-slate-900 dark:text-white">{item.englishWord} / {item.frenchWord}</div>
                      <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                        {trainingModeLabel(item.recommendedMode)} · 最近错误：{errorTypeLabel(item.lastErrorType)}，累计 {item.wrongCount} 次
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
                      开始
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
              <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">待复习计划</div>
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
                立即开始
              </button>
            )}
          </div>
          {reviewScheduleQuery.isLoading ? (
            <div className="text-sm text-slate-500 dark:text-white/45">正在加载复习计划...</div>
          ) : reviewScheduleQuery.error ? (
            <div className="rounded-[1.6rem] border border-rose-500/20 bg-rose-500/5 px-4 py-3 text-sm text-rose-500">
              {getApiErrorMessage(reviewScheduleQuery.error)}
            </div>
          ) : !reviewScheduleQuery.data?.length ? (
            <div className="text-sm text-slate-500 dark:text-white/45">当前没有待复习项目。</div>
          ) : (
            <div className="space-y-4">
              {reviewScheduleQuery.data.slice(0, 4).map((item) => (
                <div key={item.reviewScheduleId} className="rounded-[1.4rem] border border-slate-200/70 bg-white/60 p-4 dark:border-white/10 dark:bg-white/5">
                  <div className="flex items-center justify-between gap-4">
                    <div>
                      <div className="font-bold text-slate-900 dark:text-white">{item.englishWord} / {item.frenchWord}</div>
                      <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                        {trainingModeLabel(item.reviewMode)} · 第 {item.scheduleStage} 阶段
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
                        开始
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
