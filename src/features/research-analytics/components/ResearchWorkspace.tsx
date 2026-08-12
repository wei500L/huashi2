import React from 'react';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Download, RefreshCcw, Sparkles } from 'lucide-react';
import { ChartCard } from '@/components/common/ChartCard';
import { FeedbackState } from '@/components/common/FeedbackState';
import { Pagination } from '@/components/common/Pagination';
import { PageHeader, SectionEyebrow, StatusBadge } from '@/components/common';
import { getApiErrorMessage, saveBlob } from '@/lib/api';
import { formatDateTime } from '@/lib/format';
import { researchAnalyticsService } from '@/lib/services';
import { dimensionChartOption, difficultyChartOption } from '../chartOptions';
import { hasActiveResearchFilters, normalizeResearchFilters, toResearchApiFilters, writeResearchFiltersToSearch } from '../filters';
import { formatDuration, formatRate, formatRateHint, formatScore } from '../formatters';
import { researchAnalyticsKeys } from '../queryKeys';
import { EMPTY_RESEARCH_FILTERS, type ResearchWorkspaceFilters } from '../types';
import { ResearchAttemptList } from './ResearchAttemptList';

const filterFieldClass = 'min-w-0 rounded-2xl border border-slate-200 bg-white px-3 py-2.5 text-sm dark:border-white/10 dark:bg-slate-900';

export const ResearchWorkspace: React.FC<{ embed?: boolean; initialPublishId?: number | null }> = ({ embed = false, initialPublishId = null }) => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [searchParams, setSearchParams] = useSearchParams();
  const publishId = Number(searchParams.get('publishId') || initialPublishId || '') || null;
  const [filters, setFilters] = React.useState<ResearchWorkspaceFilters>(() => normalizeResearchFilters(searchParams));
  const [page, setPage] = React.useState(Number(searchParams.get('page') || '1') || 1);
  const [mobileFiltersOpen, setMobileFiltersOpen] = React.useState(false);
  const [exportError, setExportError] = React.useState<string | null>(null);
  const [exporting, setExporting] = React.useState(false);

  const releasesQuery = useQuery({
    queryKey: researchAnalyticsKeys.releases(),
    queryFn: ({ signal }) => researchAnalyticsService.listReleases({ signal }),
  });

  React.useEffect(() => {
    if (!publishId && releasesQuery.data?.[0]) {
      const next = new URLSearchParams(searchParams);
      next.set('publishId', String(releasesQuery.data[0].publishId));
      setSearchParams(next, { replace: true });
    }
  }, [publishId, releasesQuery.data, searchParams, setSearchParams]);

  const apiFilters = toResearchApiFilters(filters);
  const overviewQuery = useQuery({
    queryKey: researchAnalyticsKeys.overview(publishId || 0, filters),
    queryFn: ({ signal }) => researchAnalyticsService.getOverview(publishId!, apiFilters, { signal }),
    enabled: publishId != null,
    placeholderData: keepPreviousData,
  });
  const questionQuery = useQuery({
    queryKey: researchAnalyticsKeys.questionStats(publishId || 0, filters),
    queryFn: ({ signal }) => researchAnalyticsService.getQuestionStats(publishId!, apiFilters, { signal }),
    enabled: publishId != null,
    placeholderData: keepPreviousData,
  });
  const dimensionQuery = useQuery({
    queryKey: researchAnalyticsKeys.dimensionStats(publishId || 0, filters),
    queryFn: ({ signal }) => researchAnalyticsService.getDimensionStats(publishId!, apiFilters, { signal }),
    enabled: publishId != null,
    placeholderData: keepPreviousData,
  });
  const attemptsQuery = useQuery({
    queryKey: researchAnalyticsKeys.attempts(publishId || 0, filters, page, 'submittedAt,desc'),
    queryFn: ({ signal }) => researchAnalyticsService.listAttempts(publishId!, { ...apiFilters, pageNo: page, pageSize: 20, sort: 'submittedAt,desc' }, { signal }),
    enabled: publishId != null,
    placeholderData: keepPreviousData,
  });
  const reportQuery = useQuery({
    queryKey: researchAnalyticsKeys.aiReport(publishId || 0),
    queryFn: ({ signal }) => researchAnalyticsService.getLatestAiReport(publishId!, { signal }),
    enabled: publishId != null,
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      return status === 'PENDING' || status === 'PROCESSING' ? 4000 : false;
    },
  });

  const selectedRelease = releasesQuery.data?.find((item) => item.publishId === publishId) ?? null;
  const overview = overviewQuery.data;
  const emptyDataset = overview != null && overview.funnel.attemptStarted === 0;
  const filteredEmpty = overview != null && !emptyDataset && (attemptsQuery.data?.total ?? 0) === 0 && hasActiveResearchFilters(filters);

  const applyFilters = (next: ResearchWorkspaceFilters) => {
    setFilters(next);
    setPage(1);
    const params = new URLSearchParams(searchParams);
    if (publishId) params.set('publishId', String(publishId));
    writeResearchFiltersToSearch(params, next);
    params.delete('page');
    setSearchParams(params);
  };

  const selectPublish = (nextId: number) => {
    const params = new URLSearchParams(searchParams);
    params.set('publishId', String(nextId));
    params.delete('page');
    setSearchParams(params);
    setPage(1);
  };

  const createReport = useMutation({
    mutationFn: () => researchAnalyticsService.createAiReport(publishId!, apiFilters),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: researchAnalyticsKeys.aiReport(publishId || 0) });
    },
  });

  const exportData = async () => {
    if (!publishId) return;
    setExporting(true);
    setExportError(null);
    try {
      const job = await researchAnalyticsService.createExport(publishId, {
        ...apiFilters,
        format: 'CSV',
        includeAttachmentManifest: true,
      });
      saveBlob(await researchAnalyticsService.downloadExport(job.jobId), job.fileName || 'research-export.csv');
    } catch (error) {
      setExportError(getApiErrorMessage(error, '导出失败。'));
    } finally {
      setExporting(false);
    }
  };

  return (
    <div className="space-y-5">
      {embed ? null : (
        <PageHeader
          eyebrow="RESEARCH DATA"
          title="研究问卷数据"
          subtitle="按发布批次查看漏斗、题目统计、匿名答卷和群体报告。完成率使用已提交 / 已开始。"
        />
      )}

      <section className="min-w-0 rounded-2xl liquid-glass-panel p-4 sm:p-5">
        <div className="flex min-w-0 flex-col gap-3 lg:flex-row lg:items-end">
          <label className="min-w-0 flex-1 text-xs font-bold text-slate-500">
            发布批次
            <select
              className={`${filterFieldClass} mt-2 w-full`}
              value={publishId ?? ''}
              onChange={(event) => selectPublish(Number(event.target.value))}
            >
              <option value="">选择研究问卷发布</option>
              {(releasesQuery.data || []).map((item) => (
                <option key={item.publishId} value={item.publishId}>
                  {item.paperTitle} · {item.releaseCode || item.publishId}
                </option>
              ))}
            </select>
          </label>
          <div className="page-actions">
            <button type="button" className="lg:hidden rounded-2xl border border-slate-200 px-4 py-2.5 text-sm font-bold dark:border-white/10" onClick={() => setMobileFiltersOpen((open) => !open)}>
              {mobileFiltersOpen ? '收起筛选' : '展开筛选'}
            </button>
            <button type="button" disabled={!publishId || exporting} onClick={() => void exportData()} className="inline-flex items-center gap-2 rounded-2xl border border-slate-200 px-4 py-2.5 text-sm font-bold disabled:opacity-40 dark:border-white/10">
              <Download size={16} />{exporting ? '导出中…' : '导出脱敏 CSV'}
            </button>
            <button type="button" disabled={!publishId} onClick={() => navigate(`/teacher/research/publishes/${publishId}/report`)} className="inline-flex items-center gap-2 rounded-2xl border border-slate-200 px-4 py-2.5 text-sm font-bold disabled:opacity-40 dark:border-white/10">
              <Sparkles size={16} />群体报告
            </button>
          </div>
        </div>
        <div className={`${mobileFiltersOpen ? 'mt-4 grid' : 'mt-4 hidden'} gap-3 md:grid md:grid-cols-2 xl:grid-cols-4`}>
          <label className="text-xs font-bold text-slate-500">状态
            <select className={`${filterFieldClass} mt-2 w-full`} value={filters.status} onChange={(event) => applyFilters({ ...filters, status: event.target.value })}>
              <option value="">全部状态</option>
              <option value="IN_PROGRESS">作答中</option>
              <option value="SUBMITTED">已提交</option>
            </select>
          </label>
          <label className="text-xs font-bold text-slate-500">进入方式
            <select className={`${filterFieldClass} mt-2 w-full`} value={filters.entryType} onChange={(event) => applyFilters({ ...filters, entryType: event.target.value })}>
              <option value="">全部方式</option>
              <option value="PUBLIC_CODE">参与码</option>
              <option value="PUBLIC_QR">二维码</option>
            </select>
          </label>
          <label className="text-xs font-bold text-slate-500">质量标记
            <select className={`${filterFieldClass} mt-2 w-full`} value={filters.qualityFlag} onChange={(event) => applyFilters({ ...filters, qualityFlag: event.target.value })}>
              <option value="">全部质量</option>
              <option value="FAST_ITEM">过快作答</option>
              <option value="SHORT_TOTAL_DURATION">总时长过短</option>
              <option value="TIMING_GAP">计时缺失</option>
            </select>
          </label>
          <label className="text-xs font-bold text-slate-500">关键词
            <input className={`${filterFieldClass} mt-2 w-full`} value={filters.keyword} onChange={(event) => applyFilters({ ...filters, keyword: event.target.value })} placeholder="匿名编号，如 P-000137" />
          </label>
        </div>
        {hasActiveResearchFilters(filters) ? (
          <button type="button" className="mt-3 text-sm font-bold text-primary" onClick={() => applyFilters(EMPTY_RESEARCH_FILTERS)}>清除筛选</button>
        ) : null}
        {exportError ? <p className="mt-3 text-sm text-rose-600">{exportError}</p> : null}
      </section>

      {releasesQuery.isError ? <FeedbackState kind="error" title="无法加载研究发布" description={getApiErrorMessage(releasesQuery.error, '发布列表暂时不可用。')} primaryAction={{ label: '重试', onClick: () => void releasesQuery.refetch() }} /> : null}
      {!releasesQuery.isLoading && !releasesQuery.data?.length ? <FeedbackState kind="empty" title="还没有可分析的研究发布" description="先创建并公开发布一份研究问卷，产生答卷后这里会显示漏斗和统计。" /> : null}
      {publishId && overviewQuery.isError ? <FeedbackState kind="error" title="总览加载失败" description={getApiErrorMessage(overviewQuery.error, '统计服务暂时不可用。')} primaryAction={{ label: '重试', onClick: () => void overviewQuery.refetch() }} /> : null}
      {publishId && emptyDataset ? <FeedbackState kind="empty" title="这份发布还没有答卷" description="参与者进入并提交后，这里会显示完成率、题目难度和匿名答卷列表。" /> : null}
      {publishId && filteredEmpty ? <FeedbackState kind="empty" title="当前筛选没有匹配答卷" description="已保留这份发布的其他统计。试着放宽状态、进入方式或匿名编号。" impact="这不是空发布，只是过滤后没有行。" /> : null}

      {overview && !emptyDataset ? (
        <>
          <section className="rounded-2xl border border-slate-200/80 bg-white/60 px-4 py-3 text-sm text-slate-500 dark:border-white/10 dark:bg-white/[0.03]" aria-live="polite">
            {selectedRelease?.paperTitle} · 完成率分母是已开始答卷 · 最近更新 {formatDateTime(overview.statisticsGeneratedAt)}
          </section>
          <div className="grid min-w-0 grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-3">
            <MetricCard title="已开始" value={String(overview.funnel.attemptStarted)} hint="实际会话漏斗：创建了答卷的人数" />
            <MetricCard title="已提交" value={String(overview.funnel.submitted)} hint="已完成提交的答卷数" />
            <MetricCard title="完成率" value={formatRate(overview.rates.completionRate)} hint={`${formatRateHint(overview.rates.completionRate)} · submitted / started`} />
            <MetricCard title="有效样本" value={String(overview.dataQuality.valid)} hint={`质量标记 ${overview.dataQuality.flagged} 份`} />
            <MetricCard title="中位分" value={formatScore(overview.score.median)} hint={`样本 ${overview.score.sampleCount}`} />
            <MetricCard title="中位用时" value={formatDuration(overview.timing.median)} hint={`样本 ${overview.timing.sampleCount}`} />
          </div>
          <section className="rounded-2xl liquid-glass-panel p-4 sm:p-6">
            <SectionEyebrow>FUNNEL</SectionEyebrow>
            <h2 className="mt-2 text-lg font-black">两个漏斗，口径始终可见</h2>
            <div className="mt-4 grid gap-4 md:grid-cols-2">
              <div>
                <h3 className="font-bold">邀请码漏斗</h3>
                <p className="mt-1 text-sm text-slate-500">已生成有效码 {overview.funnel.codeGenerated} → 已核销 {overview.funnel.codeVerified}。兑换率 {formatRate(overview.rates.codeRedemptionRate)}（{formatRateHint(overview.rates.codeRedemptionRate, '个码')}）。二维码没有预生成总人数。</p>
              </div>
              <div>
                <h3 className="font-bold">实际会话漏斗</h3>
                <p className="mt-1 text-sm text-slate-500">参与者 {overview.funnel.participantCreated} → 已开始 {overview.funnel.attemptStarted} → 作答中 {overview.funnel.inProgress} → 已提交 {overview.funnel.submitted} → 超时 {overview.funnel.expired}。</p>
              </div>
            </div>
          </section>
          <div className="grid min-w-0 gap-4 xl:grid-cols-2">
            <ChartCard
              title="维度正确率"
              description="纵轴为正确率百分比，横轴为构念/迁移维度。样本量见各维度作答数。"
              option={dimensionChartOption(dimensionQuery.data?.dimensions || [])}
              loading={dimensionQuery.isLoading}
              error={dimensionQuery.error}
              isEmpty={!dimensionQuery.data?.dimensions.length}
              onRetry={() => void dimensionQuery.refetch()}
            />
            <ChartCard
              title="题目难度"
              description="纵轴为 correctCount / validAnsweredCount，横轴为题号。未作答者不进入分母。"
              option={difficultyChartOption(questionQuery.data?.questions || [])}
              loading={questionQuery.isLoading}
              error={questionQuery.error}
              isEmpty={!questionQuery.data?.questions.some((row) => row.correctRate != null)}
              onRetry={() => void questionQuery.refetch()}
            />
          </div>
          <p className="sr-only">
            维度统计摘要：{(dimensionQuery.data?.dimensions || []).map((row) => `${row.dimension} ${row.correctCount}/${row.answeredCount}`).join('；') || '暂无维度数据'}
          </p>
          <ResearchAttemptList
            rows={attemptsQuery.data?.records || []}
            loading={attemptsQuery.isLoading}
            error={attemptsQuery.error}
            onRetry={() => void attemptsQuery.refetch()}
            onOpen={(attemptId) => navigate(`/teacher/research/attempts/${attemptId}`)}
          />
          <Pagination
            page={page}
            pageCount={Math.max(1, Math.ceil((attemptsQuery.data?.total || 0) / 20))}
            total={attemptsQuery.data?.total}
            pageSize={20}
            itemLabel="份答卷"
            onPageChange={(nextPage) => {
              setPage(nextPage);
              const params = new URLSearchParams(searchParams);
              params.set('page', String(nextPage));
              setSearchParams(params);
            }}
          />
        </>
      ) : null}

      {publishId ? (
        <section className="rounded-2xl liquid-glass-panel p-4 sm:p-6">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <SectionEyebrow>GROUP AI REPORT</SectionEyebrow>
              <h2 className="mt-2 text-lg font-black">群体研究报告</h2>
              <p className="mt-2 max-w-2xl text-sm text-slate-500">报告基于不可变统计快照。低于 5 份提交时只显示规则统计，不会调用模型。</p>
            </div>
            <StatusBadge
              label={reportQuery.data?.status === 'FALLBACK' ? '规则摘要' : reportQuery.data?.status || '未生成'}
              tone={reportQuery.data?.status === 'COMPLETED' ? 'success' : reportQuery.data?.status === 'FAILED' ? 'danger' : 'warning'}
            />
          </div>
          <div className="mt-4 flex flex-wrap gap-3">
            <button type="button" disabled={createReport.isPending} onClick={() => createReport.mutate()} className="btn-liquid px-4 py-2 text-sm text-white disabled:opacity-40">
              {createReport.isPending ? '生成中…' : '生成 / 刷新报告'}
            </button>
            <button type="button" onClick={() => navigate(`/teacher/research/publishes/${publishId}/report`)} className="rounded-2xl border border-slate-200 px-4 py-2 text-sm font-bold dark:border-white/10">查看报告页</button>
          </div>
          {createReport.isError ? <p className="mt-3 text-sm text-rose-600">{getApiErrorMessage(createReport.error, '无法生成群体报告。')}</p> : null}
        </section>
      ) : null}

      {overviewQuery.isFetching && overview ? (
        <div className="inline-flex items-center gap-2 text-xs text-slate-400"><RefreshCcw size={12} />正在刷新统计</div>
      ) : null}
    </div>
  );
};

const MetricCard: React.FC<{ title: string; value: string; hint: string }> = ({ title, value, hint }) => (
  <article className="min-w-0 rounded-2xl liquid-glass-panel p-4 sm:p-6">
    <div className="text-sm text-slate-500">{title}</div>
    <div className="mt-2 text-3xl font-black tabular-nums">{value}</div>
    <p className="mt-2 text-xs leading-5 text-slate-400">{hint}</p>
  </article>
);
