import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate, useParams } from 'react-router-dom';
import { ConfirmationDialog } from '@/components/common/ConfirmationDialog';
import { FeedbackState } from '@/components/common/FeedbackState';
import { PageHeader, StatusBadge } from '@/components/common';
import { getApiErrorMessage, saveBlob } from '@/lib/api';
import { assessmentQuestionTypeLabel, formatDateTime } from '@/lib/format';
import { researchAnalyticsService } from '@/lib/services';
import {
  attemptDurationMs,
  hasScanIssue,
  isProfileQuestion,
  isQuestionAnomalous,
  isQuestionUnanswered,
  sectionHeading,
  type ResearchAttemptQuestion,
} from '@/features/research-analytics/attemptView';
import {
  aiAnalysisStatusLabel,
  formatDuration,
  formatFileSize,
  formatScore,
  participantTypeLabel,
  qualityFlagLabel,
  scanStatusLabel,
  submitReasonLabel,
} from '@/features/research-analytics/formatters';
import { researchAnalyticsKeys } from '@/features/research-analytics/queryKeys';

type QuestionFilter = 'all' | 'profile' | 'formal' | 'unanswered' | 'attachments';

const FILTERS: Array<{ id: QuestionFilter; label: string }> = [
  { id: 'all', label: '全部' },
  { id: 'profile', label: '资料' },
  { id: 'formal', label: '正式题' },
  { id: 'unanswered', label: '未答' },
  { id: 'attachments', label: '附件' },
];

const matchesFilter = (question: ResearchAttemptQuestion, filter: QuestionFilter) => {
  if (filter === 'profile') return isProfileQuestion(question);
  if (filter === 'formal') return !isProfileQuestion(question);
  if (filter === 'attachments') return question.attachments.length > 0;
  if (filter === 'unanswered') return isQuestionAnomalous(question);
  return true;
};

const ResearchAttemptDetailPage: React.FC = () => {
  const { attemptId } = useParams();
  const navigate = useNavigate();
  const numericId = Number(attemptId);
  const [filter, setFilter] = React.useState<QuestionFilter>('all');
  const [pendingFileId, setPendingFileId] = React.useState<number | null>(null);
  const [downloadError, setDownloadError] = React.useState<string | null>(null);

  const query = useQuery({
    queryKey: researchAnalyticsKeys.attemptDetail(numericId),
    queryFn: ({ signal }) => researchAnalyticsService.getAttemptDetail(numericId, { signal }),
    enabled: Number.isFinite(numericId),
  });

  const detail = query.data;
  const backToData = () => {
    const publishId = detail?.attempt.publishId;
    navigate(publishId ? `/teacher/research?tab=data&publishId=${publishId}` : '/teacher/research?tab=data');
  };

  const questions = (detail?.questions || []).filter((question) => matchesFilter(question, filter));
  const profileQuestions = questions.filter((question) => isProfileQuestion(question));
  const formalQuestions = questions.filter((question) => !isProfileQuestion(question));
  const unansweredCount = (detail?.questions || []).filter((question) => isQuestionUnanswered(question)).length;
  const attachmentCount = (detail?.questions || []).reduce((sum, question) => sum + question.attachments.length, 0);

  const downloadFile = async (fileId: number, fileName: string) => {
    try {
      saveBlob(await researchAnalyticsService.downloadFile(fileId), fileName);
    } catch (error) {
      setDownloadError(getApiErrorMessage(error, '附件下载失败，请稍后重试。'));
    } finally {
      setPendingFileId(null);
    }
  };

  const jumpTo = (questionId: number) => {
    document.getElementById(`research-question-${questionId}`)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  };

  if (query.isLoading) {
    return <FeedbackState kind="loading" title="正在打开答卷" description="正在读取匿名参与者的题目、质量和附件。" />;
  }
  if (query.isError || !detail) {
    return <FeedbackState kind="error" title="无法打开这份答卷" description={getApiErrorMessage(query.error, '答卷详情暂时不可用。')} primaryAction={{ label: '返回数据页', onClick: backToData }} />;
  }

  return (
    <div className="page-stack pb-16">
      <PageHeader
        eyebrow="匿名答卷"
        title={detail.participant.participantCode}
        subtitle={`${detail.attempt.paperTitle} · ${participantTypeLabel(detail.participant.participantType)}`}
        actions={<button type="button" className="rounded-2xl border border-slate-200 px-4 py-2 text-sm font-bold dark:border-white/10" onClick={backToData}>返回数据</button>}
      />

      <section className="rounded-2xl liquid-glass-panel p-4 sm:p-6">
        <div className="flex flex-wrap gap-2">
          <StatusBadge label={detail.attempt.status === 'SUBMITTED' ? '已提交' : '作答中'} tone={detail.attempt.status === 'SUBMITTED' ? 'success' : 'info'} />
          {detail.attempt.submitReason ? <StatusBadge label={submitReasonLabel(detail.attempt.submitReason)} tone={detail.attempt.submitReason === 'TIMEOUT' ? 'warning' : 'neutral'} /> : null}
          {detail.result.qualityFlags.map((flag) => <StatusBadge key={flag} label={qualityFlagLabel(flag)} tone="warning" />)}
          <StatusBadge label={aiAnalysisStatusLabel(detail.ai.status)} />
        </div>
        <div className="mt-4 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
          <Metric title="进度" value={`${detail.attempt.answeredCount}/${detail.attempt.questionCount}`} hint={unansweredCount ? `${unansweredCount} 题未答` : '已全部作答'} />
          <Metric title="用时" value={formatDuration(attemptDurationMs(detail))} hint={`开始 ${detail.attempt.startedAt ? formatDateTime(detail.attempt.startedAt) : '—'}`} />
          <Metric title="提交时间" value={detail.attempt.submittedAt ? formatDateTime(detail.attempt.submittedAt) : '尚未提交'} hint={`保存 ${detail.attempt.lastSavedAt ? formatDateTime(detail.attempt.lastSavedAt) : '—'}`} />
          <Metric title="得分" value={formatScore(detail.result.percentageScore)} hint="研究问卷参考分，不作为成绩单" />
        </div>
      </section>

      {detail.ai.analysis ? (
        <section className="rounded-2xl liquid-glass-panel p-4 sm:p-6">
          <p className="text-xs font-bold uppercase tracking-wider text-slate-400">单份解读</p>
          <h2 className="mt-2 text-lg font-black">{detail.ai.status === 'FALLBACK' ? '规则摘要' : '模型解读'}</h2>
          <p className="mt-3 text-sm leading-7 text-slate-600 dark:text-white/70">{detail.ai.analysis.performanceOverview}</p>
          <p className="mt-3 text-xs text-slate-400">来源 {detail.ai.modelName || '规则'} · {detail.ai.completedAt ? formatDateTime(detail.ai.completedAt) : '未完成'}{detail.ai.fallbackReason ? ` · ${detail.ai.fallbackReason}` : ''}</p>
        </section>
      ) : null}

      <div className="sticky top-20 z-20 space-y-3 rounded-2xl border border-slate-200/80 bg-white/90 p-3 backdrop-blur dark:border-white/10 dark:bg-slate-950/85">
        <div className="flex flex-wrap gap-2">
          {FILTERS.map((item) => (
            <button
              key={item.id}
              type="button"
              onClick={() => setFilter(item.id)}
              className={`rounded-full px-3 py-1.5 text-xs font-bold ${filter === item.id ? 'bg-primary text-white' : 'border border-slate-200 dark:border-white/10'}`}
            >
              {item.label}
              {item.id === 'unanswered' && unansweredCount ? ` ${unansweredCount}` : ''}
              {item.id === 'attachments' && attachmentCount ? ` ${attachmentCount}` : ''}
            </button>
          ))}
        </div>
        {questions.length ? (
          <nav aria-label="题目跳转" className="flex max-h-28 flex-wrap gap-1.5 overflow-y-auto">
            {questions.map((question, index) => {
              const formalIndex = formalQuestions.findIndex((item) => item.questionId === question.questionId);
              const profileIndex = profileQuestions.findIndex((item) => item.questionId === question.questionId);
              const label = isProfileQuestion(question)
                ? `资${profileIndex + 1}`
                : String(formalIndex >= 0 ? formalIndex + 1 : index + 1);
              const ariaLabel = isProfileQuestion(question) ? `跳到资料 ${profileIndex + 1}` : `跳到第 ${label} 题`;
              return (
                <button
                  key={question.questionId}
                  type="button"
                  onClick={() => jumpTo(question.questionId)}
                  className={`min-h-8 min-w-8 rounded-lg px-2 text-[11px] font-bold ${
                    isQuestionUnanswered(question)
                      ? 'border border-amber-300 bg-amber-50 text-amber-800'
                      : 'border border-slate-200 bg-white text-slate-600 dark:border-white/10 dark:bg-slate-900 dark:text-white/70'
                  }`}
                  aria-label={ariaLabel}
                >
                  {label}
                </button>
              );
            })}
          </nav>
        ) : null}
      </div>

      {!questions.length ? (
        <div className="rounded-2xl border border-dashed border-slate-300 px-5 py-10 text-center text-sm text-slate-500">当前筛选没有题目。</div>
      ) : null}

      {profileQuestions.length ? (
        <QuestionGroup title="资料" hint="姓名、联系方式和背景题，与正式计分题分开查看。">
          {profileQuestions.map((question, index) => (
            <QuestionCard key={question.questionId} question={question} indexLabel={`资料 ${index + 1}/${profileQuestions.length}`} onDownload={(fileId) => setPendingFileId(fileId)} />
          ))}
        </QuestionGroup>
      ) : null}

      {formalQuestions.length ? (
        <QuestionGroup title="正式题" hint="按题号排列，可从上方题号跳转。">
          {formalQuestions.map((question, index) => (
            <QuestionCard key={question.questionId} question={question} indexLabel={`第 ${index + 1}/${formalQuestions.length} 题`} onDownload={(fileId) => setPendingFileId(fileId)} />
          ))}
        </QuestionGroup>
      ) : null}

      <ConfirmationDialog
        open={pendingFileId != null}
        title="下载附件将被审计"
        description="下载会写入审计日志。只有通过类型校验的文件可以下载，且不会暴露底层存储地址。"
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

      {downloadError ? (
        <div className="rounded-2xl border border-rose-500/20 bg-rose-500/5 px-4 py-3 text-sm text-rose-500" role="alert">
          {downloadError}
        </div>
      ) : null}
    </div>
  );
};

const Metric: React.FC<{ title: string; value: string; hint: string }> = ({ title, value, hint }) => (
  <div className="min-w-0 rounded-2xl border border-slate-200/80 bg-white/70 px-4 py-3 dark:border-white/10 dark:bg-white/[0.03]">
    <div className="text-xs text-slate-500">{title}</div>
    <div className="mt-1 break-words text-lg font-black tabular-nums">{value}</div>
    <p className="mt-1 text-xs leading-5 text-slate-400">{hint}</p>
  </div>
);

const QuestionGroup: React.FC<{ title: string; hint: string; children: React.ReactNode }> = ({ title, hint, children }) => (
  <section className="space-y-3">
    <div>
      <h2 className="text-lg font-black">{title}</h2>
      <p className="mt-1 text-sm text-slate-500">{hint}</p>
    </div>
    <div className="space-y-4">{children}</div>
  </section>
);

const QuestionCard: React.FC<{ question: ResearchAttemptQuestion; indexLabel: string; onDownload: (fileId: number) => void }> = ({ question, indexLabel, onDownload }) => {
  const unanswered = isQuestionUnanswered(question);

  return (
    <article id={`research-question-${question.questionId}`} className="scroll-mt-36 rounded-2xl liquid-glass-panel p-4 sm:p-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0">
          <div className="text-xs font-bold text-slate-400">{indexLabel} · {assessmentQuestionTypeLabel(question.questionType)} · {sectionHeading(question)}</div>
          <h3 className="mt-2 font-black leading-7">{question.stemText}</h3>
          {question.promptText ? <p className="mt-2 text-sm text-slate-500">{question.promptText}</p> : null}
        </div>
        <div className="flex flex-wrap gap-2">
          {unanswered ? <StatusBadge label="未作答" tone="warning" /> : <StatusBadge label="已作答" tone="success" />}
          {hasScanIssue(question) ? <StatusBadge label="附件异常" tone="danger" /> : null}
          {!isProfileQuestion(question) && question.correct != null ? (
            <StatusBadge label={question.correct ? '答对' : '答错'} tone={question.correct ? 'success' : 'neutral'} />
          ) : null}
        </div>
      </div>

      {question.questionType !== 'INSTRUCTION' && question.options.length ? (
        <div className="mt-4 grid gap-2">
          {question.options.map((option) => {
            const selected = question.responses.includes(option.key);
            const correct = question.correctAnswers.includes(option.key);
            return (
              <div
                key={option.key}
                className={`rounded-xl border px-3 py-3 text-sm ${
                  correct
                    ? 'border-emerald-500/20 bg-emerald-500/10 text-emerald-700 dark:text-emerald-300'
                    : selected
                      ? 'border-primary/30 bg-primary/5 text-slate-800 dark:text-white'
                      : 'border-slate-200/70 bg-white/70 text-slate-500 dark:border-white/10 dark:bg-white/5'
                }`}
              >
                <div className="font-semibold">{option.key} · {option.label}</div>
                <div className="mt-1 text-xs opacity-70">{selected ? '学生选择' : correct ? '参考选项' : '未选'}</div>
              </div>
            );
          })}
        </div>
      ) : null}

      {question.questionType !== 'INSTRUCTION' && !question.options.length ? (
        <div className="mt-4 rounded-xl border border-slate-200/80 bg-white/70 px-4 py-3 text-sm dark:border-white/10 dark:bg-white/[0.03]">
          <div className="text-xs font-bold text-slate-400">学生回答</div>
          <div className="mt-1 break-words">{question.responses.join('、') || (question.attachments.length ? '见附件' : '未作答')}</div>
          {question.justification ? <p className="mt-2 text-slate-500">说明：{question.justification}</p> : null}
          {!isProfileQuestion(question) && question.correctAnswers.length ? (
            <p className="mt-2 text-xs text-slate-400">参考答案：{question.correctAnswers.join('、')}</p>
          ) : null}
        </div>
      ) : null}

      <p className="mt-3 text-xs text-slate-400">
        {isProfileQuestion(question) ? null : `得分 ${question.scoreAwarded ?? '—'} / ${question.questionScore ?? 0} · `}
        用时 {formatDuration(question.effectiveDurationMs)} · 修改 {question.responseChangeCount ?? 0} 次
      </p>

      {question.attachments.map((file) => (
        <div key={file.fileId} className="mt-3 flex flex-wrap items-center justify-between gap-3 rounded-xl border border-slate-200 px-3 py-2 dark:border-white/10">
          <div>
            <div className="font-bold">{file.originalFileName}</div>
            <div className="text-xs text-slate-400">{file.mimeType} · {formatFileSize(file.sizeBytes)} · {scanStatusLabel(file.scanStatus)}</div>
          </div>
          <button type="button" disabled={!file.downloadable} onClick={() => onDownload(file.fileId)} className="rounded-xl border border-slate-200 px-3 py-1.5 text-xs font-bold disabled:opacity-40 dark:border-white/10">
            {file.downloadable ? '下载' : '不可下载'}
          </button>
        </div>
      ))}
    </article>
  );
};

export default ResearchAttemptDetailPage;
