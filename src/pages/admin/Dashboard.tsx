import React from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Activity,
  Bot,
  Brain,
  Database,
  FileCheck2,
  GraduationCap,
  LayoutDashboard,
  Settings2,
  TriangleAlert,
  Users,
} from 'lucide-react';
import { Link } from 'react-router-dom';
import { ChartCard } from '@/components/common/ChartCard';
import { PageHeader, SectionEyebrow, StatCard, StatusBadge } from '@/components/common';
import { OnboardingTour, useOnboardingTour } from '@/features/onboarding';
import { getApiErrorMessage } from '@/lib/api';
import type { AppChartOption } from '@/lib/echarts';
import { formatDateTime } from '@/lib/format';
import { adminService } from '@/lib/services';
import { useAuthStore } from '@/store';

const integerFormatter = new Intl.NumberFormat('zh-CN');
const percentFormatter = new Intl.NumberFormat('zh-CN', {
  style: 'percent',
  minimumFractionDigits: 1,
  maximumFractionDigits: 1,
});

function formatCompactDate(value: string): string {
  return new Date(`${value}T00:00:00`).toLocaleDateString('zh-CN', {
    month: 'numeric',
    day: 'numeric',
  });
}

function formatSceneLabel(scene: string): string {
  switch (scene) {
    case 'RECOMMEND_TRAINING':
      return '训练推荐';
    case 'EXPLAIN_DIAGNOSIS':
      return '诊断解读';
    case 'TEACHER_INTERVENTION':
      return '教师干预';
    case 'LEXICAL_RAG_QUERY':
      return '词汇追问';
    default:
      return scene.replaceAll('_', ' ');
  }
}

function buildRegistrationOption(data: Array<{ date: string; registrations: number }>): AppChartOption {
  return {
    color: ['#0f766e'],
    tooltip: { trigger: 'axis' },
    grid: { top: 24, right: 18, bottom: 24, left: 18, containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: data.map((item) => formatCompactDate(item.date)),
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
    },
    series: [
      {
        name: '注册用户',
        type: 'line',
        smooth: true,
        symbolSize: 7,
        lineStyle: { width: 3 },
        areaStyle: {
          color: 'rgba(15, 118, 110, 0.12)',
        },
        data: data.map((item) => item.registrations),
      },
    ],
  };
}

function buildCompletionOption(
  data: Array<{ date: string; diagnosisCompleted: number; trainingCompleted: number; assessmentCompleted: number }>
): AppChartOption {
  return {
    color: ['#0f766e', '#2563eb', '#d97706'],
    tooltip: { trigger: 'axis' },
    legend: {
      top: 0,
      data: ['诊断', '训练', '考核'],
    },
    grid: { top: 48, right: 18, bottom: 24, left: 18, containLabel: true },
    xAxis: {
      type: 'category',
      data: data.map((item) => formatCompactDate(item.date)),
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
    },
    series: [
      {
        name: '诊断',
        type: 'bar',
        barMaxWidth: 22,
        data: data.map((item) => item.diagnosisCompleted),
      },
      {
        name: '训练',
        type: 'bar',
        barMaxWidth: 22,
        data: data.map((item) => item.trainingCompleted),
      },
      {
        name: '考核',
        type: 'bar',
        barMaxWidth: 22,
        data: data.map((item) => item.assessmentCompleted),
      },
    ],
  };
}

function buildAiOption(data: Array<{ date: string; totalCalls: number; fallbackRate: number }>): AppChartOption {
  return {
    color: ['#1d4ed8', '#e11d48'],
    tooltip: { trigger: 'axis' },
    legend: {
      top: 0,
      data: ['AI 调用量', '错误/回退率'],
    },
    grid: { top: 48, right: 22, bottom: 24, left: 18, containLabel: true },
    xAxis: {
      type: 'category',
      data: data.map((item) => formatCompactDate(item.date)),
    },
    yAxis: [
      {
        type: 'value',
        minInterval: 1,
      },
      {
        type: 'value',
        min: 0,
        max: 1,
        splitLine: { show: false },
        axisLabel: {
          formatter: (value: number) => `${Math.round(value * 100)}%`,
        },
      },
    ],
    series: [
      {
        name: 'AI 调用量',
        type: 'bar',
        barMaxWidth: 26,
        data: data.map((item) => item.totalCalls),
      },
      {
        name: '错误/回退率',
        type: 'line',
        smooth: true,
        yAxisIndex: 1,
        symbolSize: 7,
        lineStyle: { width: 3 },
        data: data.map((item) => item.fallbackRate),
      },
    ],
  };
}

function buildSceneOption(data: Array<{ scene: string; count: number }>): AppChartOption {
  return {
    tooltip: { trigger: 'item' },
    series: [
      {
        type: 'pie',
        radius: ['42%', '76%'],
        itemStyle: {
          borderRadius: 10,
          borderColor: 'transparent',
          borderWidth: 4,
        },
        data: data.map((item) => ({
          name: formatSceneLabel(item.scene),
          value: item.count,
        })),
      },
    ],
  };
}

const AdminDashboardPage: React.FC = () => {
  const user = useAuthStore((state) => state.user);
  const dashboardQuery = useQuery({
    queryKey: ['admin-dashboard'],
    queryFn: ({ signal }) => adminService.getDashboard({ signal }),
  });
  const adminOnboarding = useOnboardingTour({
    tourId: 'admin-dashboard',
    userId: user?.id,
  });

  const overview = dashboardQuery.data?.overview;
  const registrationTrend = dashboardQuery.data?.registrationTrend ?? [];
  const completionTrend = dashboardQuery.data?.completionTrend ?? [];
  const aiTrend = dashboardQuery.data?.aiTrend ?? [];
  const aiSceneDistribution = dashboardQuery.data?.aiSceneDistribution ?? [];

  const statCards = React.useMemo(
    () => [
      { title: '总用户数', value: integerFormatter.format(overview?.totalUsers ?? 0), icon: Users, color: 'text-slate-700' },
      { title: 'DAU', value: integerFormatter.format(overview?.dailyActiveUsers ?? 0), icon: Activity, color: 'text-emerald-600' },
      { title: 'WAU', value: integerFormatter.format(overview?.weeklyActiveUsers ?? 0), icon: LayoutDashboard, color: 'text-blue-600' },
      { title: '近 30 天 AI 调用', value: integerFormatter.format(overview?.aiCallsLast30Days ?? 0), icon: Bot, color: 'text-ai' },
      { title: '近 30 天诊断完成', value: integerFormatter.format(overview?.diagnosisCompletedLast30Days ?? 0), icon: Brain, color: 'text-teal-600' },
      { title: '近 30 天训练完成', value: integerFormatter.format(overview?.trainingCompletedLast30Days ?? 0), icon: GraduationCap, color: 'text-sky-600' },
      { title: '近 30 天考核提交', value: integerFormatter.format(overview?.assessmentCompletedLast30Days ?? 0), icon: FileCheck2, color: 'text-amber-600' },
      { title: '近 30 天错误/回退率', value: percentFormatter.format(overview?.aiFallbackRateLast30Days ?? 0), icon: TriangleAlert, color: 'text-rose-600' },
    ],
    [overview]
  );
  const onboardingSteps = React.useMemo(
    () => [
      {
        id: 'header-actions',
        selector: '[data-onboarding="admin-dashboard-actions"]',
        title: '后台入口导航',
        description: '首页顶部保留了管理员最常用的三个操作入口，先从用户管理、语料库管理和配置中心进入核心维护流程。',
        placement: 'bottom' as const,
      },
      {
        id: 'overview',
        selector: '[data-onboarding="admin-dashboard-overview"]',
        title: '全局运行概览',
        description: '这一块先看平台总体体量和最近刷新时间，方便你判断当前数据是否足够新、是否需要继续排查异常。',
        placement: 'bottom' as const,
      },
      {
        id: 'stats',
        selector: '[data-onboarding="admin-dashboard-stats"]',
        title: '关键指标卡片',
        description: '这里汇总了用户活跃、学习闭环完成量和 AI 调用情况，适合快速筛出是否是增长问题、教学问题还是系统稳定性问题。',
        placement: 'bottom' as const,
      },
      {
        id: 'ai-observability',
        selector: '[data-onboarding="admin-dashboard-ai"]',
        title: 'AI 监控区域',
        description: '最后看 AI 调用量、错误率和场景分布。若回退率上升，优先联动配置中心检查模型、密钥和限流配置。',
        placement: 'top' as const,
      },
    ],
    []
  );

  return (
    <div className="space-y-10 pb-20">
      <PageHeader
        eyebrow="ADMIN CONSOLE"
        title="管理员仪表盘"
        subtitle="统一查看用户增长、登录活跃、学习闭环完成量，以及 AI 调用与回退情况。"
        actions={
          <div data-onboarding="admin-dashboard-actions" className="flex flex-wrap gap-3">
            <Link
              to="/admin/users"
              className="inline-flex items-center gap-2 rounded-full border border-slate-200 px-4 py-3 text-sm dark:border-white/10"
            >
              <Users size={14} />
              用户管理
            </Link>
            <Link
              to="/admin/lexical-pairs"
              className="inline-flex items-center gap-2 rounded-full border border-slate-200 px-4 py-3 text-sm dark:border-white/10"
            >
              <Database size={14} />
              语料库管理
            </Link>
            <Link
              to="/admin/config-center"
              className="inline-flex items-center gap-2 rounded-full border border-slate-200 px-4 py-3 text-sm dark:border-white/10"
            >
              <Settings2 size={14} />
              配置中心
            </Link>
          </div>
        }
      />

      {dashboardQuery.error && (
        <div className="rounded-[1.8rem] border border-rose-500/20 bg-rose-500/5 px-5 py-4 text-sm text-rose-500">
          {getApiErrorMessage(dashboardQuery.error, '读取管理员仪表盘失败')}
        </div>
      )}

      <section data-onboarding="admin-dashboard-overview" className="liquid-glass-panel rounded-[3rem] p-10 edge-light">
        <div className="flex flex-col gap-8 xl:flex-row xl:items-end xl:justify-between">
          <div className="max-w-3xl">
            <SectionEyebrow>全局概览</SectionEyebrow>
            <h1 className="mt-3 text-4xl font-black tracking-tight text-slate-900 dark:text-white md:text-5xl">
              {integerFormatter.format(overview?.totalUsers ?? 0)} 名用户，{integerFormatter.format(overview?.enabledUsers ?? 0)} 名可用账号
            </h1>
            <p className="mt-4 leading-7 text-slate-500 dark:text-white/50">
              注册趋势按用户 `created_at` 统计，DAU/WAU 按最近登录时间计算；错误率基于 AI 调用中的规则回退或 fallback 记录。
            </p>
          </div>
          <div className="flex flex-wrap gap-3">
            <StatusBadge label={`近 30 天新增 ${integerFormatter.format(overview?.registrationsLast30Days ?? 0)}`} tone="info" />
            <StatusBadge
              label={`最近刷新 ${overview?.generatedAt ? formatDateTime(overview.generatedAt) : '--'}`}
              tone="neutral"
            />
          </div>
        </div>
      </section>

      <div data-onboarding="admin-dashboard-stats" className="grid grid-cols-1 gap-6 md:grid-cols-2 xl:grid-cols-4">
        {statCards.map((card) => (
          <StatCard key={card.title} title={card.title} value={card.value} icon={card.icon} color={card.color} />
        ))}
      </div>

      <div className="grid grid-cols-1 gap-8 xl:grid-cols-2">
        <ChartCard
          title="注册用户趋势"
          option={buildRegistrationOption(registrationTrend)}
          loading={dashboardQuery.isLoading}
          error={dashboardQuery.error}
          isEmpty={!registrationTrend.some((item) => item.registrations > 0)}
          extra={<StatusBadge label="最近 14 天" tone="info" />}
        />
        <ChartCard
          title="诊断 / 训练 / 考核完成量"
          option={buildCompletionOption(completionTrend)}
          loading={dashboardQuery.isLoading}
          error={dashboardQuery.error}
          isEmpty={!completionTrend.some((item) => item.diagnosisCompleted + item.trainingCompleted + item.assessmentCompleted > 0)}
          extra={<StatusBadge label="最近 14 天" tone="success" />}
        />
      </div>

      <div data-onboarding="admin-dashboard-ai" className="grid grid-cols-1 gap-8 xl:grid-cols-[1.2fr_0.8fr]">
        <ChartCard
          title="AI 调用量与错误率"
          option={buildAiOption(aiTrend)}
          loading={dashboardQuery.isLoading}
          error={dashboardQuery.error}
          isEmpty={!aiTrend.some((item) => item.totalCalls > 0)}
          extra={<StatusBadge label="最近 14 天" tone="warning" />}
        />

        <section className="liquid-glass-panel rounded-[2.5rem] p-8">
          <div className="flex items-center justify-between gap-3">
            <div>
              <SectionEyebrow>AI 场景分布</SectionEyebrow>
              <h2 className="mt-3 text-2xl font-black text-slate-900 dark:text-white">最近 30 天调用构成</h2>
            </div>
            <StatusBadge label="最近 30 天" tone="neutral" />
          </div>

          {!dashboardQuery.isLoading && !dashboardQuery.error && !aiSceneDistribution.length ? (
            <div className="mt-6 flex min-h-72 flex-col items-center justify-center rounded-[2rem] border border-slate-200/80 bg-white/65 px-6 py-10 text-center dark:border-white/10 dark:bg-white/5">
              <StatusBadge label="暂无内容" tone="neutral" />
              <h3 className="mt-5 text-xl font-black tracking-tight text-slate-900 dark:text-white/85">当前暂无可展示的 AI 场景分布</h3>
              <p className="mt-3 max-w-md text-sm leading-6 text-slate-500 dark:text-white/45">
                系统已经完成本次查询，但还没有足够的数据生成图表。
              </p>
            </div>
          ) : (
            <div className="mt-6 grid gap-6 lg:grid-cols-[minmax(0,1fr)_240px] lg:items-center">
              <ChartCard
                title="AI 场景分布"
                option={buildSceneOption(aiSceneDistribution)}
                loading={dashboardQuery.isLoading}
                error={dashboardQuery.error}
                embedded
                height={320}
              />

              <div className="space-y-3">
                {aiSceneDistribution.map((item) => (
                  <div
                    key={item.scene}
                    className="rounded-[1.5rem] border border-slate-200/80 bg-white/70 px-4 py-4 dark:border-white/10 dark:bg-white/5"
                  >
                    <div className="font-bold text-slate-900 dark:text-white">{formatSceneLabel(item.scene)}</div>
                    <div className="mt-1 text-sm text-slate-500 dark:text-white/45">
                      {integerFormatter.format(item.count)} 次调用
                    </div>
                    <div className="mt-2 text-xs font-bold text-slate-400 dark:text-white/30">
                      占比 {percentFormatter.format(item.ratio)}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </section>
      </div>

      <OnboardingTour
        open={adminOnboarding.isOpen}
        steps={onboardingSteps}
        onComplete={adminOnboarding.complete}
      />
    </div>
  );
};

export default AdminDashboardPage;
