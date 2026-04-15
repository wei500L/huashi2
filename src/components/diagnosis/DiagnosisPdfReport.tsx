import React from 'react';
import { EChart } from '@/components/common/EChart';
import {
  buildRadarOption,
  formatDateTime,
  formatMaybePercent,
  formatMs,
  lexicalPairTypeLabel,
} from '@/lib/format';
import type { AiGuidanceResponseVO, DiagnosisItemResultDetailVO, DiagnosisResultDetailVO } from '@/lib/contracts';
import type { AppChartOption } from '@/lib/echarts';

const DIAGNOSIS_RADAR_MAX = 1;
const ITEMS_PER_PAGE = 8;
const reportShellClassName = 'absolute left-[-10000px] top-0 w-[794px] bg-white text-slate-900';
const reportPageClassName = 'flex h-[1123px] w-[794px] flex-col gap-5 overflow-hidden bg-white px-10 py-9';

type DiagnosisPdfReportProps = {
  reportRef: React.RefObject<HTMLDivElement | null>;
  generatedAt?: string | null;
  result: DiagnosisResultDetailVO;
  explanation?: AiGuidanceResponseVO | null;
  explanationErrorMessage?: string | null;
};

type ReportSectionProps = {
  title: string;
  subtitle?: string;
  children: React.ReactNode;
  className?: string;
};

type ReportMetricCardProps = {
  label: string;
  value: string;
  hint?: string;
};

function buildStaticChartOption(option: AppChartOption): AppChartOption {
  return {
    ...option,
    animation: false,
  };
}

function findOptionLabel(item: DiagnosisItemResultDetailVO, answerKey?: string | null): string {
  if (!answerKey) {
    return '未作答';
  }
  return item.options.find((option) => option.key === answerKey)?.label || answerKey;
}

function chunkItems<T>(items: T[], size: number): T[][] {
  if (!items.length) {
    return [[]];
  }

  const chunks: T[][] = [];
  for (let index = 0; index < items.length; index += size) {
    chunks.push(items.slice(index, index + size));
  }
  return chunks;
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

function ReportSection({ title, subtitle, children, className = '' }: ReportSectionProps) {
  return (
    <section className={`rounded-[1.7rem] border border-slate-200 bg-white p-5 ${className}`.trim()}>
      <div>
        <div className="text-sm font-black text-slate-900">{title}</div>
        {subtitle ? <div className="mt-1 text-xs leading-5 text-slate-500">{subtitle}</div> : null}
      </div>
      <div className="mt-4">{children}</div>
    </section>
  );
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

function ReportChartCard({
  title,
  option,
  height = 260,
  empty = false,
}: {
  title: string;
  option: AppChartOption;
  height?: number;
  empty?: boolean;
}) {
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

function InsightList({
  title,
  items,
  toneClassName,
}: {
  title: string;
  items: string[];
  toneClassName: string;
}) {
  if (!items.length) {
    return null;
  }

  return (
    <div className={`rounded-[1.4rem] border px-4 py-4 ${toneClassName}`}>
      <div className="text-xs font-bold uppercase tracking-[0.22em] text-slate-600">{title}</div>
      <div className="mt-3 space-y-2">
        {items.map((item) => (
          <div key={item} className="rounded-[1rem] bg-white/75 px-3 py-2 text-sm leading-6 text-slate-700">
            {item}
          </div>
        ))}
      </div>
    </div>
  );
}

function HighRiskPairsTable({ result }: { result: DiagnosisResultDetailVO }) {
  if (!result.highRiskLexicalPairs.length) {
    return <div className="text-sm text-slate-400">当前没有高风险词对。</div>;
  }

  return (
    <div className="overflow-hidden rounded-[1.2rem] border border-slate-200">
      <table className="min-w-full table-fixed border-collapse text-left text-xs text-slate-700">
        <thead className="bg-slate-50 text-slate-500">
          <tr>
            <th className="px-3 py-3 font-semibold">词对</th>
            <th className="px-3 py-3 font-semibold">类型 / 错误</th>
            <th className="px-3 py-3 font-semibold">风险</th>
            <th className="px-3 py-3 font-semibold">错误次数</th>
            <th className="px-3 py-3 font-semibold">平均反应时</th>
          </tr>
        </thead>
        <tbody>
          {result.highRiskLexicalPairs.slice(0, 8).map((item) => (
            <tr key={item.lexicalPairId} className="border-t border-slate-200">
              <td className="px-3 py-3 font-semibold text-slate-900">
                {item.englishWord} / {item.frenchWord}
              </td>
              <td className="px-3 py-3">
                {lexicalPairTypeLabel(item.lexicalPairType)} · {item.dominantErrorType}
              </td>
              <td className="px-3 py-3 text-rose-600">{formatMaybePercent(item.riskScore)}</td>
              <td className="px-3 py-3">{item.errorCount}</td>
              <td className="px-3 py-3">{formatMs(item.averageReactionTime)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function DiagnosisBreakdownTable({ items }: { items: DiagnosisItemResultDetailVO[] }) {
  if (!items.length) {
    return <div className="text-sm text-slate-400">当前没有逐题结果。</div>;
  }

  return (
    <div className="overflow-hidden rounded-[1.2rem] border border-slate-200">
      <table className="min-w-full table-fixed border-collapse text-left text-[11px] text-slate-700">
        <thead className="bg-slate-50 text-slate-500">
          <tr>
            <th className="w-[48px] px-2 py-3 font-semibold">题号</th>
            <th className="w-[148px] px-2 py-3 font-semibold">词对</th>
            <th className="w-[82px] px-2 py-3 font-semibold">题型</th>
            <th className="px-2 py-3 font-semibold">你的答案</th>
            <th className="px-2 py-3 font-semibold">正确答案</th>
            <th className="w-[54px] px-2 py-3 font-semibold">结果</th>
            <th className="w-[68px] px-2 py-3 font-semibold">反应时</th>
            <th className="w-[66px] px-2 py-3 font-semibold">风险</th>
          </tr>
        </thead>
        <tbody>
          {items.map((item) => (
            <tr key={item.itemResultId} className="border-t border-slate-200 align-top">
              <td className="px-2 py-3 font-semibold text-slate-900">{item.presentationOrder}</td>
              <td className="px-2 py-3">
                <div className="font-semibold text-slate-900">
                  {item.englishWord} / {item.frenchWord}
                </div>
                <div className="mt-1 text-[10px] text-slate-500">{lexicalPairTypeLabel(item.lexicalPairType)}</div>
              </td>
              <td className="px-2 py-3">{item.taskType}</td>
              <td className="px-2 py-3">{findOptionLabel(item, item.selectedAnswerKey)}</td>
              <td className="px-2 py-3">{findOptionLabel(item, item.correctAnswerKey)}</td>
              <td className={`px-2 py-3 font-semibold ${item.correct ? 'text-emerald-600' : 'text-rose-600'}`}>
                {item.correct ? '正确' : '错误'}
              </td>
              <td className="px-2 py-3">{formatMs(item.reactionTimeMs)}</td>
              <td className="px-2 py-3">{formatMaybePercent(item.transferRiskScore)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export function DiagnosisPdfReport({
  reportRef,
  generatedAt,
  result,
  explanation,
  explanationErrorMessage,
}: DiagnosisPdfReportProps) {
  const radarOption = buildRadarOption(
    (result.chartPayload.radarMetrics || []).map((metric) => ({
      key: metric.code,
      label: metric.label,
      value: metric.value,
      max: DIAGNOSIS_RADAR_MAX,
    }))
  );
  const breakdownPages = chunkItems(result.items, ITEMS_PER_PAGE);
  const explanationSummary =
    explanationErrorMessage || (!explanation ? '暂无解释内容。' : explanation.explanation || '暂无解释内容。');
  const teacherNote = explanation?.teacherNote || '当前没有额外备注。';

  return (
    <div ref={reportRef} className={reportShellClassName} aria-hidden="true">
      <div data-pdf-page="true" className={reportPageClassName}>
        <ReportHeader
          title="风险诊断报告"
          subtitle="用于留档、家校沟通和针对性训练安排的诊断摘要，聚焦当前迁移风险、能力轮廓与重点误判词对。"
          generatedAt={generatedAt}
          meta={[
            `诊断模板 ${result.templateName || '--'}`,
            `Session #${result.sessionId}`,
            `完成于 ${formatDateTime(result.completedAt)}`,
          ]}
        />

        <div className="grid grid-cols-4 gap-3">
          <ReportMetricCard label="正迁移得分" value={formatMaybePercent(result.metrics.positiveTransferScore)} />
          <ReportMetricCard label="负迁移风险" value={formatMaybePercent(result.metrics.negativeTransferRisk)} />
          <ReportMetricCard label="语义辨析" value={formatMaybePercent(result.metrics.semanticDiscrimination)} />
          <ReportMetricCard label="平均反应时" value={formatMs(result.metrics.averageReactionTime)} />
        </div>

        <div className="grid grid-cols-[0.95fr_1.05fr] gap-4">
          <ReportChartCard title="能力雷达" option={radarOption} height={275} empty={!result.chartPayload.radarMetrics.length} />
          <ReportSection title="诊断概览" subtitle="本页用于快速说明本轮诊断的总体结论。">
            <div className="grid grid-cols-2 gap-3">
              <ReportMetricCard label="诊断状态" value={result.status || '--'} />
              <ReportMetricCard label="完成题量" value={`${result.answeredItems}/${result.totalItems}`} />
              <ReportMetricCard label="整体正确率" value={formatMaybePercent(result.metrics.overallAccuracy)} />
              <ReportMetricCard label="开始时间" value={formatDateTime(result.startedAt)} />
            </div>
          </ReportSection>
        </div>

        <ReportSection title="高风险词对" subtitle="优先处理风险值高、反应慢或误判集中的词对。">
          <HighRiskPairsTable result={result} />
        </ReportSection>
      </div>

      <div data-pdf-page="true" className={reportPageClassName}>
        <ReportHeader
          title="风险诊断报告"
          subtitle="AI 对误判原因、能力优势和下一步建议的整理，可直接作为讲评或复盘材料。"
          generatedAt={generatedAt}
          meta={[`诊断模板 ${result.templateName || '--'}`, 'AI 诊断说明', '第 2 页']}
        />

        <ReportSection title="AI 诊断摘要" subtitle="优先保留结构化 strengths / weaknesses / suggestions。">
          {explanation?.diagnosisInsight ? (
            <div className="grid grid-cols-3 gap-3">
              <InsightList
                title="强项"
                items={explanation.diagnosisInsight.strengths}
                toneClassName="border-emerald-200 bg-emerald-50"
              />
              <InsightList
                title="弱项"
                items={explanation.diagnosisInsight.weaknesses}
                toneClassName="border-rose-200 bg-rose-50"
              />
              <InsightList
                title="建议"
                items={explanation.diagnosisInsight.suggestions}
                toneClassName="border-sky-200 bg-sky-50"
              />
            </div>
          ) : (
            <div className="rounded-[1.2rem] border border-dashed border-slate-200 bg-slate-50 px-4 py-6 text-sm text-slate-500">
              当前没有结构化 insight，以下展示摘要说明与备注。
            </div>
          )}
          <div className="mt-4 rounded-[1.2rem] bg-slate-50 px-4 py-4 text-sm leading-7 text-slate-700">{explanationSummary}</div>
        </ReportSection>

        <ReportSection title="教师 / 系统备注">
          <div className="rounded-[1.2rem] bg-slate-50 px-4 py-4 text-sm leading-7 text-slate-700">{teacherNote}</div>
        </ReportSection>
      </div>

      {breakdownPages.map((items, index) => (
        <div key={`breakdown-page-${index + 1}`} data-pdf-page="true" className={reportPageClassName}>
          <ReportHeader
            title="风险诊断报告"
            subtitle="逐题拆解页按题序列出学生作答、正确答案、结果、反应时与风险值，便于教师快速复盘。"
            generatedAt={generatedAt}
            meta={[`诊断模板 ${result.templateName || '--'}`, '逐题答题拆解', `第 ${index + 3} 页`]}
          />

          <ReportSection
            title="逐题答题拆解"
            subtitle={`本页展示第 ${index * ITEMS_PER_PAGE + 1} - ${Math.min((index + 1) * ITEMS_PER_PAGE, result.items.length)} 题`}
            className="flex-1"
          >
            <DiagnosisBreakdownTable items={items} />
          </ReportSection>
        </div>
      ))}
    </div>
  );
}
