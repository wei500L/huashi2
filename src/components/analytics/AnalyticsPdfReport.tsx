import React from 'react';
import type {
  AnalyticsErrorDistributionVO,
  AnalyticsHeatmapVO,
  AnalyticsRiskBucketVO,
  AnalyticsRiskPairVO,
  AnalyticsScatterVO,
  AnalyticsTrendVO,
  ClassAnalyticsOverviewVO,
  ClassCompletionRateVO,
  StudentAnalyticsOverviewVO,
  StudentProfileSummaryVO,
  TeacherClassDetailVO,
} from '@/lib/contracts';
import type { AppChartOption } from '@/lib/echarts';
import { EChart } from '@/components/common/EChart';
import {
  buildHeatmapOption,
  buildRadarOption,
  buildScatterOption,
  buildTrendOption,
  formatDateTime,
  formatMaybePercent,
  formatMs,
  lexicalPairTypeLabel,
  riskLevelLabel,
  trainingModeLabel,
} from '@/lib/format';

const reportShellClassName = 'absolute left-[-10000px] top-0 w-[794px] bg-white text-slate-900';
const reportPageClassName = 'flex h-[1123px] w-[794px] flex-col gap-5 overflow-hidden bg-white px-10 py-9';

type ReportMetricCardProps = {
  label: string;
  value: string;
  hint?: string;
};

type ReportChartCardProps = {
  title: string;
  option: AppChartOption;
  height?: number;
  empty?: boolean;
};

type ReportSectionProps = {
  title: string;
  subtitle?: string;
  children: React.ReactNode;
  className?: string;
};

type ReportPillListProps = {
  items: string[];
  emptyText: string;
};

type StudentAnalyticsPdfReportProps = {
  reportRef: React.RefObject<HTMLDivElement | null>;
  range: '7d' | '30d';
  generatedAt?: string | null;
  overview?: StudentAnalyticsOverviewVO;
  trend?: AnalyticsTrendVO;
  heatmap?: AnalyticsHeatmapVO;
  scatter?: AnalyticsScatterVO;
  highRiskPairs?: AnalyticsRiskPairVO[];
  errorDistribution?: AnalyticsErrorDistributionVO[];
};

type ClassAnalyticsPdfReportProps = {
  reportRef: React.RefObject<HTMLDivElement | null>;
  generatedAt?: string | null;
  detail?: TeacherClassDetailVO;
  overview?: ClassAnalyticsOverviewVO;
  riskDistribution?: AnalyticsRiskBucketVO[];
  heatmap?: AnalyticsHeatmapVO;
  errorDistribution?: AnalyticsErrorDistributionVO[];
  completionRate?: ClassCompletionRateVO;
  students?: StudentProfileSummaryVO[];
};

function buildStaticChartOption(option: AppChartOption): AppChartOption {
  return {
    ...option,
    animation: false,
  };
}

function rangeLabel(range: '7d' | '30d'): string {
  return range === '7d' ? '最近 7 天' : '最近 30 天';
}

function ReportMetricCard({ label, value, hint }: ReportMetricCardProps) {
  return (
    <div className="rounded-[1.5rem] border border-slate-200 bg-slate-50 px-4 py-4">
      <div className="text-[11px] font-bold uppercase tracking-[0.24em] text-slate-500">{label}</div>
      <div className="mt-3 text-2xl font-black text-slate-900">{value}</div>
      {hint ? <div className="mt-2 text-xs leading-5 text-slate-500">{hint}</div> : null}
    </div>
  );
}

function ReportSection({ title, subtitle, children, className = '' }: ReportSectionProps) {
  return (
    <section className={`rounded-[1.7rem] border border-slate-200 bg-white p-5 ${className}`.trim()}>
      <div className="flex items-start justify-between gap-4">
        <div>
          <div className="text-sm font-black text-slate-900">{title}</div>
          {subtitle ? <div className="mt-1 text-xs leading-5 text-slate-500">{subtitle}</div> : null}
        </div>
      </div>
      <div className="mt-4">{children}</div>
    </section>
  );
}

function ReportChartCard({ title, option, height = 220, empty = false }: ReportChartCardProps) {
  return (
    <ReportSection title={title}>
      {empty ? (
        <div
          className="flex items-center justify-center rounded-[1.2rem] border border-dashed border-slate-200 bg-slate-50 text-sm text-slate-400"
          style={{ height }}
        >
          当前暂无图表数据
        </div>
      ) : (
        <div style={{ height }}>
          <EChart option={buildStaticChartOption(option)} theme="light" style={{ height: '100%', width: '100%' }} />
        </div>
      )}
    </ReportSection>
  );
}

function ReportPillList({ items, emptyText }: ReportPillListProps) {
  if (!items.length) {
    return <div className="text-sm text-slate-400">{emptyText}</div>;
  }

  return (
    <div className="flex flex-wrap gap-2">
      {items.map((item) => (
        <span key={item} className="rounded-full bg-slate-100 px-3 py-1.5 text-xs font-semibold text-slate-700">
          {item}
        </span>
      ))}
    </div>
  );
}

function StudentRiskTable({ pairs }: { pairs: AnalyticsRiskPairVO[] }) {
  if (!pairs.length) {
    return <div className="text-sm text-slate-400">当前没有高风险词对。</div>;
  }

  return (
    <div className="overflow-hidden rounded-[1.2rem] border border-slate-200">
      <table className="min-w-full table-fixed border-collapse text-left text-xs text-slate-700">
        <thead className="bg-slate-50 text-slate-500">
          <tr>
            <th className="px-3 py-3 font-semibold">词对</th>
            <th className="px-3 py-3 font-semibold">类型</th>
            <th className="px-3 py-3 font-semibold">风险</th>
            <th className="px-3 py-3 font-semibold">错答 / 尝试</th>
          </tr>
        </thead>
        <tbody>
          {pairs.slice(0, 8).map((pair) => (
            <tr key={pair.lexicalPairId} className="border-t border-slate-200">
              <td className="px-3 py-3 font-semibold text-slate-900">
                {pair.englishWord} / {pair.frenchWord}
              </td>
              <td className="px-3 py-3">{lexicalPairTypeLabel(pair.lexicalPairType)}</td>
              <td className="px-3 py-3 text-rose-600">{formatMaybePercent(pair.riskScore)}</td>
              <td className="px-3 py-3">
                {pair.incorrectCount} / {pair.attemptCount}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function ClassStudentTable({ students }: { students: StudentProfileSummaryVO[] }) {
  if (!students.length) {
    return <div className="text-sm text-slate-400">当前没有学生分析数据。</div>;
  }

  return (
    <div className="overflow-hidden rounded-[1.2rem] border border-slate-200">
      <table className="min-w-full table-fixed border-collapse text-left text-xs text-slate-700">
        <thead className="bg-slate-50 text-slate-500">
          <tr>
            <th className="px-3 py-3 font-semibold">学生</th>
            <th className="px-3 py-3 font-semibold">风险</th>
            <th className="px-3 py-3 font-semibold">正确率</th>
            <th className="px-3 py-3 font-semibold">反应时</th>
            <th className="px-3 py-3 font-semibold">待复习</th>
          </tr>
        </thead>
        <tbody>
          {students
            .slice()
            .sort((left, right) => right.recentNegativeTransferRisk - left.recentNegativeTransferRisk)
            .slice(0, 8)
            .map((student) => (
              <tr key={student.studentUserId} className="border-t border-slate-200">
                <td className="px-3 py-3 font-semibold text-slate-900">{student.studentName}</td>
                <td className="px-3 py-3 text-rose-600">{formatMaybePercent(student.recentNegativeTransferRisk)}</td>
                <td className="px-3 py-3">{formatMaybePercent(student.recentAccuracy)}</td>
                <td className="px-3 py-3">{formatMs(student.recentAvgReactionTimeMs)}</td>
                <td className="px-3 py-3">{student.pendingReviewCount}</td>
              </tr>
            ))}
        </tbody>
      </table>
    </div>
  );
}

function ReportHeader({
  title,
  subtitle,
  generatedAt,
  meta,
}: {
  title: string;
  subtitle: string;
  generatedAt?: string | null;
  meta: string[];
}) {
  return (
    <header className="rounded-[1.9rem] bg-slate-950 px-6 py-6 text-white">
      <div className="flex items-start justify-between gap-6">
        <div>
          <div className="text-[11px] font-bold uppercase tracking-[0.28em] text-sky-200">EF.Transfer Report</div>
          <h1 className="mt-3 text-[28px] font-black tracking-tight">{title}</h1>
          <p className="mt-2 max-w-[520px] text-sm leading-6 text-slate-300">{subtitle}</p>
        </div>
        <div className="rounded-[1.3rem] bg-white/10 px-4 py-3 text-right text-xs leading-6 text-slate-200">
          <div>生成时间</div>
          <div className="font-semibold text-white">{formatDateTime(generatedAt)}</div>
        </div>
      </div>
      <div className="mt-5 flex flex-wrap gap-2">
        {meta.map((item) => (
          <span key={item} className="rounded-full bg-white/10 px-3 py-1.5 text-xs font-semibold text-slate-100">
            {item}
          </span>
        ))}
      </div>
    </header>
  );
}

export function StudentAnalyticsPdfReport({
  reportRef,
  range,
  generatedAt,
  overview,
  trend,
  heatmap,
  scatter,
  highRiskPairs = [],
  errorDistribution = [],
}: StudentAnalyticsPdfReportProps) {
  const latestSnapshot = overview?.latestSnapshot;

  const contextOption: AppChartOption = {
    tooltip: { trigger: 'axis' },
    legend: { bottom: 0, textStyle: { color: '#64748b' } },
    grid: { left: '6%', right: '5%', top: '8%', bottom: '20%', containLabel: true },
    xAxis: {
      type: 'category',
      data: (overview?.contextPerformance || []).map((item) => item.contextSupportLevel),
      axisLabel: { color: '#64748b' },
    },
    yAxis: [
      { type: 'value', max: 1, axisLabel: { color: '#64748b' } },
      { type: 'value', axisLabel: { color: '#64748b' } },
    ],
    series: [
      {
        name: '正确率',
        type: 'bar',
        data: (overview?.contextPerformance || []).map((item) => item.accuracy),
      },
      {
        name: '平均反应时',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        data: (overview?.contextPerformance || []).map((item) => item.avgReactionTimeMs),
      },
    ],
  };

  const errorDistributionOption: AppChartOption = {
    tooltip: { trigger: 'item' },
    series: [
      {
        type: 'pie',
        radius: ['48%', '75%'],
        data: errorDistribution.map((item) => ({ name: item.label, value: item.count })),
      },
    ],
  };

  return (
    <div ref={reportRef} className={reportShellClassName} aria-hidden="true">
      <div data-pdf-page="true" className={reportPageClassName}>
        <ReportHeader
          title="学生学情报告"
          subtitle="面向教师与家长的阶段性学情快照，聚焦近期迁移风险、训练建议与关键变化趋势。"
          generatedAt={generatedAt}
          meta={[
            `统计范围 ${rangeLabel(range)}`,
            `学生 ${overview?.studentName || '--'}`,
            `年级 ${overview?.gradeName || '--'}`,
          ]}
        />

        <div className="grid grid-cols-2 gap-4">
          <ReportSection title="学生档案">
            <div className="grid grid-cols-2 gap-3 text-sm text-slate-600">
              <div>
                <div className="text-xs uppercase tracking-[0.18em] text-slate-400">姓名</div>
                <div className="mt-2 text-base font-bold text-slate-900">{overview?.studentName || '--'}</div>
              </div>
              <div>
                <div className="text-xs uppercase tracking-[0.18em] text-slate-400">年级</div>
                <div className="mt-2 text-base font-bold text-slate-900">{overview?.gradeName || '--'}</div>
              </div>
              <div>
                <div className="text-xs uppercase tracking-[0.18em] text-slate-400">法语水平</div>
                <div className="mt-2 text-base font-bold text-slate-900">{overview?.frenchLevel || '--'}</div>
              </div>
            </div>
          </ReportSection>
          <ReportSection title="当前判断" subtitle="系统根据最近阶段性数据生成。">
            <div className="space-y-3">
              <ReportMetricCard label="主要风险" value={riskLevelLabel(overview?.primaryRiskLevel)} />
              <ReportMetricCard label="推荐训练模式" value={trainingModeLabel(overview?.recommendedTrainingMode)} />
            </div>
          </ReportSection>
        </div>

        <div className="grid grid-cols-4 gap-3">
          {(overview?.cards || []).slice(0, 4).map((card) => (
            <ReportMetricCard key={card.key} label={card.label} value={`${card.value}${card.unit || ''}`} />
          ))}
        </div>

        <div className="grid grid-cols-[1.15fr_0.85fr] gap-4">
          <ReportChartCard
            title="能力雷达"
            option={buildRadarOption(overview?.radar)}
            height={260}
            empty={!overview?.radar.length}
          />
          <ReportSection title="最新快照" subtitle="用于快速向家长或教师说明最近状态。">
            <div className="grid grid-cols-2 gap-3">
              <ReportMetricCard label="最近正确率" value={formatMaybePercent(latestSnapshot?.recentAccuracy)} />
              <ReportMetricCard label="最近风险" value={formatMaybePercent(latestSnapshot?.recentNegativeTransferRisk)} />
              <ReportMetricCard label="平均反应时" value={formatMs(latestSnapshot?.recentAvgReactionTimeMs)} />
              <ReportMetricCard label="待复习词对" value={String(latestSnapshot?.pendingReviewCount ?? 0)} />
            </div>
            <div className="mt-4">
              <div className="text-xs uppercase tracking-[0.2em] text-slate-400">重点标签</div>
              <div className="mt-2">
                <ReportPillList items={latestSnapshot?.focusTags || []} emptyText="当前没有额外关注标签。" />
              </div>
            </div>
          </ReportSection>
        </div>

        <ReportChartCard title="趋势分析" option={buildTrendOption(trend)} height={260} empty={!trend?.series.length} />
      </div>

      <div data-pdf-page="true" className={reportPageClassName}>
        <ReportHeader
          title="学生学情报告"
          subtitle="迁移热区、语境表现和高风险词对用于支持家校沟通与训练安排。"
          generatedAt={generatedAt}
          meta={[`统计范围 ${rangeLabel(range)}`, `学生 ${overview?.studentName || '--'}`, '第 2 页']}
        />

        <div className="grid grid-cols-2 gap-4">
          <ReportChartCard title="迁移热力图" option={buildHeatmapOption(heatmap)} height={230} empty={!heatmap?.cells.length} />
          <ReportChartCard title="反应时 / 正确率散点" option={buildScatterOption(scatter)} height={230} empty={!scatter?.points.length} />
        </div>

        <div className="grid grid-cols-2 gap-4">
          <ReportChartCard
            title="语境支持表现"
            option={contextOption}
            height={220}
            empty={!overview?.contextPerformance.length}
          />
          <ReportChartCard
            title="错误分布"
            option={errorDistributionOption}
            height={220}
            empty={!errorDistribution.length}
          />
        </div>

        <ReportSection
          title="高风险词对"
          subtitle="优先处理最近误判频繁、风险值偏高的词对，可直接作为训练与讲评重点。"
          className="flex-1"
        >
          <StudentRiskTable pairs={highRiskPairs} />
        </ReportSection>
      </div>
    </div>
  );
}

export function ClassAnalyticsPdfReport({
  reportRef,
  generatedAt,
  detail,
  overview,
  riskDistribution = [],
  heatmap,
  errorDistribution = [],
  completionRate,
  students = [],
}: ClassAnalyticsPdfReportProps) {
  const latestSnapshot = overview?.latestSnapshot;

  const riskDistributionOption: AppChartOption = {
    tooltip: { trigger: 'axis' },
    grid: { left: '8%', right: '5%', top: '10%', bottom: '16%', containLabel: true },
    xAxis: {
      type: 'category',
      data: riskDistribution.map((item) => `${item.bucketStart.toFixed(1)}-${item.bucketEnd.toFixed(1)}`),
      axisLabel: { color: '#64748b', fontSize: 10 },
    },
    yAxis: { type: 'value', axisLabel: { color: '#64748b' } },
    series: [
      {
        type: 'bar',
        data: riskDistribution.map((item) => item.studentCount),
      },
    ],
  };

  const errorDistributionOption: AppChartOption = {
    tooltip: { trigger: 'item' },
    series: [
      {
        type: 'pie',
        radius: ['48%', '75%'],
        data: errorDistribution.map((item) => ({ name: item.label, value: item.count })),
      },
    ],
  };

  const focusModes = (latestSnapshot?.recommendedFocusModes || []).map(
    (item) => `${trainingModeLabel(item.mode)} · ${item.studentCount} 人`
  );

  return (
    <div ref={reportRef} className={reportShellClassName} aria-hidden="true">
      <div data-pdf-page="true" className={reportPageClassName}>
        <ReportHeader
          title="班级学情报告"
          subtitle="面向教师和家长会场景的班级分析摘要，用于沟通整体风险分布、活跃度和近期训练方向。"
          generatedAt={generatedAt}
          meta={[
            `班级 ${overview?.className || detail?.className || '--'}`,
            `邀请码 ${overview?.classCode || detail?.classCode || '--'}`,
            `年级 ${detail?.gradeName || '--'}`,
          ]}
        />

        <div className="grid grid-cols-4 gap-3">
          {(overview?.cards || []).slice(0, 4).map((card) => (
            <ReportMetricCard key={card.key} label={card.label} value={`${card.value}${card.unit || ''}`} />
          ))}
        </div>

        <div className="grid grid-cols-[1.1fr_0.9fr] gap-4">
          <ReportSection title="班级概览">
            <div className="grid grid-cols-2 gap-3 text-sm text-slate-600">
              <ReportMetricCard label="班级人数" value={String(overview?.studentCount ?? detail?.studentCount ?? 0)} />
              <ReportMetricCard label="活跃学生" value={String(overview?.activeStudentCount ?? latestSnapshot?.activeStudentCount ?? 0)} />
              <ReportMetricCard label="高风险学生" value={String(overview?.highRiskStudentCount ?? latestSnapshot?.highRiskStudentCount ?? 0)} />
              <ReportMetricCard label="主要风险" value={riskLevelLabel(overview?.primaryRiskLevel)} />
            </div>
          </ReportSection>
          <ReportSection title="建议聚焦" subtitle="适合班会讲评和后续训练安排。">
            <div className="space-y-3">
              <ReportMetricCard label="班级最近正确率" value={formatMaybePercent(latestSnapshot?.recentAccuracy)} />
              <ReportMetricCard label="班级平均反应时" value={formatMs(latestSnapshot?.recentAvgReactionTimeMs)} />
            </div>
            <div className="mt-4">
              <div className="text-xs uppercase tracking-[0.2em] text-slate-400">推荐重点模式</div>
              <div className="mt-2">
                <ReportPillList items={focusModes} emptyText="当前没有模式聚焦建议。" />
              </div>
            </div>
          </ReportSection>
        </div>

        <div className="grid grid-cols-[0.95fr_1.05fr] gap-4">
          <ReportChartCard
            title="班级能力雷达"
            option={buildRadarOption(overview?.radar)}
            height={255}
            empty={!overview?.radar.length}
          />
          <ReportChartCard title="班级迁移热力图" option={buildHeatmapOption(heatmap)} height={255} empty={!heatmap?.cells.length} />
        </div>

        <ReportSection title="班级说明">
          <div className="grid grid-cols-3 gap-3 text-sm text-slate-600">
            <ReportMetricCard label="建档时间" value={formatDateTime(detail?.createdAt)} />
            <ReportMetricCard label="最近更新" value={formatDateTime(detail?.updatedAt)} />
            <ReportMetricCard label="班级邀请码" value={detail?.classCode || '--'} />
          </div>
        </ReportSection>
      </div>

      <div data-pdf-page="true" className={reportPageClassName}>
        <ReportHeader
          title="班级学情报告"
          subtitle="按风险分层、完成趋势与学生名单快速找到需要优先干预的对象。"
          generatedAt={generatedAt}
          meta={[`班级 ${overview?.className || detail?.className || '--'}`, '最近 30 天', '第 2 页']}
        />

        <div className="grid grid-cols-3 gap-4">
          <ReportChartCard title="风险分桶" option={riskDistributionOption} height={220} empty={!riskDistribution.length} />
          <ReportChartCard
            title="完成趋势"
            option={buildTrendOption(completionRate?.trend)}
            height={220}
            empty={!completionRate?.trend.series.length}
          />
          <ReportChartCard title="错误分布" option={errorDistributionOption} height={220} empty={!errorDistribution.length} />
        </div>

        <div className="grid grid-cols-[0.9fr_1.1fr] gap-4">
          <ReportSection title="训练完成摘要" subtitle="可直接用于教师讲评或家长沟通。">
            <div className="grid grid-cols-2 gap-3">
              <ReportMetricCard label="总体完成率" value={formatMaybePercent(completionRate?.overallRate)} />
              <ReportMetricCard label="已完成学生" value={String(completionRate?.completedStudentCount ?? 0)} />
              <ReportMetricCard label="总学生数" value={String(completionRate?.studentCount ?? 0)} />
              <ReportMetricCard label="最近活跃" value={String(latestSnapshot?.activeStudentCount ?? 0)} />
            </div>
            <div className="mt-4 space-y-2">
              {(completionRate?.byMode || []).slice(0, 4).map((item) => (
                <div
                  key={item.mode}
                  className="flex items-center justify-between rounded-[1rem] bg-slate-50 px-3 py-2 text-sm text-slate-700"
                >
                  <span>{trainingModeLabel(item.mode)}</span>
                  <span className="font-semibold">
                    {item.completedStudentCount} / {item.studentCount} · {formatMaybePercent(item.completionRate)}
                  </span>
                </div>
              ))}
            </div>
          </ReportSection>

          <ReportSection title="重点学生名单" subtitle="按最近迁移风险从高到低排序，便于优先干预。">
            <ClassStudentTable students={students} />
          </ReportSection>
        </div>
      </div>
    </div>
  );
}
