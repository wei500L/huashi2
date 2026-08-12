import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate, useParams } from 'react-router-dom';
import { ConfirmationDialog } from '@/components/common/ConfirmationDialog';
import { FeedbackState } from '@/components/common/FeedbackState';
import { PageHeader, SectionEyebrow, StatusBadge } from '@/components/common';
import { getApiErrorMessage, saveBlob } from '@/lib/api';
import { formatDateTime } from '@/lib/format';
import { researchAnalyticsService } from '@/lib/services';
import { formatDuration, formatFileSize, formatScore } from '@/features/research-analytics/formatters';
import { researchAnalyticsKeys } from '@/features/research-analytics/queryKeys';

const ResearchAttemptDetailPage: React.FC = () => {
  const { attemptId } = useParams();
  const navigate = useNavigate();
  const numericId = Number(attemptId);
  const [filter, setFilter] = React.useState<'all' | 'flagged' | 'attachments' | 'unanswered'>('all');
  const [pendingFileId, setPendingFileId] = React.useState<number | null>(null);

  const query = useQuery({
    queryKey: researchAnalyticsKeys.attemptDetail(numericId),
    queryFn: ({ signal }) => researchAnalyticsService.getAttemptDetail(numericId, { signal }),
    enabled: Number.isFinite(numericId),
  });

  const detail = query.data;
  const questions = (detail?.questions || []).filter((question) => {
    if (filter === 'attachments') return question.attachments.length > 0;
    if (filter === 'unanswered') return !question.responses.length && !question.attachments.length;
    if (filter === 'flagged') return question.correct === false;
    return true;
  });

  const downloadFile = async (fileId: number, fileName: string) => {
    saveBlob(await researchAnalyticsService.downloadFile(fileId), fileName);
    setPendingFileId(null);
  };

  if (query.isLoading) {
    return <FeedbackState kind="loading" title="正在打开答卷" description="正在读取匿名参与者的题目、质量和附件。" />;
  }
  if (query.isError || !detail) {
    return <FeedbackState kind="error" title="无法打开这份答卷" description={getApiErrorMessage(query.error, '答卷详情暂时不可用。')} primaryAction={{ label: '返回数据页', onClick: () => navigate('/teacher/research?tab=data') }} />;
  }

  return (
    <div className="page-stack pb-16">
      <PageHeader
        eyebrow="ANONYMOUS ATTEMPT"
        title={detail.participant.participantCode}
        subtitle={`${detail.attempt.paperTitle} · ${detail.participant.participantType === 'PUBLIC_QR' ? '二维码进入' : '参与码进入'}`}
        actions={<button type="button" className="rounded-2xl border border-slate-200 px-4 py-2 text-sm font-bold dark:border-white/10" onClick={() => navigate(-1)}>返回</button>}
      />
      <section className="rounded-2xl liquid-glass-panel p-4 sm:p-6">
        <div className="flex flex-wrap gap-2">
          <StatusBadge label={detail.attempt.status === 'SUBMITTED' ? '已提交' : '作答中'} tone={detail.attempt.status === 'SUBMITTED' ? 'success' : 'info'} />
          {detail.result.qualityFlags.map((flag) => <StatusBadge key={flag} label={flag} tone="warning" />)}
          <StatusBadge label={detail.ai.status === 'FALLBACK' ? '规则摘要' : detail.ai.status || '无单份解读'} />
        </div>
        <p className="mt-4 text-sm text-slate-500">
          开始 {detail.attempt.startedAt ? formatDateTime(detail.attempt.startedAt) : '—'}
          {' · '}保存 {detail.attempt.lastSavedAt ? formatDateTime(detail.attempt.lastSavedAt) : '—'}
          {' · '}提交 {detail.attempt.submittedAt ? formatDateTime(detail.attempt.submittedAt) : '—'}
        </p>
        <div className="mt-4 grid gap-3 sm:grid-cols-3">
          <div>得分 <strong>{formatScore(detail.result.percentageScore)}</strong></div>
          <div>进度 <strong>{detail.attempt.answeredCount}/{detail.attempt.questionCount}</strong></div>
          <div>附件 <strong>{detail.questions.reduce((sum, question) => sum + question.attachments.length, 0)}</strong></div>
        </div>
      </section>
      {detail.ai.analysis ? (
        <section className="rounded-2xl liquid-glass-panel p-4 sm:p-6">
          <SectionEyebrow>SINGLE ATTEMPT AI</SectionEyebrow>
          <h2 className="mt-2 text-lg font-black">{detail.ai.status === 'FALLBACK' ? '规则摘要' : '单份 AI 解读'}</h2>
          <p className="mt-3 text-sm leading-7 text-slate-600 dark:text-white/70">{detail.ai.analysis.performanceOverview}</p>
          <p className="mt-3 text-xs text-slate-400">来源 {detail.ai.modelName || '规则'} · {detail.ai.completedAt ? formatDateTime(detail.ai.completedAt) : '未完成'}{detail.ai.fallbackReason ? ` · ${detail.ai.fallbackReason}` : ''}</p>
        </section>
      ) : null}
      <div className="flex flex-wrap gap-2">
        {([['all', '全部'], ['flagged', '仅看异常'], ['attachments', '仅看附件'], ['unanswered', '仅看未答']] as const).map(([id, label]) => (
          <button key={id} type="button" onClick={() => setFilter(id)} className={`rounded-full px-3 py-1.5 text-xs font-bold ${filter === id ? 'bg-primary text-white' : 'border border-slate-200 dark:border-white/10'}`}>{label}</button>
        ))}
      </div>
      <div className="space-y-4">
        {questions.map((question) => (
          <article key={question.questionId} className="rounded-2xl liquid-glass-panel p-4 sm:p-6">
            <div className="text-xs font-bold uppercase tracking-wider text-slate-400">第 {question.questionOrder} 题 · {question.questionType}</div>
            <h3 className="mt-2 font-black leading-7">{question.stemText}</h3>
            <p className="mt-3 text-sm">学生回答：{question.responses.join('、') || (question.attachments.length ? '见附件' : '未作答')}</p>
            {question.correctAnswers.length ? <p className="mt-1 text-sm text-slate-500">参考答案：{question.correctAnswers.join('、')}</p> : null}
            <p className="mt-2 text-xs text-slate-400">得分 {question.scoreAwarded ?? '—'} / {question.questionScore ?? 0} · 用时 {formatDuration(question.effectiveDurationMs)} · 修改 {question.responseChangeCount ?? 0} 次</p>
            {question.attachments.map((file) => (
              <div key={file.fileId} className="mt-3 flex flex-wrap items-center justify-between gap-3 rounded-xl border border-slate-200 px-3 py-2 dark:border-white/10">
                <div>
                  <div className="font-bold">{file.originalFileName}</div>
                  <div className="text-xs text-slate-400">{file.mimeType} · {formatFileSize(file.sizeBytes)} · {file.scanStatus}</div>
                </div>
                <button type="button" disabled={!file.downloadable} onClick={() => setPendingFileId(file.fileId)} className="rounded-xl border border-slate-200 px-3 py-1.5 text-xs font-bold disabled:opacity-40 dark:border-white/10">
                  {file.downloadable ? '下载' : '不可下载'}
                </button>
              </div>
            ))}
          </article>
        ))}
      </div>
      <ConfirmationDialog
        open={pendingFileId != null}
        title="下载附件将被审计"
        description="下载会写入审计日志。只有扫描通过的文件可以下载，且不会暴露底层存储地址。"
        safety="下载不会把文件公开到外网，也不会展示底层存储地址。"
        nextStep="确认后将开始下载，并留下教师操作审计记录。"
        confirmLabel="确认下载"
        cancelLabel="取消"
        onCancel={() => setPendingFileId(null)}
        onConfirm={() => {
          const file = detail.questions.flatMap((question) => question.attachments).find((item) => item.fileId === pendingFileId);
          if (file) void downloadFile(file.fileId, file.originalFileName);
        }}
      />
    </div>
  );
};

export default ResearchAttemptDetailPage;
