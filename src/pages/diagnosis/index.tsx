import React from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Brain, CheckCircle2, ChevronRight, Timer } from 'lucide-react';
import { PageHeader } from '@/components/common';
import { EChart } from '@/components/common/EChart';
import { aiService, diagnosisSessionService, diagnosisTemplateService } from '@/lib/services';
import { buildRadarOption, formatDateTime, formatMaybePercent, formatMs, lexicalPairTypeLabel } from '@/lib/format';
import type { DiagnosisOptionViewVO } from '@/lib/contracts';

type Phase = 'boot' | 'select' | 'running' | 'result';

function inferSemanticMatch(option: DiagnosisOptionViewVO): boolean {
  const key = option.key.toLowerCase();
  const label = option.label.toLowerCase();
  if (key.includes('mismatch') || key.includes('false') || label.includes('不') || label.includes('diff')) {
    return false;
  }
  return true;
}

const DiagnosisPage: React.FC = () => {
  const queryClient = useQueryClient();
  const [phase, setPhase] = React.useState<Phase>('boot');
  const [sessionId, setSessionId] = React.useState<number | null>(null);
  const shownAtRef = React.useRef<number>(Date.now());

  const historyQuery = useQuery({
    queryKey: ['diagnosis-history', 'in-progress'],
    queryFn: () => diagnosisSessionService.listHistory({ pageNo: 1, pageSize: 1, status: 'IN_PROGRESS' }),
  });

  React.useEffect(() => {
    if (!historyQuery.data) {
      return;
    }
    const inProgress = historyQuery.data.records[0];
    if (inProgress?.sessionId) {
      setSessionId(inProgress.sessionId);
      shownAtRef.current = Date.now();
      setPhase('running');
    } else {
      setPhase('select');
    }
  }, [historyQuery.data]);

  React.useEffect(() => {
    if (historyQuery.error) {
      setPhase('select');
    }
  }, [historyQuery.error]);

  const templatesQuery = useQuery({
    queryKey: ['student-diagnosis-templates'],
    queryFn: () => diagnosisTemplateService.listPublished({ pageNo: 1, pageSize: 20 }),
    enabled: phase === 'select',
  });

  const createSessionMutation = useMutation({
    mutationFn: (templateId: number) => diagnosisSessionService.create(templateId),
    onSuccess: (created) => {
      setSessionId(created.sessionId);
      setPhase('running');
      shownAtRef.current = Date.now();
      void queryClient.invalidateQueries({ queryKey: ['diagnosis-history'] });
    },
  });

  const nextItemQuery = useQuery({
    queryKey: ['diagnosis-next-item', sessionId],
    queryFn: () => diagnosisSessionService.getNextItem(sessionId as number),
    enabled: phase === 'running' && !!sessionId,
  });

  const completeMutation = useMutation({
    mutationFn: (value: number) => diagnosisSessionService.complete(value),
    onSuccess: () => {
      setPhase('result');
      void queryClient.invalidateQueries({ queryKey: ['diagnosis-history'] });
      void queryClient.invalidateQueries({ queryKey: ['student-overview'] });
      void queryClient.invalidateQueries({ queryKey: ['recommended-training-plan'] });
    },
  });

  const submitAnswerMutation = useMutation({
    mutationFn: (payload: {
      itemResultId: number;
      selectedSemanticMatch?: boolean;
      selectedAnswerKey?: string;
      reactionTimeMs: number;
      hesitationTimeMs: number;
    }) => diagnosisSessionService.submitAnswer(sessionId as number, payload),
    onSuccess: async (progress) => {
      if (progress.completed || progress.answeredItems >= progress.totalItems) {
        await completeMutation.mutateAsync(progress.sessionId);
        return;
      }
      shownAtRef.current = Date.now();
      await nextItemQuery.refetch();
    },
  });

  const resultQuery = useQuery({
    queryKey: ['diagnosis-result', sessionId],
    queryFn: () => diagnosisSessionService.getResult(sessionId as number),
    enabled: phase === 'result' && !!sessionId,
  });

  const explanationQuery = useQuery({
    queryKey: ['diagnosis-explanation', sessionId],
    queryFn: () => aiService.explainDiagnosis(),
    enabled: phase === 'result' && !!sessionId,
    retry: false,
  });

  React.useEffect(() => {
    if (!sessionId || phase !== 'running') {
      return;
    }
    const persist = () => {
      const snapshot = nextItemQuery.data
        ? {
            sessionId,
            currentItemOrder: nextItemQuery.data.currentItemOrder,
            answeredItems: nextItemQuery.data.answeredItems,
            timestamp: new Date().toISOString(),
          }
        : { sessionId, timestamp: new Date().toISOString() };
      void diagnosisSessionService.saveProgress(sessionId, snapshot);
    };
    const onVisibilityChange = () => {
      if (document.hidden) {
        persist();
      }
    };
    window.addEventListener('beforeunload', persist);
    document.addEventListener('visibilitychange', onVisibilityChange);
    return () => {
      window.removeEventListener('beforeunload', persist);
      document.removeEventListener('visibilitychange', onVisibilityChange);
    };
  }, [nextItemQuery.data, phase, sessionId]);

  const currentItem = nextItemQuery.data?.item;
  const submitAnswer = async (option: DiagnosisOptionViewVO) => {
    if (!currentItem) {
      return;
    }
    const reactionTimeMs = Math.max(1, Date.now() - shownAtRef.current);
    const hesitationTimeMs = Math.max(0, reactionTimeMs - 1200);
    shownAtRef.current = Date.now();

    if (currentItem.taskType === 'REACTION_TIME') {
      await submitAnswerMutation.mutateAsync({
        itemResultId: currentItem.itemResultId,
        selectedSemanticMatch: inferSemanticMatch(option),
        reactionTimeMs,
        hesitationTimeMs,
      });
      return;
    }
    await submitAnswerMutation.mutateAsync({
      itemResultId: currentItem.itemResultId,
      selectedAnswerKey: option.key,
      reactionTimeMs,
      hesitationTimeMs,
    });
  };

  if (phase === 'boot' || historyQuery.isLoading) {
    return (
      <div className="min-h-[60vh] flex items-center justify-center">
        <div className="text-sm uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">loading diagnosis session</div>
      </div>
    );
  }

  if (phase === 'select') {
    return (
      <div className="space-y-8">
        <PageHeader title="智能诊断" subtitle="选择一个已发布模板，开始真实后端 session 流程。" />
        <section className="liquid-glass-panel rounded-[3rem] p-10 edge-light">
          <div className="max-w-3xl">
            <div className="inline-flex p-4 rounded-3xl bg-primary/10 border border-primary/20">
              <Brain size={32} className="text-primary" />
            </div>
            <h2 className="mt-6 text-4xl font-black text-slate-900 dark:text-white">开始一轮新的迁移诊断</h2>
            <p className="mt-4 text-slate-500 dark:text-white/50 leading-7">
              系统会根据模板生成真实的诊断题流，并在完成后直接写入 summary、训练计划和分析聚合。
            </p>
          </div>
        </section>

        {templatesQuery.error && (
          <div className="rounded-[2rem] border border-rose-500/20 bg-rose-500/5 p-6 text-rose-500">{templatesQuery.error.message}</div>
        )}

        {!templatesQuery.isLoading && !templatesQuery.data?.records.length && (
          <div className="rounded-[2rem] border border-slate-200 dark:border-white/10 p-8 text-slate-500 dark:text-white/45">
            当前没有已发布模板。请联系教师先发布模板。
          </div>
        )}

        <div className="grid lg:grid-cols-2 gap-6">
          {(templatesQuery.data?.records || []).map((template) => (
            <button
              key={template.id}
              type="button"
              onClick={() => createSessionMutation.mutate(template.id)}
              className="text-left liquid-glass rounded-[2.4rem] p-7 edge-light hover:border-primary/40 transition-all"
            >
              <div className="flex items-start justify-between gap-4">
                <div>
                  <div className="text-xs uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">{template.status}</div>
                  <div className="mt-3 text-2xl font-black text-slate-900 dark:text-white">{template.templateName}</div>
                  <div className="mt-3 text-sm leading-6 text-slate-500 dark:text-white/45">{template.description || '无额外描述'}</div>
                </div>
                <ChevronRight className="text-primary shrink-0" />
              </div>
              <div className="mt-6 flex gap-4 text-sm text-slate-500 dark:text-white/45">
                <span>{template.itemCount} 题</span>
                <span>{template.estimatedDurationMinutes} 分钟</span>
              </div>
            </button>
          ))}
        </div>
      </div>
    );
  }

  if (phase === 'running') {
    return (
      <div className="max-w-5xl mx-auto space-y-8">
        <PageHeader
          title="诊断进行中"
          subtitle={sessionId ? `Session #${sessionId}` : '正在加载当前题目'}
        />

        {nextItemQuery.error && (
          <div className="rounded-[2rem] border border-rose-500/20 bg-rose-500/5 p-6 text-rose-500">{nextItemQuery.error.message}</div>
        )}

        {nextItemQuery.isLoading || !currentItem ? (
          <div className="min-h-[360px] rounded-[2.8rem] liquid-glass-panel flex items-center justify-center">
            <div className="text-sm uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">loading next item</div>
          </div>
        ) : (
          <>
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3 text-sm text-slate-500 dark:text-white/45">
                <Timer size={16} />
                <span>
                  第 {nextItemQuery.data?.currentItemOrder}/{nextItemQuery.data?.totalItems} 题
                </span>
              </div>
              <div className="w-56 h-2 rounded-full bg-slate-200 dark:bg-white/10 overflow-hidden">
                <div
                  className="h-full bg-gradient-to-r from-sky-500 to-blue-500"
                  style={{
                    width: `${((nextItemQuery.data?.answeredItems || 0) / Math.max(1, nextItemQuery.data?.totalItems || 1)) * 100}%`,
                  }}
                />
              </div>
            </div>

            <section className="rounded-[3rem] liquid-glass-panel p-10 edge-light">
              <div className="text-xs uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">
                {currentItem.taskType} · {lexicalPairTypeLabel(currentItem.lexicalPairType)}
              </div>
              <div className="mt-8 grid md:grid-cols-2 gap-6 items-start">
                <div className="rounded-[2rem] border border-slate-200/80 dark:border-white/10 bg-white/60 dark:bg-white/5 p-8">
                  <div className="text-sm uppercase tracking-[0.24em] text-sky-500 mb-3">English</div>
                  <div className="text-4xl font-black text-slate-900 dark:text-white">{currentItem.englishWord}</div>
                </div>
                <div className="rounded-[2rem] border border-slate-200/80 dark:border-white/10 bg-white/60 dark:bg-white/5 p-8">
                  <div className="text-sm uppercase tracking-[0.24em] text-rose-500 mb-3">French</div>
                  <div className="text-4xl font-black text-slate-900 dark:text-white">{currentItem.frenchWord}</div>
                </div>
              </div>

              {(currentItem.stimulus.promptText || currentItem.stimulus.instruction || currentItem.stimulus.contextSentence) && (
                <div className="mt-8 rounded-[2rem] border border-dashed border-slate-300 dark:border-white/10 p-6 bg-white/40 dark:bg-white/5">
                  {currentItem.stimulus.instruction && (
                    <div className="text-sm font-bold text-slate-700 dark:text-white/75">{currentItem.stimulus.instruction}</div>
                  )}
                  {currentItem.stimulus.promptText && (
                    <div className="mt-3 text-lg text-slate-800 dark:text-white/85">{currentItem.stimulus.promptText}</div>
                  )}
                  {currentItem.stimulus.contextSentence && (
                    <div className="mt-3 text-slate-500 dark:text-white/45 italic">{currentItem.stimulus.contextSentence}</div>
                  )}
                </div>
              )}

              <div className="mt-8 grid gap-4">
                {currentItem.options.map((option) => (
                  <button
                    key={option.key}
                    type="button"
                    disabled={submitAnswerMutation.isPending || completeMutation.isPending}
                    onClick={() => void submitAnswer(option)}
                    className="w-full rounded-[1.8rem] border border-slate-200 dark:border-white/10 bg-white/70 dark:bg-white/5 px-5 py-4 text-left hover:border-primary/50 transition-all disabled:opacity-60"
                  >
                    <div className="flex items-center justify-between gap-4">
                      <span className="font-bold text-slate-900 dark:text-white">{option.label}</span>
                      <ChevronRight className="text-primary" size={16} />
                    </div>
                  </button>
                ))}
              </div>
            </section>
          </>
        )}
      </div>
    );
  }

  const result = resultQuery.data;
  const radarOption = buildRadarOption(
    result?.chartPayload.radarMetrics.map((metric) => ({
      key: metric.key,
      label: metric.label,
      value: metric.value,
      max: metric.max,
    }))
  );

  return (
    <div className="space-y-8 pb-20">
      <PageHeader title="诊断结果" subtitle={result ? `${result.templateName} · 完成于 ${formatDateTime(result.completedAt)}` : '正在加载结果'} />

      {resultQuery.error && (
        <div className="rounded-[2rem] border border-rose-500/20 bg-rose-500/5 p-6 text-rose-500">{resultQuery.error.message}</div>
      )}

      {result && (
        <>
          <section className="liquid-glass-panel rounded-[3rem] p-10 edge-light">
            <div className="flex flex-col lg:flex-row justify-between gap-8 items-start">
              <div>
                <div className="inline-flex items-center gap-3 rounded-full border border-emerald-500/20 bg-emerald-500/10 px-4 py-2 text-xs uppercase tracking-[0.24em] text-emerald-500">
                  <CheckCircle2 size={14} />
                  diagnosis completed
                </div>
                <h2 className="mt-5 text-4xl font-black text-slate-900 dark:text-white">已生成真实诊断 summary</h2>
                <p className="mt-4 text-slate-500 dark:text-white/45 leading-7">
                  本次结果已经进入训练计划和分析聚合链路。你现在可以直接转入训练，或者先阅读 AI 解释。
                </p>
              </div>
              <button type="button" onClick={() => setPhase('select')} className="btn-liquid px-6 py-3 text-white">
                再做一轮诊断
              </button>
            </div>
          </section>

          <div className="grid md:grid-cols-2 xl:grid-cols-4 gap-6">
            <div className="rounded-[2rem] liquid-glass p-6">
              <div className="text-xs uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">正迁移得分</div>
              <div className="mt-3 text-3xl font-black text-slate-900 dark:text-white">{formatMaybePercent(result.metrics.positiveTransferScore)}</div>
            </div>
            <div className="rounded-[2rem] liquid-glass p-6">
              <div className="text-xs uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">负迁移风险</div>
              <div className="mt-3 text-3xl font-black text-rose-500">{formatMaybePercent(result.metrics.negativeTransferRisk)}</div>
            </div>
            <div className="rounded-[2rem] liquid-glass p-6">
              <div className="text-xs uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">语义辨析</div>
              <div className="mt-3 text-3xl font-black text-slate-900 dark:text-white">{formatMaybePercent(result.metrics.semanticDiscrimination)}</div>
            </div>
            <div className="rounded-[2rem] liquid-glass p-6">
              <div className="text-xs uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">平均反应时</div>
              <div className="mt-3 text-3xl font-black text-slate-900 dark:text-white">{formatMs(result.metrics.averageReactionTime)}</div>
            </div>
          </div>

          <div className="grid xl:grid-cols-[1.1fr_0.9fr] gap-8">
            <section className="rounded-[2.5rem] liquid-glass-panel p-8">
              <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30 mb-4">radar profile</div>
              <div className="h-[360px]">
                <EChart option={radarOption} />
              </div>
            </section>
            <section className="rounded-[2.5rem] liquid-glass-panel p-8">
              <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30 mb-4">AI explain diagnosis</div>
              {explanationQuery.isLoading ? (
                <div className="text-sm text-slate-500 dark:text-white/45">正在生成解释...</div>
              ) : explanationQuery.data ? (
                <div className="space-y-4">
                  <p className="text-base leading-7 text-slate-800 dark:text-white/85">{explanationQuery.data.explanation}</p>
                  <div className="rounded-[1.6rem] border border-slate-200/70 dark:border-white/10 p-4 bg-white/60 dark:bg-white/5">
                    <div className="text-sm font-bold text-slate-900 dark:text-white">教师/系统备注</div>
                    <div className="mt-2 text-sm text-slate-500 dark:text-white/45 leading-6">{explanationQuery.data.teacherNote}</div>
                  </div>
                </div>
              ) : explanationQuery.error ? (
                <div className="text-sm text-rose-500">{explanationQuery.error.message}</div>
              ) : (
                <div className="text-sm text-slate-500 dark:text-white/45">暂无解释内容。</div>
              )}
            </section>
          </div>

          <div className="grid xl:grid-cols-2 gap-8">
            <section className="rounded-[2.5rem] liquid-glass-panel p-8">
              <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30 mb-4">高风险词对</div>
              <div className="space-y-4">
                {result.highRiskLexicalPairs.map((item) => (
                  <div key={item.lexicalPairId} className="rounded-[1.6rem] border border-slate-200/70 dark:border-white/10 p-4 bg-white/60 dark:bg-white/5">
                    <div className="flex items-center justify-between gap-4">
                      <div>
                        <div className="font-black text-slate-900 dark:text-white">{item.englishWord} / {item.frenchWord}</div>
                        <div className="text-sm text-slate-500 dark:text-white/45 mt-2">
                          {lexicalPairTypeLabel(item.lexicalPairType)} · {item.dominantErrorType}
                        </div>
                      </div>
                      <div className="text-right">
                        <div className="font-black text-rose-500">{formatMaybePercent(item.riskScore)}</div>
                        <div className="text-xs text-slate-400 dark:text-white/30">{formatMs(item.averageReactionTime)}</div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </section>
            <section className="rounded-[2.5rem] liquid-glass-panel p-8">
              <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30 mb-4">题目明细</div>
              <div className="space-y-4 max-h-[540px] overflow-y-auto no-scrollbar">
                {result.items.map((item) => (
                  <div key={item.itemResultId} className="rounded-[1.6rem] border border-slate-200/70 dark:border-white/10 p-4 bg-white/60 dark:bg-white/5">
                    <div className="flex items-center justify-between gap-4">
                      <div>
                        <div className="font-bold text-slate-900 dark:text-white">
                          #{item.presentationOrder} {item.englishWord} / {item.frenchWord}
                        </div>
                        <div className="text-sm text-slate-500 dark:text-white/45 mt-2">
                          {item.detectedErrorType} · {item.correct ? '正确' : '错误'}
                        </div>
                      </div>
                      <div className="text-right text-sm text-slate-500 dark:text-white/45">
                        <div>{formatMs(item.reactionTimeMs)}</div>
                        <div>{formatMaybePercent(item.transferRiskScore)}</div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </section>
          </div>
        </>
      )}
    </div>
  );
};

export default DiagnosisPage;
