import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { PageHeader, PanelSkeleton } from '@/components/common';
import { diagnosisSessionService, trainingService } from '@/lib/services';
import { formatDateTime, formatMaybePercent, formatMs, lexicalPairTypeLabel } from '@/lib/format';

type HistoryTab = 'diagnosis' | 'training';

const pageSize = 10;

function totalPages(total = 0): number {
  return Math.max(1, Math.ceil(total / pageSize));
}

const HistoryPage: React.FC = () => {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = React.useState<HistoryTab>('diagnosis');
  const [diagnosisStatus, setDiagnosisStatus] = React.useState('ALL');
  const [trainingStatus, setTrainingStatus] = React.useState('ALL');
  const [diagnosisPageNo, setDiagnosisPageNo] = React.useState(1);
  const [trainingPageNo, setTrainingPageNo] = React.useState(1);
  const [selectedDiagnosisSessionId, setSelectedDiagnosisSessionId] = React.useState<number | null>(null);
  const [selectedTrainingSessionId, setSelectedTrainingSessionId] = React.useState<number | null>(null);

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

  React.useEffect(() => {
    setDiagnosisPageNo(1);
  }, [diagnosisStatus]);

  React.useEffect(() => {
    setTrainingPageNo(1);
  }, [trainingStatus]);

  const diagnosisData = diagnosisHistoryQuery.data;
  const trainingData = trainingHistoryQuery.data;

  return (
    <div className="space-y-8 pb-20">
      <PageHeader
        title="学习历史"
        subtitle="统一查看诊断与训练记录；进行中的 session 会跳回对应页面继续，已完成记录可在当前页查看详情。"
      />

      <section className="rounded-[2.5rem] liquid-glass-panel p-4">
        <div className="grid gap-3 md:grid-cols-2">
          <button
            type="button"
            onClick={() => setActiveTab('diagnosis')}
            className={`rounded-[1.8rem] px-5 py-4 text-left transition-all ${
              activeTab === 'diagnosis'
                ? 'border border-primary/30 bg-primary/10'
                : 'border border-slate-200/70 bg-white/60 dark:border-white/10 dark:bg-white/5'
            }`}
          >
            <div className="text-[10px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">diagnosis history</div>
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
            <div className="text-[10px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">training history</div>
            <div className="mt-2 text-xl font-black text-slate-900 dark:text-white">训练历史</div>
            <div className="mt-2 text-sm text-slate-500 dark:text-white/45">查看模式、正确率和风险词复习建议。</div>
          </button>
        </div>
      </section>

      {activeTab === 'diagnosis' ? (
        <div className="grid gap-8 xl:grid-cols-[1.05fr_0.95fr]">
          <section className="rounded-[2.5rem] liquid-glass-panel p-8 space-y-6">
            <div className="flex flex-wrap items-center gap-3">
              <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">filters</div>
              <select
                value={diagnosisStatus}
                onChange={(event) => setDiagnosisStatus(event.target.value)}
                className="rounded-full border border-slate-200 bg-white/70 px-4 py-2 text-sm dark:border-white/10 dark:bg-white/5"
              >
                <option value="ALL">全部状态</option>
                <option value="IN_PROGRESS">进行中</option>
                <option value="COMPLETED">已完成</option>
              </select>
            </div>

            {diagnosisHistoryQuery.isLoading ? (
              <PanelSkeleton />
            ) : diagnosisHistoryQuery.error ? (
              <div className="rounded-2xl border border-rose-500/20 bg-rose-500/5 px-4 py-3 text-sm text-rose-500">
                {diagnosisHistoryQuery.error.message}
              </div>
            ) : !diagnosisData?.records.length ? (
              <div className="rounded-2xl border border-slate-200 bg-white/70 px-4 py-8 text-sm text-slate-500 dark:border-white/10 dark:bg-white/5 dark:text-white/45">
                当前没有匹配的诊断记录。
              </div>
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
                        <div className="text-xs uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">{record.status}</div>
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

          <section className="rounded-[2.5rem] liquid-glass-panel p-8 space-y-6">
            <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">diagnosis detail</div>
            {selectedDiagnosisSessionId === null ? (
              <div className="rounded-2xl border border-slate-200 bg-white/70 px-4 py-8 text-sm text-slate-500 dark:border-white/10 dark:bg-white/5 dark:text-white/45">
                选择一条已完成诊断记录后，在这里查看详细结果。
              </div>
            ) : diagnosisDetailQuery.isLoading ? (
              <PanelSkeleton />
            ) : diagnosisDetailQuery.error ? (
              <div className="rounded-2xl border border-rose-500/20 bg-rose-500/5 px-4 py-3 text-sm text-rose-500">
                {diagnosisDetailQuery.error.message}
              </div>
            ) : diagnosisDetailQuery.data ? (
              <>
                <div>
                  <div className="text-2xl font-black text-slate-900 dark:text-white">{diagnosisDetailQuery.data.templateName}</div>
                  <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                    完成于 {formatDateTime(diagnosisDetailQuery.data.completedAt)}
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
                          {lexicalPairTypeLabel(pair.lexicalPairType)} · 风险 {formatMaybePercent(pair.riskScore, 1)} · 主导错误 {pair.dominantErrorType}
                        </div>
                      </div>
                    ))
                  ) : (
                    <div className="text-sm text-slate-500 dark:text-white/45">本次没有高风险词对。</div>
                  )}
                </div>
              </>
            ) : null}
          </section>
        </div>
      ) : (
        <div className="grid gap-8 xl:grid-cols-[1.05fr_0.95fr]">
          <section className="rounded-[2.5rem] liquid-glass-panel p-8 space-y-6">
            <div className="flex flex-wrap items-center gap-3">
              <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">filters</div>
              <select
                value={trainingStatus}
                onChange={(event) => setTrainingStatus(event.target.value)}
                className="rounded-full border border-slate-200 bg-white/70 px-4 py-2 text-sm dark:border-white/10 dark:bg-white/5"
              >
                <option value="ALL">全部状态</option>
                <option value="IN_PROGRESS">进行中</option>
                <option value="COMPLETED">已完成</option>
              </select>
            </div>

            {trainingHistoryQuery.isLoading ? (
              <PanelSkeleton />
            ) : trainingHistoryQuery.error ? (
              <div className="rounded-2xl border border-rose-500/20 bg-rose-500/5 px-4 py-3 text-sm text-rose-500">
                {trainingHistoryQuery.error.message}
              </div>
            ) : !trainingData?.records.length ? (
              <div className="rounded-2xl border border-slate-200 bg-white/70 px-4 py-8 text-sm text-slate-500 dark:border-white/10 dark:bg-white/5 dark:text-white/45">
                当前没有匹配的训练记录。
              </div>
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
                        <div className="text-xs uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">{record.status}</div>
                        <div className="mt-2 text-xl font-black text-slate-900 dark:text-white">{record.mode}</div>
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

          <section className="rounded-[2.5rem] liquid-glass-panel p-8 space-y-6">
            <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">training detail</div>
            {selectedTrainingSessionId === null ? (
              <div className="rounded-2xl border border-slate-200 bg-white/70 px-4 py-8 text-sm text-slate-500 dark:border-white/10 dark:bg-white/5 dark:text-white/45">
                选择一条已完成训练记录后，在这里查看训练总结。
              </div>
            ) : trainingDetailQuery.isLoading ? (
              <PanelSkeleton />
            ) : trainingDetailQuery.error ? (
              <div className="rounded-2xl border border-rose-500/20 bg-rose-500/5 px-4 py-3 text-sm text-rose-500">
                {trainingDetailQuery.error.message}
              </div>
            ) : trainingDetailQuery.data ? (
              <>
                <div>
                  <div className="text-2xl font-black text-slate-900 dark:text-white">{trainingDetailQuery.data.mode}</div>
                  <div className="mt-2 text-sm text-slate-500 dark:text-white/45">{trainingDetailQuery.data.improvementHint}</div>
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
                      {trainingDetailQuery.data.nextRecommendedMode}
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
                          {lexicalPairTypeLabel(item.lexicalPairType)} · {item.riskLevel} · {item.reason}
                        </div>
                      </div>
                    ))
                  ) : (
                    <div className="text-sm text-slate-500 dark:text-white/45">本次没有额外复习词对。</div>
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
