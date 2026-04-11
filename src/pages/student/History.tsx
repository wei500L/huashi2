import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { PageHeader, SectionEyebrow, StatusBadge } from '@/components/common';
import { FeedbackState } from '@/components/common/FeedbackState';
import { getProductizedErrorState } from '@/lib/async-state';
import {
  assessmentAttemptStatusLabel,
  assessmentAttemptStatusTone,
  assessmentQuestionTypeLabel,
  diagnosisSessionStatusLabel,
  diagnosisSessionStatusTone,
  diagnosisTaskTypeLabel,
  errorTypeLabel,
  formatDateTime,
  formatMaybePercent,
  formatMs,
  lexicalPairTypeLabel,
  riskLevelLabel,
  trainingModeLabel,
  trainingSessionStatusLabel,
  trainingSessionStatusTone,
} from '@/lib/format';
import { assessmentService, diagnosisSessionService, trainingService } from '@/lib/services';
import { buildTrainingHref } from '@/lib/training-launch';
import type {
  AssessmentAttemptResultQuestionVO,
  AssessmentOptionVO,
  DiagnosisItemResultDetailVO,
  DiagnosisOptionPayload,
  TrainingItemResultDetailVO,
  TrainingOptionViewVO,
} from '@/lib/contracts';

type HistoryTab = 'diagnosis' | 'training' | 'assessment';

const pageSize = 10;

function HistoryStatePanel({
  kind,
  title,
  description,
  impact,
  nextStep,
  actionLabel,
  onAction,
}: {
  kind: React.ComponentProps<typeof FeedbackState>['kind'];
  title: string;
  description: string;
  impact: string;
  nextStep: string;
  actionLabel?: string;
  onAction?: () => void;
}) {
  return (
    <FeedbackState
      kind={kind}
      title={title}
      description={description}
      impact={impact}
      nextStep={nextStep}
      primaryAction={actionLabel && onAction ? { label: actionLabel, onClick: onAction } : undefined}
    />
  );
}

function HistoryErrorPanel({
  error,
  resourceLabel,
  taskLabel,
  onRetry,
}: {
  error: unknown;
  resourceLabel: string;
  taskLabel: string;
  onRetry: () => void;
}) {
  const state = getProductizedErrorState(error, {
    resourceLabel,
    taskLabel,
    retryActionLabel: '重新获取',
  });

  return (
    <HistoryStatePanel
      kind={state.kind}
      title={state.title}
      description={state.description}
      impact={state.impact}
      nextStep={state.nextStep}
      actionLabel="重新获取"
      onAction={onRetry}
    />
  );
}

function totalPages(total = 0): number {
  return Math.max(1, Math.ceil(total / pageSize));
}

function findDiagnosisOptionLabel(options: DiagnosisOptionPayload[], answerKey?: string | null) {
  if (!answerKey) {
    return null;
  }
  return options.find((option) => option.key === answerKey)?.label || answerKey;
}

function DiagnosisHistoryItemReviewCard({ item }: { item: DiagnosisItemResultDetailVO }) {
  const selectedLabel = findDiagnosisOptionLabel(item.options, item.selectedAnswerKey);
  const correctLabel = findDiagnosisOptionLabel(item.options, item.correctAnswerKey);

  return (
    <div className="rounded-[1.6rem] border border-slate-200/70 bg-white/60 p-4 dark:border-white/10 dark:bg-white/5">
      <div className="flex items-start justify-between gap-4">
        <div>
          <div className="font-black text-slate-900 dark:text-white">
            {item.englishWord} / {item.frenchWord}
          </div>
          <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
            {diagnosisTaskTypeLabel(item.taskType)} · {errorTypeLabel(item.detectedErrorType)} · {formatMs(item.reactionTimeMs)}
          </div>
          {(item.stimulus.promptText || item.stimulus.instruction || item.stimulus.contextSentence) && (
            <div className="mt-3 rounded-[1.2rem] border border-dashed border-slate-200/80 px-4 py-3 text-sm text-slate-600 dark:border-white/10 dark:text-white/60">
              {item.stimulus.instruction && <div className="font-semibold">{item.stimulus.instruction}</div>}
              {item.stimulus.promptText && <div className="mt-1">{item.stimulus.promptText}</div>}
              {item.stimulus.contextSentence && <div className="mt-2 italic">{item.stimulus.contextSentence}</div>}
            </div>
          )}
        </div>
        <div className="text-right text-sm text-slate-500 dark:text-white/45">
          <StatusBadge label={item.correct ? '答对' : '答错'} tone={item.correct ? 'success' : 'danger'} />
          <div>{formatMaybePercent(item.itemScore)}</div>
        </div>
      </div>

      <div className="mt-4 grid gap-2 text-sm text-slate-500 dark:text-white/45">
        <div>你的答案：{selectedLabel || '未作答'}</div>
        <div>正确答案：{correctLabel || '未返回'}</div>
      </div>
    </div>
  );
}

function findTrainingOptionLabel(options: TrainingOptionViewVO[], answerKey?: string | null) {
  if (!answerKey) {
    return null;
  }
  return options.find((option) => option.key === answerKey)?.label || answerKey;
}

function TrainingHistoryItemReviewCard({ item }: { item: TrainingItemResultDetailVO }) {
  const selectedLabel = findTrainingOptionLabel(item.options, item.selectedAnswerKey);
  const correctLabel = findTrainingOptionLabel(item.options, item.correctAnswerKey);

  return (
    <div className="rounded-[1.6rem] border border-slate-200/70 bg-white/60 p-4 dark:border-white/10 dark:bg-white/5">
      <div className="flex items-start justify-between gap-4">
        <div>
          <div className="font-black text-slate-900 dark:text-white">
            {item.englishWord} / {item.frenchWord}
          </div>
          <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
            {trainingModeLabel(item.mode)} · {item.cognitiveTag} · {item.detectedErrorType ? errorTypeLabel(item.detectedErrorType) : '已完成'}
          </div>
          <div className="mt-3 rounded-[1.2rem] border border-dashed border-slate-200/80 px-4 py-3 text-sm text-slate-600 dark:border-white/10 dark:text-white/60">
            <div className="font-semibold">{item.content.question}</div>
            {item.content.sentence && <div className="mt-2 italic">{item.content.sentence}</div>}
            {item.stimulus.explanation && <div className="mt-2">{item.stimulus.explanation}</div>}
          </div>
        </div>
        <div className="text-right text-sm text-slate-500 dark:text-white/45">
          <StatusBadge label={item.correct ? '答对' : '答错'} tone={item.correct ? 'success' : 'danger'} />
          <div>{formatMs(item.reactionTimeMs)}</div>
        </div>
      </div>

      <div className="mt-4 grid gap-2 text-sm text-slate-500 dark:text-white/45">
        <div>你的答案：{selectedLabel || '未作答'}</div>
        <div>正确答案：{correctLabel || '未返回'}</div>
      </div>
    </div>
  );
}

function findAssessmentOptionLabel(options: AssessmentOptionVO[], answerKey?: string | null) {
  if (!answerKey) {
    return null;
  }
  return options.find((option) => option.key === answerKey)?.label || answerKey;
}

function AssessmentHistoryItemReviewCard({ item }: { item: AssessmentAttemptResultQuestionVO }) {
  const selectedLabel = item.options.length
    ? item.responses.map((answerKey) => findAssessmentOptionLabel(item.options, answerKey) || answerKey).join(' / ')
    : item.responses.join(' / ');
  const correctLabel = item.options.length
    ? item.correctAnswers.map((answerKey) => findAssessmentOptionLabel(item.options, answerKey) || answerKey).join(' / ')
    : item.correctAnswers.join(' / ');

  return (
    <div className="rounded-[1.6rem] border border-slate-200/70 bg-white/60 p-4 dark:border-white/10 dark:bg-white/5">
      <div className="flex items-start justify-between gap-4">
        <div>
          <div className="font-black text-slate-900 dark:text-white">
            第 {item.questionOrder} 题 · {assessmentQuestionTypeLabel(item.questionType)}
          </div>
          <div className="mt-2 text-sm text-slate-500 dark:text-white/45">{item.stemText}</div>
          {item.promptText && <div className="mt-2 text-sm text-slate-500 dark:text-white/45">{item.promptText}</div>}
        </div>
        <div className="text-right text-sm text-slate-500 dark:text-white/45">
          <StatusBadge label={item.correct ? '答对' : '答错'} tone={item.correct ? 'success' : 'danger'} />
          <div>
            {item.scoreAwarded ?? 0} / {item.score}
          </div>
        </div>
      </div>

      <div className="mt-4 grid gap-2 text-sm text-slate-500 dark:text-white/45">
        <div>你的答案：{selectedLabel || '未作答'}</div>
        <div>正确答案：{correctLabel || '未返回'}</div>
      </div>

      {!!item.options.length && (
        <div className="mt-4 grid gap-2">
          {item.options.map((option) => {
            const selected = item.responses.includes(option.key);
            const correct = item.correctAnswers.includes(option.key);
            return (
              <div
                key={option.key}
                className={`rounded-[1.2rem] border px-4 py-3 text-sm ${
                  correct
                    ? 'border-emerald-500/20 bg-emerald-500/10 text-emerald-700 dark:text-emerald-300'
                    : selected
                      ? 'border-rose-500/20 bg-rose-500/5 text-rose-600 dark:text-rose-300'
                      : 'border-slate-200/70 bg-white/70 text-slate-600 dark:border-white/10 dark:bg-white/5 dark:text-white/60'
                }`}
              >
                <div className="font-semibold">{option.key}</div>
                <div className="mt-1">{option.label}</div>
              </div>
            );
          })}
        </div>
      )}

      {item.explanationText && (
        <div className="mt-4 rounded-[1.2rem] border border-dashed border-slate-200/80 px-4 py-3 text-sm text-slate-600 dark:border-white/10 dark:text-white/60">
          {item.explanationText}
        </div>
      )}
    </div>
  );
}

const HistoryPage: React.FC = () => {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = React.useState<HistoryTab>('diagnosis');
  const [diagnosisStatus, setDiagnosisStatus] = React.useState('ALL');
  const [trainingStatus, setTrainingStatus] = React.useState('ALL');
  const [assessmentStatus, setAssessmentStatus] = React.useState('ALL');
  const [diagnosisPageNo, setDiagnosisPageNo] = React.useState(1);
  const [trainingPageNo, setTrainingPageNo] = React.useState(1);
  const [assessmentPageNo, setAssessmentPageNo] = React.useState(1);
  const [selectedDiagnosisSessionId, setSelectedDiagnosisSessionId] = React.useState<number | null>(null);
  const [selectedTrainingSessionId, setSelectedTrainingSessionId] = React.useState<number | null>(null);
  const [selectedAssessmentAttemptId, setSelectedAssessmentAttemptId] = React.useState<number | null>(null);

  const diagnosisHistoryQuery = useQuery({
    queryKey: ['diagnosis-history', 'history-page', diagnosisPageNo, diagnosisStatus],
    queryFn: ({ signal }) =>
      diagnosisSessionService.listHistory(
        {
          pageNo: diagnosisPageNo,
          pageSize,
          status: diagnosisStatus === 'ALL' ? undefined : diagnosisStatus,
        },
        { signal }
      ),
  });

  const trainingHistoryQuery = useQuery({
    queryKey: ['training-history', 'history-page', trainingPageNo, trainingStatus],
    queryFn: ({ signal }) =>
      trainingService.listHistory(
        {
          pageNo: trainingPageNo,
          pageSize,
          status: trainingStatus === 'ALL' ? undefined : trainingStatus,
        },
        { signal }
      ),
  });
  const assessmentHistoryQuery = useQuery({
    queryKey: ['student-assessment-history', assessmentPageNo, assessmentStatus],
    queryFn: ({ signal }) =>
      assessmentService.listStudentHistory(
        {
          pageNo: assessmentPageNo,
          pageSize,
          status: assessmentStatus === 'ALL' ? undefined : assessmentStatus,
        },
        { signal }
      ),
  });

  const diagnosisDetailQuery = useQuery({
    queryKey: ['diagnosis-result', 'history-page', selectedDiagnosisSessionId],
    queryFn: ({ signal }) => diagnosisSessionService.getResult(selectedDiagnosisSessionId as number, { signal }),
    enabled: selectedDiagnosisSessionId !== null,
  });

  const trainingDetailQuery = useQuery({
    queryKey: ['training-summary', 'history-page', selectedTrainingSessionId],
    queryFn: ({ signal }) => trainingService.getSummary(selectedTrainingSessionId as number, { signal }),
    enabled: selectedTrainingSessionId !== null,
  });
  const selectedAssessmentRecord = React.useMemo(
    () => assessmentHistoryQuery.data?.records.find((record) => record.attemptId === selectedAssessmentAttemptId) || null,
    [assessmentHistoryQuery.data?.records, selectedAssessmentAttemptId]
  );
  const assessmentDetailQuery = useQuery({
    queryKey: ['student-assessment-result', 'history-page', selectedAssessmentAttemptId],
    queryFn: ({ signal }) => assessmentService.getStudentAttemptResult(selectedAssessmentAttemptId as number, { signal }),
    enabled: selectedAssessmentAttemptId !== null && selectedAssessmentRecord?.status === 'SUBMITTED',
  });

  React.useEffect(() => {
    setDiagnosisPageNo(1);
  }, [diagnosisStatus]);

  React.useEffect(() => {
    setTrainingPageNo(1);
  }, [trainingStatus]);

  React.useEffect(() => {
    setAssessmentPageNo(1);
  }, [assessmentStatus]);

  const diagnosisData = diagnosisHistoryQuery.data;
  const trainingData = trainingHistoryQuery.data;
  const assessmentData = assessmentHistoryQuery.data;

  return (
    <div className="space-y-8 pb-20">
      <PageHeader
        eyebrow="学习记录"
        title="学习历史"
        subtitle="统一查看诊断、训练与通用测评记录；进行中的任务会跳回对应页面继续，已完成记录可在当前页查看详情。"
      />

      <section className="rounded-[2.5rem] liquid-glass-panel p-4">
        <div className="grid gap-3 md:grid-cols-3">
          <button
            type="button"
            onClick={() => setActiveTab('diagnosis')}
            className={`rounded-[1.8rem] px-5 py-4 text-left transition-all ${
              activeTab === 'diagnosis'
                ? 'border border-primary/30 bg-primary/10'
                : 'border border-slate-200/70 bg-white/60 dark:border-white/10 dark:bg-white/5'
            }`}
          >
            <SectionEyebrow>诊断记录</SectionEyebrow>
            <div className="mt-2 text-xl font-black text-slate-900 dark:text-white">诊断历史</div>
            <div className="mt-2 text-sm text-slate-500 dark:text-white/45">查看模板、结果得分和完成时间。</div>
          </button>
          <button
            type="button"
            onClick={() => setActiveTab('training')}
            className={`rounded-[1.8rem] px-5 py-4 text-left transition-all ${
              activeTab === 'training'
                ? 'border border-primary/30 bg-primary/10'
                : 'border border-slate-200/70 bg-white/60 dark:border-white/10 dark:bg-white/5'
            }`}
          >
            <SectionEyebrow>训练记录</SectionEyebrow>
            <div className="mt-2 text-xl font-black text-slate-900 dark:text-white">训练历史</div>
            <div className="mt-2 text-sm text-slate-500 dark:text-white/45">查看模式、正确率和风险词复习建议。</div>
          </button>
          <button
            type="button"
            onClick={() => setActiveTab('assessment')}
            className={`rounded-[1.8rem] px-5 py-4 text-left transition-all ${
              activeTab === 'assessment'
                ? 'border border-primary/30 bg-primary/10'
                : 'border border-slate-200/70 bg-white/60 dark:border-white/10 dark:bg-white/5'
            }`}
          >
            <SectionEyebrow>通用测评</SectionEyebrow>
            <div className="mt-2 text-xl font-black text-slate-900 dark:text-white">测评历史</div>
            <div className="mt-2 text-sm text-slate-500 dark:text-white/45">查看整卷测评、继续作答与题目级回看。</div>
          </button>
        </div>
      </section>

      {activeTab === 'diagnosis' ? (
        <div className="grid gap-8 xl:grid-cols-[1.05fr_0.95fr]">
          <section className="space-y-6 rounded-[2.5rem] liquid-glass-panel p-8">
            <div className="flex flex-wrap items-center gap-3">
              <SectionEyebrow>筛选</SectionEyebrow>
              <select
                value={diagnosisStatus}
                onChange={(event) => setDiagnosisStatus(event.target.value)}
                className="native-select rounded-full border border-slate-200 bg-white/70 px-4 py-2 text-sm dark:border-white/10 dark:bg-white/5"
              >
                <option value="ALL">全部状态</option>
                <option value="IN_PROGRESS">进行中</option>
                <option value="COMPLETED">已完成</option>
              </select>
            </div>

            {diagnosisHistoryQuery.isLoading ? (
              <HistoryStatePanel
                kind="loading"
                title="正在同步诊断记录"
                description="系统正在拉取最近的诊断历史，当前筛选条件已经保留。"
                impact="你暂时不能查看最新的诊断记录，但不会影响其他页面使用。"
                nextStep="请稍等片刻；如果长时间没有结果，可手动重新获取。"
              />
            ) : diagnosisHistoryQuery.error ? (
              <HistoryErrorPanel
                error={diagnosisHistoryQuery.error}
                resourceLabel="诊断记录"
                taskLabel="查看诊断历史"
                onRetry={() => void diagnosisHistoryQuery.refetch()}
              />
            ) : !diagnosisData?.records.length ? (
              <HistoryStatePanel
                kind="empty"
                title="当前筛选下还没有诊断记录"
                description="系统已经完成查询，但这组条件下没有可展示的诊断历史。"
                impact="这不会影响你继续开始新的诊断，或查看其他类型的学习记录。"
                nextStep="你可以切换筛选条件，或直接返回诊断页开始新的任务。"
              />
            ) : (
              <div className="space-y-4">
                {diagnosisData.records.map((record) => (
                  <div
                    key={record.sessionId}
                    className={`rounded-[1.8rem] border p-5 ${
                      selectedDiagnosisSessionId === record.sessionId
                        ? 'border-primary/30 bg-primary/5'
                        : 'border-slate-200/70 bg-white/60 dark:border-white/10 dark:bg-white/5'
                    }`}
                  >
                    <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                      <div>
                        <StatusBadge label={diagnosisSessionStatusLabel(record.status)} tone={diagnosisSessionStatusTone(record.status)} />
                        <div className="mt-2 text-xl font-black text-slate-900 dark:text-white">{record.templateName || `模板 #${record.templateId}`}</div>
                        <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                          开始于 {formatDateTime(record.startedAt)} · 完成于 {formatDateTime(record.completedAt)}
                        </div>
                        <div className="mt-3 flex flex-wrap gap-3 text-sm text-slate-500 dark:text-white/45">
                          <span>正迁移 {formatMaybePercent(record.positiveTransferScore, 1)}</span>
                          <span>负迁移 {formatMaybePercent(record.negativeTransferRisk, 1)}</span>
                          <span>正确率 {formatMaybePercent(record.overallAccuracy, 1)}</span>
                        </div>
                      </div>
                      <div className="flex flex-wrap gap-3">
                        {record.status === 'IN_PROGRESS' ? (
                          <button
                            type="button"
                            onClick={() => navigate('/diagnosis')}
                            className="btn-liquid px-5 py-3 text-white"
                          >
                            继续诊断
                          </button>
                        ) : (
                          <button
                            type="button"
                            onClick={() => setSelectedDiagnosisSessionId(record.sessionId)}
                            className="rounded-full border border-slate-200 px-5 py-3 text-sm font-bold dark:border-white/10"
                          >
                            查看详情
                          </button>
                        )}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}

            <div className="flex items-center justify-between text-sm text-slate-500 dark:text-white/45">
              <span>
                第 {diagnosisPageNo}/{totalPages(diagnosisData?.total)} 页
              </span>
              <div className="flex gap-3">
                <button
                  type="button"
                  onClick={() => setDiagnosisPageNo((page) => Math.max(1, page - 1))}
                  disabled={diagnosisPageNo === 1}
                  className="rounded-full border border-slate-200 px-4 py-2 disabled:opacity-50 dark:border-white/10"
                >
                  上一页
                </button>
                <button
                  type="button"
                  onClick={() => setDiagnosisPageNo((page) => Math.min(totalPages(diagnosisData?.total), page + 1))}
                  disabled={diagnosisPageNo >= totalPages(diagnosisData?.total)}
                  className="rounded-full border border-slate-200 px-4 py-2 disabled:opacity-50 dark:border-white/10"
                >
                  下一页
                </button>
              </div>
            </div>
          </section>

          <section className="space-y-6 rounded-[2.5rem] liquid-glass-panel p-8">
            <SectionEyebrow>诊断详情</SectionEyebrow>
            {selectedDiagnosisSessionId === null ? (
              <HistoryStatePanel
                kind="empty"
                title="先选择一条诊断记录"
                description="详细结果区会在你选中一条已完成诊断后显示。"
                impact="当前不会影响左侧记录浏览，但你还不能查看题目级回看和风险词对。"
                nextStep="从左侧选择一条已完成记录，即可在这里继续查看。"
              />
            ) : diagnosisDetailQuery.isLoading ? (
              <HistoryStatePanel
                kind="loading"
                title="正在整理诊断结果"
                description="系统正在加载这次诊断的指标、风险词对和题目回看。"
                impact="你暂时不能查看这条记录的详细结果，但左侧历史列表仍可继续切换。"
                nextStep="请稍等片刻；如果长时间没有结果，可手动重新获取。"
              />
            ) : diagnosisDetailQuery.error ? (
              <HistoryErrorPanel
                error={diagnosisDetailQuery.error}
                resourceLabel="诊断结果"
                taskLabel="查看诊断详情"
                onRetry={() => void diagnosisDetailQuery.refetch()}
              />
            ) : diagnosisDetailQuery.data ? (
              <>
                <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                  <div>
                    <div className="text-2xl font-black text-slate-900 dark:text-white">{diagnosisDetailQuery.data.templateName}</div>
                    <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                      完成于 {formatDateTime(diagnosisDetailQuery.data.completedAt)}
                    </div>
                  </div>
                  <div className="flex flex-wrap gap-3">
                    <button
                      type="button"
                      onClick={() =>
                        navigate(
                          buildTrainingHref({
                            source: 'history-diagnosis',
                            diagnosisSummaryId: diagnosisDetailQuery.data.summaryId,
                          })
                        )
                      }
                      className="btn-liquid px-5 py-3 text-white"
                    >
                      基于结果开始训练
                    </button>
                    <button
                      type="button"
                      onClick={() => navigate('/analytics')}
                      className="rounded-full border border-slate-200 px-5 py-3 text-sm font-bold dark:border-white/10"
                    >
                      查看学情分析
                    </button>
                  </div>
                </div>
                <div className="grid gap-4 md:grid-cols-2">
                  <div className="rounded-[1.8rem] border border-slate-200/70 bg-white/60 p-4 dark:border-white/10 dark:bg-white/5">
                    <div className="text-xs uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">正迁移得分</div>
                    <div className="mt-2 text-3xl font-black text-slate-900 dark:text-white">
                      {formatMaybePercent(diagnosisDetailQuery.data.metrics.positiveTransferScore, 1)}
                    </div>
                  </div>
                  <div className="rounded-[1.8rem] border border-slate-200/70 bg-white/60 p-4 dark:border-white/10 dark:bg-white/5">
                    <div className="text-xs uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">平均反应时</div>
                    <div className="mt-2 text-3xl font-black text-slate-900 dark:text-white">
                      {formatMs(diagnosisDetailQuery.data.metrics.averageReactionTime)}
                    </div>
                  </div>
                  <div className="rounded-[1.8rem] border border-slate-200/70 bg-white/60 p-4 dark:border-white/10 dark:bg-white/5">
                    <div className="text-xs uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">负迁移风险</div>
                    <div className="mt-2 text-3xl font-black text-slate-900 dark:text-white">
                      {formatMaybePercent(diagnosisDetailQuery.data.metrics.negativeTransferRisk, 1)}
                    </div>
                  </div>
                  <div className="rounded-[1.8rem] border border-slate-200/70 bg-white/60 p-4 dark:border-white/10 dark:bg-white/5">
                    <div className="text-xs uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">总体正确率</div>
                    <div className="mt-2 text-3xl font-black text-slate-900 dark:text-white">
                      {formatMaybePercent(diagnosisDetailQuery.data.metrics.overallAccuracy, 1)}
                    </div>
                  </div>
                </div>
                <div className="space-y-3">
                  <div className="text-sm font-bold text-slate-900 dark:text-white">高风险词对</div>
                  {diagnosisDetailQuery.data.highRiskLexicalPairs.length ? (
                    diagnosisDetailQuery.data.highRiskLexicalPairs.slice(0, 5).map((pair) => (
                      <div
                        key={pair.lexicalPairId}
                        className="rounded-[1.6rem] border border-slate-200/70 bg-white/60 p-4 dark:border-white/10 dark:bg-white/5"
                      >
                        <div className="font-black text-slate-900 dark:text-white">
                          {pair.englishWord} / {pair.frenchWord}
                        </div>
                        <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                          {lexicalPairTypeLabel(pair.lexicalPairType)} · 风险 {formatMaybePercent(pair.riskScore, 1)} · 主导错误 {errorTypeLabel(pair.dominantErrorType)}
                        </div>
                      </div>
                    ))
                  ) : (
                    <div className="text-sm text-slate-500 dark:text-white/45">本次没有高风险词对。</div>
                  )}
                </div>
                <div className="space-y-3">
                  <div className="text-sm font-bold text-slate-900 dark:text-white">题目回看</div>
                  {diagnosisDetailQuery.data.items.length ? (
                    diagnosisDetailQuery.data.items.map((item) => (
                      <DiagnosisHistoryItemReviewCard key={item.itemResultId} item={item} />
                    ))
                  ) : (
                    <div className="text-sm text-slate-500 dark:text-white/45">本次没有返回题目级结果。</div>
                  )}
                </div>
              </>
            ) : null}
          </section>
        </div>
      ) : activeTab === 'training' ? (
        <div className="grid gap-8 xl:grid-cols-[1.05fr_0.95fr]">
          <section className="space-y-6 rounded-[2.5rem] liquid-glass-panel p-8">
            <div className="flex flex-wrap items-center gap-3">
              <SectionEyebrow>筛选</SectionEyebrow>
              <select
                value={trainingStatus}
                onChange={(event) => setTrainingStatus(event.target.value)}
                className="native-select rounded-full border border-slate-200 bg-white/70 px-4 py-2 text-sm dark:border-white/10 dark:bg-white/5"
              >
                <option value="ALL">全部状态</option>
                <option value="IN_PROGRESS">进行中</option>
                <option value="COMPLETED">已完成</option>
              </select>
            </div>

            {trainingHistoryQuery.isLoading ? (
              <HistoryStatePanel
                kind="loading"
                title="正在同步训练记录"
                description="系统正在拉取最近的训练历史，当前筛选条件已经保留。"
                impact="你暂时不能查看最新的训练记录，但不会影响继续训练。"
                nextStep="请稍等片刻；如果长时间没有结果，可手动重新获取。"
              />
            ) : trainingHistoryQuery.error ? (
              <HistoryErrorPanel
                error={trainingHistoryQuery.error}
                resourceLabel="训练记录"
                taskLabel="查看训练历史"
                onRetry={() => void trainingHistoryQuery.refetch()}
              />
            ) : !trainingData?.records.length ? (
              <HistoryStatePanel
                kind="empty"
                title="当前筛选下还没有训练记录"
                description="系统已经完成查询，但这组条件下没有可展示的训练历史。"
                impact="这不会影响你开始新的训练，或查看其他学习记录。"
                nextStep="你可以切换筛选条件，或直接返回训练页继续学习。"
              />
            ) : (
              <div className="space-y-4">
                {trainingData.records.map((record) => (
                  <div
                    key={record.sessionId}
                    className={`rounded-[1.8rem] border p-5 ${
                      selectedTrainingSessionId === record.sessionId
                        ? 'border-primary/30 bg-primary/5'
                        : 'border-slate-200/70 bg-white/60 dark:border-white/10 dark:bg-white/5'
                    }`}
                  >
                    <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                      <div>
                        <StatusBadge label={trainingSessionStatusLabel(record.status)} tone={trainingSessionStatusTone(record.status)} />
                        <div className="mt-2 text-xl font-black text-slate-900 dark:text-white">{trainingModeLabel(record.mode)}</div>
                        <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                          开始于 {formatDateTime(record.startedAt)} · 完成于 {formatDateTime(record.completedAt)}
                        </div>
                        <div className="mt-3 flex flex-wrap gap-3 text-sm text-slate-500 dark:text-white/45">
                          <span>题量 {record.answeredItems}/{record.totalItems}</span>
                          <span>当前序号 {record.currentItemOrder ?? '--'}</span>
                        </div>
                      </div>
                      <div className="flex flex-wrap gap-3">
                        {record.status === 'IN_PROGRESS' ? (
                          <button
                            type="button"
                            onClick={() => navigate('/training')}
                            className="btn-liquid px-5 py-3 text-white"
                          >
                            继续训练
                          </button>
                        ) : (
                          <button
                            type="button"
                            onClick={() => setSelectedTrainingSessionId(record.sessionId)}
                            className="rounded-full border border-slate-200 px-5 py-3 text-sm font-bold dark:border-white/10"
                          >
                            查看总结
                          </button>
                        )}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}

            <div className="flex items-center justify-between text-sm text-slate-500 dark:text-white/45">
              <span>
                第 {trainingPageNo}/{totalPages(trainingData?.total)} 页
              </span>
              <div className="flex gap-3">
                <button
                  type="button"
                  onClick={() => setTrainingPageNo((page) => Math.max(1, page - 1))}
                  disabled={trainingPageNo === 1}
                  className="rounded-full border border-slate-200 px-4 py-2 disabled:opacity-50 dark:border-white/10"
                >
                  上一页
                </button>
                <button
                  type="button"
                  onClick={() => setTrainingPageNo((page) => Math.min(totalPages(trainingData?.total), page + 1))}
                  disabled={trainingPageNo >= totalPages(trainingData?.total)}
                  className="rounded-full border border-slate-200 px-4 py-2 disabled:opacity-50 dark:border-white/10"
                >
                  下一页
                </button>
              </div>
            </div>
          </section>

          <section className="space-y-6 rounded-[2.5rem] liquid-glass-panel p-8">
            <SectionEyebrow>训练详情</SectionEyebrow>
            {selectedTrainingSessionId === null ? (
              <HistoryStatePanel
                kind="empty"
                title="先选择一条训练记录"
                description="训练总结会在你选中一条已完成训练后显示。"
                impact="当前不会影响左侧记录浏览，但你还不能查看复习建议和题目回看。"
                nextStep="从左侧选择一条已完成记录，即可在这里继续查看。"
              />
            ) : trainingDetailQuery.isLoading ? (
              <HistoryStatePanel
                kind="loading"
                title="正在整理训练总结"
                description="系统正在加载这次训练的表现指标、复习建议和题目回看。"
                impact="你暂时不能查看这条训练记录的详细总结，但左侧列表仍可继续切换。"
                nextStep="请稍等片刻；如果长时间没有结果，可手动重新获取。"
              />
            ) : trainingDetailQuery.error ? (
              <HistoryErrorPanel
                error={trainingDetailQuery.error}
                resourceLabel="训练总结"
                taskLabel="查看训练详情"
                onRetry={() => void trainingDetailQuery.refetch()}
              />
            ) : trainingDetailQuery.data ? (
              <>
                <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                  <div>
                    <div className="text-2xl font-black text-slate-900 dark:text-white">{trainingModeLabel(trainingDetailQuery.data.mode)}</div>
                    <div className="mt-2 text-sm text-slate-500 dark:text-white/45">{trainingDetailQuery.data.improvementHint}</div>
                  </div>
                  <div className="flex flex-wrap gap-3">
                    <button
                      type="button"
                      onClick={() =>
                        navigate(
                          trainingDetailQuery.data.nextRecommendedMode
                            ? buildTrainingHref({
                                mode: trainingDetailQuery.data.nextRecommendedMode,
                                source: 'history-training',
                              })
                            : '/training'
                        )
                      }
                      className="btn-liquid px-5 py-3 text-white"
                    >
                      继续下一推荐训练
                    </button>
                    <button
                      type="button"
                      onClick={() => navigate('/errors')}
                      className="rounded-full border border-slate-200 px-5 py-3 text-sm font-bold dark:border-white/10"
                    >
                      去做错题复习
                    </button>
                  </div>
                </div>
                <div className="grid gap-4 md:grid-cols-3">
                  <div className="rounded-[1.8rem] border border-slate-200/70 bg-white/60 p-4 dark:border-white/10 dark:bg-white/5">
                    <div className="text-xs uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">正确率</div>
                    <div className="mt-2 text-3xl font-black text-slate-900 dark:text-white">
                      {formatMaybePercent(trainingDetailQuery.data.accuracy, 1)}
                    </div>
                  </div>
                  <div className="rounded-[1.8rem] border border-slate-200/70 bg-white/60 p-4 dark:border-white/10 dark:bg-white/5">
                    <div className="text-xs uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">平均反应时</div>
                    <div className="mt-2 text-3xl font-black text-slate-900 dark:text-white">
                      {formatMs(trainingDetailQuery.data.averageReactionTime)}
                    </div>
                  </div>
                  <div className="rounded-[1.8rem] border border-slate-200/70 bg-white/60 p-4 dark:border-white/10 dark:bg-white/5">
                    <div className="text-xs uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">下一推荐模式</div>
                    <div className="mt-2 text-2xl font-black text-slate-900 dark:text-white">
                      {trainingModeLabel(trainingDetailQuery.data.nextRecommendedMode)}
                    </div>
                  </div>
                </div>
                <div className="space-y-3">
                  <div className="text-sm font-bold text-slate-900 dark:text-white">建议复习词对</div>
                  {trainingDetailQuery.data.riskWordsToReview.length ? (
                    trainingDetailQuery.data.riskWordsToReview.map((item) => (
                      <div
                        key={item.lexicalPairId}
                        className="rounded-[1.6rem] border border-slate-200/70 bg-white/60 p-4 dark:border-white/10 dark:bg-white/5"
                      >
                        <div className="font-black text-slate-900 dark:text-white">
                          {item.englishWord} / {item.frenchWord}
                        </div>
                        <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                          {lexicalPairTypeLabel(item.lexicalPairType)} · {riskLevelLabel(item.riskLevel)} · {item.reason}
                        </div>
                      </div>
                    ))
                  ) : (
                    <div className="text-sm text-slate-500 dark:text-white/45">本次没有额外复习词对。</div>
                  )}
                </div>
                <div className="space-y-3">
                  <div className="text-sm font-bold text-slate-900 dark:text-white">题目回看</div>
                  {trainingDetailQuery.data.items.length ? (
                    trainingDetailQuery.data.items.map((item) => (
                      <TrainingHistoryItemReviewCard key={item.itemResultId} item={item} />
                    ))
                  ) : (
                    <div className="text-sm text-slate-500 dark:text-white/45">本次没有返回题目级结果。</div>
                  )}
                </div>
              </>
            ) : null}
          </section>
        </div>
      ) : (
        <div className="grid gap-8 xl:grid-cols-[1.05fr_0.95fr]">
          <section className="space-y-6 rounded-[2.5rem] liquid-glass-panel p-8">
            <div className="flex flex-wrap items-center gap-3">
              <SectionEyebrow>筛选</SectionEyebrow>
              <select
                value={assessmentStatus}
                onChange={(event) => setAssessmentStatus(event.target.value)}
                className="native-select rounded-full border border-slate-200 bg-white/70 px-4 py-2 text-sm dark:border-white/10 dark:bg-white/5"
              >
                <option value="ALL">全部状态</option>
                <option value="IN_PROGRESS">进行中</option>
                <option value="SUBMITTED">已提交</option>
              </select>
            </div>

            {assessmentHistoryQuery.isLoading ? (
              <HistoryStatePanel
                kind="loading"
                title="正在同步测评记录"
                description="系统正在拉取最近的通用测评历史，当前筛选条件已经保留。"
                impact="你暂时不能查看最新的测评记录，但不会影响继续作答。"
                nextStep="请稍等片刻；如果长时间没有结果，可手动重新获取。"
              />
            ) : assessmentHistoryQuery.error ? (
              <HistoryErrorPanel
                error={assessmentHistoryQuery.error}
                resourceLabel="测评记录"
                taskLabel="查看测评历史"
                onRetry={() => void assessmentHistoryQuery.refetch()}
              />
            ) : !assessmentData?.records.length ? (
              <HistoryStatePanel
                kind="empty"
                title="当前筛选下还没有测评记录"
                description="系统已经完成查询，但这组条件下没有可展示的测评历史。"
                impact="这不会影响你继续开始新的测评，或查看诊断与训练记录。"
                nextStep="你可以切换筛选条件，或直接回到测评页继续作答。"
              />
            ) : (
              <div className="space-y-4">
                {assessmentData.records.map((record) => (
                  <div
                    key={record.attemptId}
                    className={`rounded-[1.8rem] border p-5 ${
                      selectedAssessmentAttemptId === record.attemptId
                        ? 'border-primary/30 bg-primary/5'
                        : 'border-slate-200/70 bg-white/60 dark:border-white/10 dark:bg-white/5'
                    }`}
                  >
                    <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                      <div>
                        <StatusBadge label={assessmentAttemptStatusLabel(record.status)} tone={assessmentAttemptStatusTone(record.status)} />
                        <div className="mt-2 text-xl font-black text-slate-900 dark:text-white">{record.title}</div>
                        <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                          {record.className} · 开始于 {formatDateTime(record.startedAt)} · 提交于 {formatDateTime(record.submittedAt)}
                        </div>
                        <div className="mt-3 flex flex-wrap gap-3 text-sm text-slate-500 dark:text-white/45">
                          <span>进度 {record.answeredCount}/{record.questionCount}</span>
                          <span>得分 {record.totalScore ?? '--'}</span>
                          <span>最后保存 {formatDateTime(record.lastSavedAt)}</span>
                        </div>
                      </div>
                      <div className="flex flex-wrap gap-3">
                        {record.status === 'IN_PROGRESS' ? (
                          <button
                            type="button"
                            onClick={() => navigate(`/assessments/attempts/${record.attemptId}`)}
                            className="btn-liquid px-5 py-3 text-white"
                          >
                            继续作答
                          </button>
                        ) : (
                          <button
                            type="button"
                            onClick={() => setSelectedAssessmentAttemptId(record.attemptId)}
                            className="rounded-full border border-slate-200 px-5 py-3 text-sm font-bold dark:border-white/10"
                          >
                            查看结果
                          </button>
                        )}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}

            <div className="flex items-center justify-between text-sm text-slate-500 dark:text-white/45">
              <span>
                第 {assessmentPageNo}/{totalPages(assessmentData?.total)} 页
              </span>
              <div className="flex gap-3">
                <button
                  type="button"
                  onClick={() => setAssessmentPageNo((page) => Math.max(1, page - 1))}
                  disabled={assessmentPageNo === 1}
                  className="rounded-full border border-slate-200 px-4 py-2 disabled:opacity-50 dark:border-white/10"
                >
                  上一页
                </button>
                <button
                  type="button"
                  onClick={() => setAssessmentPageNo((page) => Math.min(totalPages(assessmentData?.total), page + 1))}
                  disabled={assessmentPageNo >= totalPages(assessmentData?.total)}
                  className="rounded-full border border-slate-200 px-4 py-2 disabled:opacity-50 dark:border-white/10"
                >
                  下一页
                </button>
              </div>
            </div>
          </section>

          <section className="space-y-6 rounded-[2.5rem] liquid-glass-panel p-8">
            <SectionEyebrow>测评详情</SectionEyebrow>
            {selectedAssessmentAttemptId === null ? (
              <HistoryStatePanel
                kind="empty"
                title="先选择一条测评记录"
                description="结果区会在你选中一条测评记录后显示。"
                impact="当前不会影响左侧列表浏览，但你还不能查看成绩和题目回看。"
                nextStep="从左侧选择一条记录，即可在这里继续查看。"
              />
            ) : selectedAssessmentRecord?.status === 'IN_PROGRESS' ? (
              <div className="space-y-4">
                <div className="rounded-[1.8rem] border border-amber-500/20 bg-amber-500/10 px-4 py-4 text-sm text-amber-700 dark:text-amber-300">
                  这份测评仍在进行中，学生需要回到测评页继续作答。
                </div>
                <div className="grid gap-3 text-sm text-slate-500 dark:text-white/45">
                  <div>开始时间：{formatDateTime(selectedAssessmentRecord.startedAt)}</div>
                  <div>最后保存：{formatDateTime(selectedAssessmentRecord.lastSavedAt)}</div>
                  <div>作答时限：{formatDateTime(selectedAssessmentRecord.expiresAt)}</div>
                  <div>当前进度：{selectedAssessmentRecord.answeredCount}/{selectedAssessmentRecord.questionCount}</div>
                </div>
                <button
                  type="button"
                  onClick={() => navigate(`/assessments/attempts/${selectedAssessmentRecord.attemptId}`)}
                  className="btn-liquid px-5 py-3 text-white"
                >
                  回到测评继续作答
                </button>
              </div>
            ) : assessmentDetailQuery.isLoading ? (
              <HistoryStatePanel
                kind="loading"
                title="正在整理测评结果"
                description="系统正在加载这次测评的得分、作答表现和题目回看。"
                impact="你暂时不能查看这条测评记录的详细结果，但左侧列表仍可继续切换。"
                nextStep="请稍等片刻；如果长时间没有结果，可手动重新获取。"
              />
            ) : assessmentDetailQuery.error ? (
              <HistoryErrorPanel
                error={assessmentDetailQuery.error}
                resourceLabel="测评结果"
                taskLabel="查看测评详情"
                onRetry={() => void assessmentDetailQuery.refetch()}
              />
            ) : assessmentDetailQuery.data ? (
              <>
                <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                  <div>
                    <div className="text-2xl font-black text-slate-900 dark:text-white">{assessmentDetailQuery.data.paperTitle}</div>
                    <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                      {assessmentDetailQuery.data.className} · 提交于 {formatDateTime(assessmentDetailQuery.data.submittedAt)}
                    </div>
                  </div>
                  <button
                    type="button"
                    onClick={() => navigate(`/assessments/attempts/${assessmentDetailQuery.data.attemptId}/result`)}
                    className="rounded-full border border-slate-200 px-5 py-3 text-sm font-bold dark:border-white/10"
                  >
                    打开独立结果页
                  </button>
                </div>
                <div className="grid gap-4 md:grid-cols-3">
                  <div className="rounded-[1.8rem] border border-slate-200/70 bg-white/60 p-4 dark:border-white/10 dark:bg-white/5">
                    <div className="text-xs uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">总分</div>
                    <div className="mt-2 text-3xl font-black text-slate-900 dark:text-white">{assessmentDetailQuery.data.totalScore}</div>
                  </div>
                  <div className="rounded-[1.8rem] border border-slate-200/70 bg-white/60 p-4 dark:border-white/10 dark:bg-white/5">
                    <div className="text-xs uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">答对率</div>
                    <div className="mt-2 text-3xl font-black text-slate-900 dark:text-white">
                      {assessmentDetailQuery.data.questionCount
                        ? `${Math.round((assessmentDetailQuery.data.correctCount / assessmentDetailQuery.data.questionCount) * 100)}%`
                        : '0%'}
                    </div>
                  </div>
                  <div className="rounded-[1.8rem] border border-slate-200/70 bg-white/60 p-4 dark:border-white/10 dark:bg-white/5">
                    <div className="text-xs uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">作答题数</div>
                    <div className="mt-2 text-3xl font-black text-slate-900 dark:text-white">
                      {assessmentDetailQuery.data.answeredCount}/{assessmentDetailQuery.data.questionCount}
                    </div>
                  </div>
                </div>
                {assessmentDetailQuery.data.instructionsText && (
                  <div className="rounded-[1.6rem] border border-dashed border-slate-200/80 px-4 py-4 text-sm text-slate-600 dark:border-white/10 dark:text-white/60">
                    {assessmentDetailQuery.data.instructionsText}
                  </div>
                )}
                <div className="space-y-3">
                  <div className="text-sm font-bold text-slate-900 dark:text-white">题目回看</div>
                  {assessmentDetailQuery.data.questions.length ? (
                    assessmentDetailQuery.data.questions.map((item) => (
                      <AssessmentHistoryItemReviewCard key={item.answerId} item={item} />
                    ))
                  ) : (
                    <div className="text-sm text-slate-500 dark:text-white/45">本次没有返回题目级结果。</div>
                  )}
                </div>
              </>
            ) : null}
          </section>
        </div>
      )}
    </div>
  );
};

export default HistoryPage;
