import React from 'react';
import { FeedbackState } from '@/components/common/FeedbackState';
import { DataTable, type DataTableColumn, StatusBadge } from '@/components/common';
import { getApiErrorMessage } from '@/lib/api';
import type { ResearchAttemptSummaryVO } from '@/lib/contracts';
import { formatDateTime } from '@/lib/format';
import { aiAnalysisStatusLabel, formatDuration, qualityFlagLabels } from '../formatters';

export const ResearchAttemptList: React.FC<{
  rows: ResearchAttemptSummaryVO[];
  loading: boolean;
  error: unknown;
  onRetry: () => void;
  onOpen: (attemptId: number) => void;
  exportAction?: React.ReactNode;
}> = ({ rows, loading, error, onRetry, onOpen, exportAction }) => {
  const columns: Array<DataTableColumn<ResearchAttemptSummaryVO>> = [
    { id: 'participant', header: '参与者', render: (row) => <span className="font-bold">{row.participantCode}</span> },
    { id: 'attemptNo', header: '次数', render: (row) => String(row.attemptNo || 1) },
    { id: 'status', header: '状态', render: (row) => <StatusBadge label={row.status === 'SUBMITTED' ? '已提交' : '作答中'} tone={row.status === 'SUBMITTED' ? 'success' : 'info'} /> },
    { id: 'progress', header: '进度', render: (row) => `${row.answeredCount}/${row.questionCount}` },
    { id: 'duration', header: '用时', render: (row) => formatDuration(row.effectiveDurationMs) },
    { id: 'quality', header: '质量', render: (row) => row.qualityFlags.length ? qualityFlagLabels(row.qualityFlags).join('、') : '正常' },
    { id: 'attachments', header: '附件', render: (row) => String(row.attachmentCount) },
    { id: 'submittedAt', header: '提交时间', render: (row) => row.submittedAt ? formatDateTime(row.submittedAt) : '—' },
    { id: 'ai', header: '解读', render: (row) => aiAnalysisStatusLabel(row.aiAnalysisStatus) },
    { id: 'actions', header: '操作', render: (row) => (
      <button type="button" className="text-sm font-bold text-primary" onClick={() => onOpen(row.attemptId)} aria-label={`查看 ${row.participantCode} 的答卷`}>
        查看详情
      </button>
    ) },
  ];

  if (loading && !rows.length) {
    return <FeedbackState kind="loading" title="正在加载答卷" description="正在读取当前发布的匿名答卷列表。" compact />;
  }
  if (error && !rows.length) {
    return <FeedbackState kind="error" title="答卷列表加载失败" description={getApiErrorMessage(error, '暂时无法读取答卷。')} primaryAction={{ label: '重试', onClick: onRetry }} />;
  }

  return (
    <section className="rounded-2xl liquid-glass-panel p-4 sm:p-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 className="text-lg font-black">匿名答卷</h2>
        {exportAction}
      </div>
      <div className="mt-4 hidden md:block">
        <DataTable rows={rows} columns={columns} getRowId={(row) => row.attemptId} caption="研究问卷匿名答卷表" />
      </div>
      <div className="mt-4 space-y-3 md:hidden">
        {rows.map((row) => (
          <button
            key={row.attemptId}
            type="button"
            onClick={() => onOpen(row.attemptId)}
            className="w-full min-w-0 rounded-2xl border border-slate-200 bg-white/70 p-4 text-left dark:border-white/10 dark:bg-white/[0.03]"
            aria-label={`查看 ${row.participantCode} 的答卷`}
          >
            <div className="flex items-center justify-between gap-3">
              <span className="font-black">{row.participantCode}</span>
              <StatusBadge label={row.status === 'SUBMITTED' ? '已提交' : '作答中'} tone={row.status === 'SUBMITTED' ? 'success' : 'info'} />
            </div>
            <p className="mt-2 text-sm text-slate-500">
              第 {row.attemptNo || 1} 次 · {row.submittedAt ? formatDateTime(row.submittedAt) : '尚未提交'} · {row.answeredCount}/{row.questionCount} · 用时 {formatDuration(row.effectiveDurationMs)}
            </p>
            <p className="mt-1 text-xs text-slate-400">
              {row.qualityFlags.length ? qualityFlagLabels(row.qualityFlags).join('、') : '质量正常'}
              {row.attachmentCount ? ` · 附件 ${row.attachmentCount}` : ''}
            </p>
          </button>
        ))}
      </div>
    </section>
  );
};
