import React from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate, useParams } from 'react-router-dom';
import { FeedbackState } from '@/components/common/FeedbackState';
import { PageHeader, SectionEyebrow, StatusBadge } from '@/components/common';
import { getApiErrorMessage } from '@/lib/api';
import { formatDateTime } from '@/lib/format';
import { researchAnalyticsService } from '@/lib/services';
import { researchAnalyticsKeys } from '@/features/research-analytics/queryKeys';

const ResearchAiReportPage: React.FC = () => {
  const { publishId } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const numericId = Number(publishId);

  const query = useQuery({
    queryKey: researchAnalyticsKeys.aiReport(numericId),
    queryFn: ({ signal }) => researchAnalyticsService.getLatestAiReport(numericId, { signal }),
    enabled: Number.isFinite(numericId),
    refetchInterval: (current) => {
      const status = current.state.data?.status;
      return status === 'PENDING' || status === 'PROCESSING' ? 4000 : false;
    },
  });

  const create = useMutation({
    mutationFn: () => researchAnalyticsService.createAiReport(numericId),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: researchAnalyticsKeys.aiReport(numericId) }),
  });
  const retry = useMutation({
    mutationFn: (reportId: number) => researchAnalyticsService.retryAiReport(reportId),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: researchAnalyticsKeys.aiReport(numericId) }),
  });

  const report = query.data;
  const interpretation = report?.status === 'FALLBACK' ? report.ruleFallback : report?.report;

  return (
    <div className="page-stack pb-16">
      <PageHeader
        eyebrow="GROUP REPORT"
        title="群体研究报告"
        subtitle="先看统计事实，再看模型或规则解读。低样本不会生成模型报告。"
        actions={<button type="button" className="rounded-2xl border border-slate-200 px-4 py-2 text-sm font-bold dark:border-white/10" onClick={() => navigate(`/teacher/research?tab=data&publishId=${numericId}`)}>返回数据</button>}
      />
      {query.isLoading ? <FeedbackState kind="loading" title="正在读取报告" description="正在加载最新群体报告和统计快照。" /> : null}
      {query.isError ? <FeedbackState kind="error" title="报告加载失败" description={getApiErrorMessage(query.error, '暂时无法读取群体报告。')} /> : null}
      {!query.isLoading && !report ? (
        <FeedbackState
          kind="empty"
          title="还没有群体报告"
          description="先确认至少有 5 份提交，再基于当前过滤器生成不可变统计快照。"
          primaryAction={{ label: '生成报告', onClick: () => create.mutate(), disabled: create.isPending }}
        />
      ) : null}
      {report ? (
        <>
          <section className="rounded-2xl liquid-glass-panel p-4 sm:p-6">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <SectionEyebrow>REPORT BASIS</SectionEyebrow>
                <h2 className="mt-2 text-lg font-black">报告依据</h2>
              </div>
              <StatusBadge label={report.status === 'FALLBACK' ? '规则摘要' : report.status} tone={report.status === 'COMPLETED' ? 'success' : report.status === 'FAILED' ? 'danger' : 'warning'} />
            </div>
            <p className="mt-3 text-sm text-slate-500">
              发布 {report.publishId} · 样本 {report.sampleCount ?? '—'} · 快照 {report.snapshot?.sourceMaxUpdatedAt ? formatDateTime(report.snapshot.sourceMaxUpdatedAt) : '—'}
              {' · '}来源 {report.source === 'RULE_FALLBACK' ? '规则摘要' : report.source || '处理中'}
              {report.modelName ? ` · ${report.modelName}` : ''}
            </p>
            <div className="mt-4 flex flex-wrap gap-3">
              <button type="button" disabled={create.isPending} onClick={() => create.mutate()} className="btn-liquid px-4 py-2 text-sm text-white">重新生成</button>
              {report.status === 'FAILED' || report.status === 'FALLBACK' ? (
                <button type="button" disabled={retry.isPending} onClick={() => retry.mutate(report.reportId)} className="rounded-2xl border border-slate-200 px-4 py-2 text-sm font-bold dark:border-white/10">重试</button>
              ) : null}
            </div>
            {create.isError ? <p className="mt-3 text-sm text-rose-600">{getApiErrorMessage(create.error, '生成失败。样本不足时只会保留规则统计。')}</p> : null}
          </section>
          {report.status === 'PENDING' || report.status === 'PROCESSING' ? (
            <FeedbackState kind="saving" title="报告生成中" description="基础统计仍可查看。完成后会自动刷新，不会把规则摘要标成模型结论。" />
          ) : null}
          <section className="rounded-2xl liquid-glass-panel p-4 sm:p-6">
            <SectionEyebrow>STATISTICAL FACTS</SectionEyebrow>
            <h2 className="mt-2 text-lg font-black">统计事实</h2>
            <p className="mt-3 text-sm leading-7 text-slate-600 dark:text-white/70">
              这份报告绑定快照 {report.snapshot?.snapshotVersion || 'RESEARCH_STATS_V1'}，样本量 {report.sampleCount ?? '—'}。
              模型只能看到聚合数据、匿名题号和题干摘要，不会看到姓名、联系方式、参与码、IP 或附件原文。
            </p>
          </section>
          {interpretation ? (
            <section className="rounded-2xl liquid-glass-panel p-4 sm:p-6">
              <SectionEyebrow>MODEL OR RULE READING</SectionEyebrow>
              <h2 className="mt-2 text-lg font-black">{report.status === 'FALLBACK' ? '规则摘要' : '模型解读'}</h2>
              <p className="mt-3 text-sm leading-7">{interpretation.executiveSummary}</p>
              <FindingList title="观察到的模式" items={interpretation.observedPatterns} />
              <FindingList title="研究提醒" items={interpretation.researchCautions} />
              <FindingList title="质量限制" items={interpretation.dataQualityLimitations} />
              <p className="mt-4 text-xs text-slate-400">置信度 {interpretation.confidence}。不能据此推断因果关系。</p>
            </section>
          ) : null}
        </>
      ) : null}
    </div>
  );
};

const FindingList: React.FC<{ title: string; items?: string[] | null }> = ({ title, items }) => (
  <div className="mt-4">
    <h3 className="font-bold">{title}</h3>
    <ul className="mt-2 list-disc space-y-1 pl-5 text-sm text-slate-600 dark:text-white/70">
      {(items || []).map((item) => <li key={item}>{item}</li>)}
    </ul>
  </div>
);

export default ResearchAiReportPage;
