import React from 'react';
import type { TeacherInterventionEffectVO } from '@/lib/contracts';
import {
  formatDateTime,
  formatMaybePercent,
  formatMs,
  riskLevelLabel,
  trainingModeLabel,
} from '@/lib/format';

type InterventionEffectPanelProps = {
  effectTracking?: TeacherInterventionEffectVO | null;
};

type ComparisonMetric = {
  key: string;
  label: string;
  baseline: number | null;
  completion: number | null;
  delta: number | null;
  lowerIsBetter?: boolean;
  formatter: (value: number | null) => string;
  deltaFormatter: (value: number | null) => string;
};

function formatSignedPercent(value: number | null): string {
  if (value == null) {
    return '--';
  }
  return `${value > 0 ? '+' : ''}${(value * 100).toFixed(1)}%`;
}

function formatSignedInteger(value: number | null): string {
  if (value == null) {
    return '--';
  }
  return `${value > 0 ? '+' : ''}${Math.round(value)}`;
}

function formatSignedMs(value: number | null): string {
  if (value == null) {
    return '--';
  }
  return `${value > 0 ? '+' : ''}${Math.round(value)} ms`;
}

function deltaTone(delta: number | null, lowerIsBetter = false): string {
  if (delta == null || Math.abs(delta) < 1e-9) {
    return 'text-slate-500 dark:text-white/45';
  }
  let improved = delta > 0;
  if (lowerIsBetter) {
    improved = delta < 0;
  }
  return improved ? 'text-emerald-600 dark:text-emerald-300' : 'text-rose-500 dark:text-rose-300';
}

function deltaLabel(delta: number | null, lowerIsBetter = false): string {
  if (delta == null) {
    return '待完成';
  }
  if (Math.abs(delta) < 1e-9) {
    return '持平';
  }
  let improved = delta > 0;
  if (lowerIsBetter) {
    improved = delta < 0;
  }
  return improved ? '改善' : '回落';
}

function formatRiskLevel(value?: string | null): string {
  return value ? riskLevelLabel(value) : '--';
}

function formatTrainingMode(value?: string | null): string {
  return value ? trainingModeLabel(value) : '--';
}

const InterventionEffectPanel: React.FC<InterventionEffectPanelProps> = ({ effectTracking }) => {
  const baseline = effectTracking?.baselineSnapshot || null;
  const completion = effectTracking?.completionSnapshot || null;
  const metricDiff = effectTracking?.metricDiff || null;

  if (!baseline && !completion) {
    return (
      <div className="rounded-[1.8rem] border border-dashed border-slate-300/80 bg-slate-50/80 p-5 text-sm text-slate-500 dark:border-white/15 dark:bg-white/[0.03] dark:text-white/45">
        尚未记录干预效果快照。新建干预后会自动绑定基线快照，标记完成后会生成结果快照。
      </div>
    );
  }

  const metrics: ComparisonMetric[] = [
    {
      key: 'accuracy',
      label: '正确率',
      baseline: baseline?.recentAccuracy ?? null,
      completion: completion?.recentAccuracy ?? null,
      delta: metricDiff?.recentAccuracyDelta ?? null,
      formatter: (value) => formatMaybePercent(value, 1),
      deltaFormatter: formatSignedPercent,
    },
    {
      key: 'risk',
      label: '负迁移风险',
      baseline: baseline?.recentNegativeTransferRisk ?? null,
      completion: completion?.recentNegativeTransferRisk ?? null,
      delta: metricDiff?.recentNegativeTransferRiskDelta ?? null,
      lowerIsBetter: true,
      formatter: (value) => formatMaybePercent(value, 1),
      deltaFormatter: formatSignedPercent,
    },
    {
      key: 'reaction',
      label: '平均反应时',
      baseline: baseline?.recentAvgReactionTimeMs ?? null,
      completion: completion?.recentAvgReactionTimeMs ?? null,
      delta: metricDiff?.recentAvgReactionTimeMsDelta ?? null,
      lowerIsBetter: true,
      formatter: (value) => formatMs(value),
      deltaFormatter: formatSignedMs,
    },
    {
      key: 'review',
      label: '待复习项',
      baseline: baseline?.pendingReviewCount ?? null,
      completion: completion?.pendingReviewCount ?? null,
      delta: metricDiff?.pendingReviewCountDelta ?? null,
      lowerIsBetter: true,
      formatter: (value) => (value == null ? '--' : String(value)),
      deltaFormatter: formatSignedInteger,
    },
    {
      key: 'riskPairs',
      label: '高风险词对',
      baseline: baseline?.highRiskPairCount ?? null,
      completion: completion?.highRiskPairCount ?? null,
      delta: metricDiff?.highRiskPairCountDelta ?? null,
      lowerIsBetter: true,
      formatter: (value) => (value == null ? '--' : String(value)),
      deltaFormatter: formatSignedInteger,
    },
  ];

  return (
    <div className="min-w-0 rounded-[1.8rem] border border-slate-200/70 bg-white/60 p-4 sm:p-5 dark:border-white/10 dark:bg-white/5">
      <div className="flex min-w-0 flex-col gap-3 sm:flex-row sm:flex-wrap sm:items-start sm:justify-between sm:gap-4">
        <div className="min-w-0">
          <div className="text-xs font-bold uppercase tracking-[0.28em] text-slate-400 dark:text-white/35">Effect Tracking</div>
          <div className="mt-2 text-lg font-black text-slate-900 dark:text-white">干预前后指标对比</div>
          <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
            {baseline && completion ? '已形成基线与完成快照闭环，可直接评估干预效果。' : '当前只记录了基线快照，完成干预后会补齐结果快照。'}
          </div>
        </div>
        <div className="min-w-0 text-left text-xs text-slate-500 sm:text-right dark:text-white/45">
          <div className="break-words">基线快照 {formatDateTime(baseline?.snapshotAt)}</div>
          <div className="mt-2 break-words">完成快照 {formatDateTime(completion?.snapshotAt)}</div>
        </div>
      </div>

      <div className="mt-5 grid min-w-0 grid-cols-1 gap-4 sm:grid-cols-2">
        <div className="min-w-0 rounded-[1.4rem] border border-slate-200/70 bg-white/70 p-4 dark:border-white/10 dark:bg-white/[0.03]">
          <div className="text-xs font-bold uppercase tracking-[0.24em] text-slate-400 dark:text-white/35">Before</div>
          <div className="mt-3 break-words text-sm text-slate-700 dark:text-white/80">{formatRiskLevel(baseline?.primaryRiskLevel)}</div>
          <div className="mt-2 break-words text-sm text-slate-500 dark:text-white/45">{formatTrainingMode(baseline?.recommendedTrainingMode)}</div>
        </div>
        <div className="min-w-0 rounded-[1.4rem] border border-slate-200/70 bg-white/70 p-4 dark:border-white/10 dark:bg-white/[0.03]">
          <div className="text-xs font-bold uppercase tracking-[0.24em] text-slate-400 dark:text-white/35">After</div>
          <div className="mt-3 break-words text-sm text-slate-700 dark:text-white/80">{formatRiskLevel(completion?.primaryRiskLevel)}</div>
          <div className="mt-2 break-words text-sm text-slate-500 dark:text-white/45">{formatTrainingMode(completion?.recommendedTrainingMode)}</div>
        </div>
      </div>

      <div className="mt-5 grid min-w-0 grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5">
        {metrics.map((metric) => (
          <div
            key={metric.key}
            className="min-w-0 rounded-[1.4rem] border border-slate-200/70 bg-white/70 p-4 dark:border-white/10 dark:bg-white/[0.03]"
          >
            <div className="truncate text-xs font-bold uppercase tracking-[0.2em] text-slate-400 dark:text-white/35">{metric.label}</div>
            <div className="mt-4 flex min-w-0 items-baseline justify-between gap-2 sm:gap-4">
              <div className="min-w-0">
                <div className="text-xs text-slate-400 dark:text-white/35">前</div>
                <div className="mt-1 break-all text-sm font-bold text-slate-700 dark:text-white/80">{metric.formatter(metric.baseline)}</div>
              </div>
              <div className="shrink-0 text-slate-300 dark:text-white/15">→</div>
              <div className="min-w-0 text-right">
                <div className="text-xs text-slate-400 dark:text-white/35">后</div>
                <div className="mt-1 break-all text-sm font-bold text-slate-900 dark:text-white">{metric.formatter(metric.completion)}</div>
              </div>
            </div>
            <div className={`mt-4 break-words text-sm font-bold ${deltaTone(metric.delta, metric.lowerIsBetter)}`}>
              {deltaLabel(metric.delta, metric.lowerIsBetter)} {metric.deltaFormatter(metric.delta)}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default InterventionEffectPanel;
