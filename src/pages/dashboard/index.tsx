import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import type { EChartsOption } from 'echarts';
import { AlertTriangle, ArrowRight, Brain, Clock3, RefreshCw, Target } from 'lucide-react';
import { ChartCard } from '@/components/common/ChartCard';
import { PageHeader, StatCard } from '@/components/common';
import { studentService, trainingService } from '@/lib/services';
import { buildRadarOption, buildTrendOption, formatDateTime, formatMaybePercent, formatMs, lexicalPairTypeLabel } from '@/lib/format';
import { normalizeApiError } from '@/lib/api';

const DashboardPage: React.FC = () => {
  const navigate = useNavigate();

  const overviewQuery = useQuery({
    queryKey: ['student-overview'],
    queryFn: () => studentService.getOverview(),
  });
  const trendsQuery = useQuery({
    queryKey: ['student-trends', '7d'],
    queryFn: () => studentService.getTrends('7d'),
  });
  const highRiskPairsQuery = useQuery({
    queryKey: ['student-high-risk-pairs', '30d', 5],
    queryFn: () => studentService.getHighRiskPairs('30d', 5),
  });
  const errorDistributionQuery = useQuery({
    queryKey: ['student-error-distribution', '30d'],
    queryFn: () => studentService.getErrorDistribution('30d'),
  });
  const recommendedPlanQuery = useQuery({
    queryKey: ['recommended-training-plan'],
    queryFn: () => trainingService.getRecommendedPlan(),
    retry: false,
  });
  const wrongBookQuery = useQuery({
    queryKey: ['wrong-book'],
    queryFn: () => trainingService.getWrongBook(),
  });
  const reviewScheduleQuery = useQuery({
    queryKey: ['review-schedule', true],
    queryFn: () => trainingService.getReviewSchedule(true),
  });

  const overview = overviewQuery.data;
  const topCards = overview?.cards.slice(0, 4) ?? [];
  const errorDistributionOption = {
    tooltip: { trigger: 'item' },
    series: [
      {
        type: 'pie',
        radius: ['48%', '76%'],
        data: (errorDistributionQuery.data || []).map((item) => ({ name: item.label, value: item.count })),
      },
    ],
  } as EChartsOption;

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
          {overviewQuery.error.message}
        </div>
      )}

      <section className="liquid-glass-panel rounded-[3rem] p-10 edge-light">
        <div className="flex flex-col lg:flex-row gap-8 items-start justify-between">
          <div className="max-w-3xl">
            <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">student profile</div>
            <h1 className="mt-3 text-4xl md:text-5xl font-black tracking-tight text-slate-900 dark:text-white">
              {overview?.studentName || '加载中'}，当前主风险为 {overview?.primaryRiskLevel || '--'}
            </h1>
            <p className="mt-4 text-slate-500 dark:text-white/50 leading-7">
              推荐训练模式：{overview?.recommendedTrainingMode || '--'}。最近活跃时间 {formatDateTime(overview?.latestSnapshot.lastActiveAt)}，待复习词对{' '}
              {overview?.latestSnapshot.pendingReviewCount ?? 0} 组。
            </p>
          </div>
          <div className="grid sm:grid-cols-2 gap-4 min-w-[280px]">
            <div className="rounded-[2rem] border border-slate-200/80 dark:border-white/10 p-5 bg-white/60 dark:bg-white/5">
              <div className="text-xs uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">英语 / 法语</div>
              <div className="mt-3 text-2xl font-black text-slate-900 dark:text-white">
                {overview?.englishLevel || '--'} / {overview?.frenchLevel || '--'}
              </div>
            </div>
            <div className="rounded-[2rem] border border-slate-200/80 dark:border-white/10 p-5 bg-white/60 dark:bg-white/5">
              <div className="text-xs uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">平均反应时</div>
              <div className="mt-3 text-2xl font-black text-slate-900 dark:text-white">
                {formatMs(overview?.latestSnapshot.recentAvgReactionTimeMs)}
              </div>
            </div>
          </div>
        </div>
      </section>

      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-6">
        {topCards.map((card) => (
          <StatCard
            key={card.key}
            title={card.label}
            value={`${card.value.toFixed(card.unit === '%' ? 0 : 0)}${card.unit || ''}`}
            icon={card.key.includes('risk') ? AlertTriangle : card.key.includes('latency') ? Clock3 : card.key.includes('context') ? RefreshCw : Target}
          />
        ))}
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-[1.3fr_0.7fr] gap-8">
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

      <div className="grid grid-cols-1 xl:grid-cols-[1fr_1fr_0.9fr] gap-8">
        <ChartCard
          title="错误分布"
          option={errorDistributionOption}
          loading={errorDistributionQuery.isLoading}
          isEmpty={!errorDistributionQuery.data?.length}
        />

        <section className="liquid-glass-panel rounded-[2.5rem] p-8">
          <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30 mb-6">high risk pairs</div>
          <div className="space-y-4">
            {(highRiskPairsQuery.data || []).map((item) => (
              <div key={item.lexicalPairId} className="rounded-[1.5rem] border border-slate-200/70 dark:border-white/10 p-4 bg-white/60 dark:bg-white/5">
                <div className="flex items-center justify-between gap-4">
                  <div>
                    <div className="font-black text-slate-900 dark:text-white">{item.englishWord} / {item.frenchWord}</div>
                    <div className="text-sm text-slate-500 dark:text-white/45 mt-1">{lexicalPairTypeLabel(item.lexicalPairType)}</div>
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
          <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30 mb-4">recommended plan</div>
          {recommendedPlanQuery.data ? (
            <div className="space-y-4">
              <div className="text-2xl font-black text-slate-900 dark:text-white">{recommendedPlanQuery.data.priorityMode}</div>
              <p className="text-sm leading-6 text-slate-500 dark:text-white/45">{recommendedPlanQuery.data.recommendationReason}</p>
              <div className="space-y-3">
                {recommendedPlanQuery.data.suggestedSessions.map((session) => (
                  <button
                    key={session.mode}
                    type="button"
                    onClick={() => navigate('/training')}
                    className="w-full text-left rounded-[1.4rem] border border-slate-200/70 dark:border-white/10 p-4 hover:border-primary/40 transition-all"
                  >
                    <div className="font-bold text-slate-900 dark:text-white">{session.label}</div>
                    <div className="text-sm text-slate-500 dark:text-white/45 mt-1">建议题量 {session.count}</div>
                  </button>
                ))}
              </div>
            </div>
          ) : planError?.status === 409 ? (
            <div className="space-y-3">
              <div className="text-lg font-black text-slate-900 dark:text-white">尚未生成训练计划</div>
              <p className="text-sm leading-6 text-slate-500 dark:text-white/45">先完成一轮诊断，系统才能基于最新 summary 生成训练计划。</p>
            </div>
          ) : recommendedPlanQuery.isLoading ? (
            <div className="text-sm text-slate-500 dark:text-white/45">正在拉取推荐计划...</div>
          ) : (
            <div className="text-sm text-rose-500">{planError?.message || '推荐计划加载失败'}</div>
          )}
        </section>
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-2 gap-8">
        <section className="liquid-glass-panel rounded-[2.5rem] p-8">
          <div className="flex items-center justify-between mb-6">
            <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">wrong book</div>
            <button type="button" onClick={() => navigate('/errors')} className="text-sm text-primary flex items-center gap-2">
              查看全部 <ArrowRight size={14} />
            </button>
          </div>
          <div className="space-y-4">
            {(wrongBookQuery.data || []).slice(0, 4).map((item) => (
              <div key={item.wrongBookId} className="rounded-[1.4rem] border border-slate-200/70 dark:border-white/10 p-4 bg-white/60 dark:bg-white/5">
                <div className="font-bold text-slate-900 dark:text-white">{item.englishWord} / {item.frenchWord}</div>
                <div className="text-sm text-slate-500 dark:text-white/45 mt-2">
                  最近错误：{item.lastErrorType}，累计 {item.wrongCount} 次
                </div>
              </div>
            ))}
            {!wrongBookQuery.isLoading && !wrongBookQuery.data?.length && (
              <div className="text-sm text-slate-500 dark:text-white/45">当前没有错题记录。</div>
            )}
          </div>
        </section>

        <section className="liquid-glass-panel rounded-[2.5rem] p-8">
          <div className="flex items-center gap-3 mb-6">
            <Brain size={16} className="text-primary" />
            <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">review schedule</div>
          </div>
          <div className="space-y-4">
            {(reviewScheduleQuery.data || []).slice(0, 4).map((item) => (
              <div key={item.reviewScheduleId} className="rounded-[1.4rem] border border-slate-200/70 dark:border-white/10 p-4 bg-white/60 dark:bg-white/5">
                <div className="flex items-center justify-between gap-4">
                  <div>
                    <div className="font-bold text-slate-900 dark:text-white">{item.englishWord} / {item.frenchWord}</div>
                    <div className="text-sm text-slate-500 dark:text-white/45 mt-2">
                      {item.reviewMode} · 第 {item.scheduleStage} 阶段
                    </div>
                  </div>
                  <div className="text-sm text-slate-500 dark:text-white/45">{formatDateTime(item.dueAt)}</div>
                </div>
              </div>
            ))}
            {!reviewScheduleQuery.isLoading && !reviewScheduleQuery.data?.length && (
              <div className="text-sm text-slate-500 dark:text-white/45">当前没有待复习项目。</div>
            )}
          </div>
        </section>
      </div>
    </div>
  );
};

export default DashboardPage;
