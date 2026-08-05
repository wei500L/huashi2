import React from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AlertTriangle, Download, Eye, EyeOff, FileText, PencilLine, RefreshCw, Search, ShieldCheck, Trash2, UserPlus } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { flushSync } from 'react-dom';
import { ClassAnalyticsPdfReport } from '@/components/analytics/AnalyticsPdfReport';
import { ChartCard } from '@/components/common/ChartCard';
import { PageHeader, SectionEyebrow, StatusBadge } from '@/components/common';
import { ConfirmationDialog } from '@/components/common/ConfirmationDialog';
import { saveBlob } from '@/lib/api';
import type { AppChartOption } from '@/lib/echarts';
import { exportReportPagesToPdf } from '@/lib/pdf-report';
import { teacherAnalyticsService, teacherClassService } from '@/lib/services';
import { buildHeatmapOption, buildRadarOption, buildTrendOption, formatDateTime, formatMaybePercent, formatMs, riskLevelLabel } from '@/lib/format';
import { useAuthStore } from '@/store';
import { cn } from '@/lib/utils';

const TeacherClassDetailPage: React.FC = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const params = useParams();
  const queryClient = useQueryClient();
  const [searchParams] = useSearchParams();
  const source = searchParams.get('source');
  const classId = Number(params.classId);
  const hasValidClassId = Number.isFinite(classId) && classId > 0;

  const [candidateKeyword, setCandidateKeyword] = React.useState('');
  const [studentKeyword, setStudentKeyword] = React.useState('');
  const [rosterView, setRosterView] = React.useState<'all' | 'risk'>('all');
  const [selectedStudentIds, setSelectedStudentIds] = React.useState<number[]>([]);
  const [feedback, setFeedback] = React.useState<string | null>(null);
  const [errorMessage, setErrorMessage] = React.useState<string | null>(null);
  const [exportErrorMessage, setExportErrorMessage] = React.useState<string | null>(null);
  const [isPdfExporting, setIsPdfExporting] = React.useState(false);
  const [showIdentity, setShowIdentity] = React.useState(false);
  const [reportGeneratedAt, setReportGeneratedAt] = React.useState<string | null>(null);
  const [archiveConfirmOpen, setArchiveConfirmOpen] = React.useState(false);
  const [removeStudentConfirmId, setRemoveStudentConfirmId] = React.useState<number | null>(null);
  const reportRef = React.useRef<HTMLDivElement | null>(null);
  const currentUser = useAuthStore((state) => state.user);

  const maskStudentName = React.useCallback((name?: string | null) => {
    if (showIdentity || !name) return name || '学生';
    const chars = Array.from(name);
    return chars.length <= 1 ? '•' : `${chars[0]}${'•'.repeat(Math.min(chars.length - 1, 3))}`;
  }, [showIdentity]);

  const detailQuery = useQuery({
    queryKey: ['teacher-class-detail', classId],
    queryFn: ({ signal }) => teacherClassService.getDetail(classId, { signal }),
    enabled: hasValidClassId,
  });
  const overviewQuery = useQuery({
    queryKey: ['teacher-class-overview', classId],
    queryFn: ({ signal }) => teacherAnalyticsService.getClassOverview(classId, '30d', { signal }),
    enabled: hasValidClassId,
  });
  const riskDistributionQuery = useQuery({
    queryKey: ['teacher-class-risk-distribution', classId],
    queryFn: ({ signal }) => teacherAnalyticsService.getRiskDistribution(classId, { signal }),
    enabled: hasValidClassId,
  });
  const heatmapQuery = useQuery({
    queryKey: ['teacher-class-heatmap', classId],
    queryFn: ({ signal }) => teacherAnalyticsService.getHeatmap(classId, '30d', { signal }),
    enabled: hasValidClassId,
  });
  const errorDistributionQuery = useQuery({
    queryKey: ['teacher-class-error-distribution', classId],
    queryFn: ({ signal }) => teacherAnalyticsService.getErrorDistribution(classId, '30d', { signal }),
    enabled: hasValidClassId,
  });
  const completionRateQuery = useQuery({
    queryKey: ['teacher-class-completion-rate', classId],
    queryFn: ({ signal }) => teacherAnalyticsService.getCompletionRate(classId, '30d', 'day', { signal }),
    enabled: hasValidClassId,
  });
  const analyticsStudentsQuery = useQuery({
    queryKey: ['teacher-class-students', classId],
    queryFn: ({ signal }) => teacherAnalyticsService.listStudents(classId, { signal }),
    enabled: hasValidClassId,
  });
  const candidateQuery = useQuery({
    queryKey: ['teacher-class-student-candidates', classId, candidateKeyword],
    queryFn: ({ signal }) => teacherClassService.listStudentCandidates(classId, candidateKeyword, { signal }),
    enabled: hasValidClassId && Boolean(detailQuery.data && currentUser?.id === detailQuery.data.teacherUserId),
  });

  const isClassOwner = Boolean(detailQuery.data && currentUser?.id === detailQuery.data.teacherUserId);

  const analyticsStudentMap = React.useMemo(
    () => new Map((analyticsStudentsQuery.data || []).map((item) => [item.studentUserId, item])),
    [analyticsStudentsQuery.data]
  );

  const highRiskStudents = React.useMemo(
    () => [...(analyticsStudentsQuery.data || [])]
      .filter((student) => student.primaryRiskLevel === 'HIGH')
      .sort((left, right) => right.recentNegativeTransferRisk - left.recentNegativeTransferRisk),
    [analyticsStudentsQuery.data]
  );

  const visibleRosterStudents = React.useMemo(() => {
    const normalizedKeyword = studentKeyword.trim().toLocaleLowerCase();
    return (detailQuery.data?.students || []).filter((student) => {
      const analyticsStudent = analyticsStudentMap.get(student.studentUserId);
      const matchesRisk = rosterView === 'all' || analyticsStudent?.primaryRiskLevel === 'HIGH';
      const matchesKeyword = !normalizedKeyword || [student.studentName, student.studentNo, student.gradeName, student.username]
        .some((value) => value?.toLocaleLowerCase().includes(normalizedKeyword));
      return matchesRisk && matchesKeyword;
    });
  }, [analyticsStudentMap, detailQuery.data?.students, rosterView, studentKeyword]);

  React.useEffect(() => {
    const assignedIds = new Set(
      (candidateQuery.data || [])
        .filter((item) => item.assigned)
        .map((item) => item.studentUserId)
    );
    setSelectedStudentIds((current) => current.filter((id) => !assignedIds.has(id)));
  }, [candidateQuery.data]);

  const invalidateClassQueries = React.useCallback(async () => {
    await queryClient.invalidateQueries({ queryKey: ['teacher-classes-management'] });
    await queryClient.invalidateQueries({ queryKey: ['teacher-class-detail', classId] });
    await queryClient.invalidateQueries({ queryKey: ['teacher-class-students', classId] });
    await queryClient.invalidateQueries({ queryKey: ['teacher-class-overview', classId] });
    await queryClient.invalidateQueries({ queryKey: ['teacher-class-risk-distribution', classId] });
    await queryClient.invalidateQueries({ queryKey: ['teacher-class-heatmap', classId] });
    await queryClient.invalidateQueries({ queryKey: ['teacher-class-error-distribution', classId] });
    await queryClient.invalidateQueries({ queryKey: ['teacher-class-completion-rate', classId] });
    await queryClient.invalidateQueries({ queryKey: ['teacher-class-student-candidates', classId] });
  }, [classId, queryClient]);

  const addStudentsMutation = useMutation({
    mutationFn: (studentUserIds: number[]) => teacherClassService.addStudents(classId, { studentUserIds }),
    onSuccess: async (_, studentUserIds) => {
      setSelectedStudentIds([]);
      setFeedback(`已加入 ${studentUserIds.length} 名学生到当前班级。`);
      setErrorMessage(null);
      await invalidateClassQueries();
    },
    onError: (error) => {
      setFeedback(null);
      setErrorMessage(error instanceof Error ? error.message : '学生加入班级失败');
    },
  });

  const removeStudentMutation = useMutation({
    mutationFn: (studentUserId: number) => teacherClassService.removeStudents(classId, { studentUserIds: [studentUserId] }),
    onSuccess: async () => {
      setRemoveStudentConfirmId(null);
      setFeedback('学生已移出当前班级。');
      setErrorMessage(null);
      await invalidateClassQueries();
    },
    onError: (error) => {
      setRemoveStudentConfirmId(null);
      setFeedback(null);
      setErrorMessage(error instanceof Error ? error.message : '学生移出班级失败');
    },
  });

  const archiveMutation = useMutation({
    mutationFn: () => teacherClassService.archiveClass(classId),
    onSuccess: async () => {
      setArchiveConfirmOpen(false);
      await queryClient.invalidateQueries({ queryKey: ['teacher-classes-management'] });
      navigate(source ? `/teacher/classes?source=${encodeURIComponent(source)}` : '/teacher/classes');
    },
    onError: (error) => {
      setFeedback(null);
      setErrorMessage(error instanceof Error ? error.message : '班级归档失败');
    },
  });

  const riskDistributionOption: AppChartOption = {
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: (riskDistributionQuery.data || []).map((item) => `${item.bucketStart.toFixed(1)}-${item.bucketEnd.toFixed(1)}`),
      axisLabel: { color: '#94a3b8' },
    },
    yAxis: { type: 'value', axisLabel: { color: '#94a3b8' } },
    series: [
      {
        type: 'bar',
        data: (riskDistributionQuery.data || []).map((item) => item.studentCount),
      },
    ],
  };

  const errorDistributionOption: AppChartOption = {
    tooltip: { trigger: 'item' },
    series: [
      {
        type: 'pie',
        radius: ['52%', '78%'],
        data: (errorDistributionQuery.data || []).map((item) => ({ name: item.label, value: item.count })),
      },
    ],
  };

  const handleExport = async () => {
    try {
      setExportErrorMessage(null);
      const blob = await teacherAnalyticsService.exportClassCsv(classId);
      saveBlob(blob, `class-${classId}-analytics.csv`);
    } catch (error) {
      setExportErrorMessage(error instanceof Error ? error.message : 'CSV 导出失败');
    }
  };

  const handlePdfExport = async () => {
    try {
      setIsPdfExporting(true);
      setExportErrorMessage(null);
      flushSync(() => {
        setReportGeneratedAt(new Date().toISOString());
      });
      await exportReportPagesToPdf(reportRef.current, `class-${classId}-analytics-report.pdf`);
    } catch (error) {
      setExportErrorMessage(error instanceof Error ? error.message : 'PDF 报告导出失败');
    } finally {
      setIsPdfExporting(false);
      setReportGeneratedAt(null);
    }
  };

  const handleArchive = () => {
    setArchiveConfirmOpen(true);
  };

  const toggleStudentSelection = (studentUserId: number) => {
    setSelectedStudentIds((current) =>
      current.includes(studentUserId) ? current.filter((item) => item !== studentUserId) : [...current, studentUserId]
    );
    setFeedback(null);
    setErrorMessage(null);
  };

  const managementErrorMessage = React.useMemo(() => {
    if (exportErrorMessage) {
      return exportErrorMessage;
    }
    if (errorMessage) {
      return errorMessage;
    }
    if (detailQuery.error instanceof Error) {
      return detailQuery.error.message;
    }
    return null;
  }, [detailQuery.error, errorMessage, exportErrorMessage]);

  const canExportPdf =
    hasValidClassId &&
    Boolean(detailQuery.data) &&
    Boolean(overviewQuery.data) &&
    Boolean(riskDistributionQuery.data) &&
    Boolean(heatmapQuery.data) &&
    Boolean(errorDistributionQuery.data) &&
    Boolean(completionRateQuery.data) &&
    Boolean(analyticsStudentsQuery.data);

  return (
    <div className="space-y-6 pb-16">
      <PageHeader
        compact
        eyebrow={t('ui.sections.students')}
        title={detailQuery.data?.className || overviewQuery.data?.className || t('ui.pages.classDetail.fallbackTitle')}
        subtitle={[
          t('ui.meta.classCode', { code: detailQuery.data?.classCode || overviewQuery.data?.classCode || '--' }),
          detailQuery.data?.gradeName || '未设置年级',
          detailQuery.data ? `当前在班 ${detailQuery.data.studentCount} 人` : null,
        ]
          .filter(Boolean)
          .join(' · ')}
        actions={
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              onClick={() => setShowIdentity((current) => !current)}
              className="inline-flex items-center gap-2 rounded-xl border border-slate-200 px-3 py-2.5 text-xs font-bold text-slate-600 dark:border-white/10 dark:text-white/65"
              aria-pressed={showIdentity}
            >
              {showIdentity ? <EyeOff size={14} /> : <Eye size={14} />}
              {showIdentity ? '隐藏身份' : '显示身份'}
            </button>
            <Link
              to={`/teacher/assessments/new?classId=${classId}&source=class-detail`}
              className="inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-sm font-black text-white"
            >
              <FileText size={14} /> 发布测评
            </Link>
            <button
              type="button"
              onClick={() => void handlePdfExport()}
              disabled={!canExportPdf || isPdfExporting}
              className="inline-flex items-center gap-2 rounded-xl border border-slate-200 px-3 py-2.5 text-sm font-semibold text-slate-700 transition hover:border-primary/40 hover:text-primary disabled:cursor-not-allowed disabled:opacity-60 dark:border-white/10 dark:text-white/75"
            >
              <FileText size={14} /> {isPdfExporting ? t('common.actions.exportingPdf') : t('common.actions.exportPdf')}
            </button>
            <button
              type="button"
              onClick={() => void handleExport()}
              className="inline-flex items-center gap-2 rounded-xl border border-slate-200 px-3 py-2.5 text-sm font-semibold text-slate-700 transition hover:border-primary/40 hover:text-primary dark:border-white/10 dark:text-white/75"
            >
              <Download size={14} /> {t('common.actions.exportCsv')}
            </button>
            {isClassOwner ? (
              <>
                <button
                  type="button"
                  onClick={() => navigate(source ? `/teacher/classes/${classId}/edit?source=${encodeURIComponent(source)}` : `/teacher/classes/${classId}/edit`)}
                  className="inline-flex items-center gap-2 rounded-xl border border-slate-200 px-3 py-2.5 text-sm font-semibold text-slate-700 transition hover:border-primary/40 hover:text-primary dark:border-white/10 dark:text-white/75"
                >
                  <PencilLine size={14} /> 编辑班级
                </button>
                <button
                  type="button"
                  onClick={handleArchive}
                  disabled={archiveMutation.isPending}
                  className="inline-flex items-center gap-2 rounded-xl border border-rose-500/20 px-3 py-2.5 text-sm font-semibold text-rose-600 transition hover:bg-rose-500/10 disabled:cursor-not-allowed disabled:opacity-60 dark:text-rose-300"
                >
                  <Trash2 size={14} /> 归档
                </button>
              </>
            ) : null}
          </div>
        }
      />

      {managementErrorMessage && (
        <div className="rounded-xl border border-rose-500/20 bg-rose-500/5 px-4 py-3 text-sm text-rose-500">
          {managementErrorMessage}
        </div>
      )}

      {!detailQuery.isLoading && detailQuery.data && !detailQuery.data.active ? (
        <div className="rounded-xl border border-amber-500/20 bg-amber-500/10 px-4 py-3 text-sm text-amber-800 dark:text-amber-200">
          <div className="flex items-start gap-3"><ShieldCheck size={16} className="mt-0.5" /><span>该班级已归档，历史证据仍可查看；新增学生和批量操作已停用。</span></div>
        </div>
      ) : null}

      {feedback && (
        <div className="rounded-xl border border-emerald-500/20 bg-emerald-500/10 px-4 py-3 text-sm text-emerald-700 dark:text-emerald-300">
          {feedback}
        </div>
      )}

      <section className="rounded-2xl border border-slate-200/80 bg-white/70 p-4 dark:border-white/10 dark:bg-white/[0.03]">
        <div className="flex flex-wrap items-center gap-2">
          <StatusBadge label={`建档 ${formatDateTime(detailQuery.data?.createdAt) || '--'}`} />
          <StatusBadge label={`最近更新 ${formatDateTime(detailQuery.data?.updatedAt) || '--'}`} />
          <StatusBadge label={`年级 ${detailQuery.data?.gradeName || '--'}`} />
          <StatusBadge label={`邀请码 ${detailQuery.data?.classCode || '--'}`} />
        </div>
        <div className="mt-3 text-xs leading-5 text-slate-500 dark:text-white/45">
          把邀请码发给学生即可。学生在 `/register` 完成注册后，会自动进入当前班级。
        </div>
      </section>

      <div className="grid overflow-hidden rounded-2xl border border-slate-200/80 bg-white/70 sm:grid-cols-2 xl:grid-cols-4 dark:border-white/10 dark:bg-white/[0.03]">
        {(overviewQuery.data?.cards || []).slice(0, 4).map((card) => (
          <div key={card.key} className="border-b border-slate-200/70 px-4 py-3.5 last:border-b-0 sm:border-r xl:border-b-0 dark:border-white/10">
            <div className="truncate text-[11px] font-semibold text-slate-500 dark:text-white/45">{card.label}</div>
            <div className="mt-1 text-xl font-black tabular-nums text-slate-900 dark:text-white">{card.value}{card.unit || ''}</div>
          </div>
        ))}
      </div>

      <section className="rounded-2xl border border-slate-200/80 bg-white/70 p-4 dark:border-white/10 dark:bg-white/[0.03]">
        <div className="flex items-end justify-between gap-4">
          <div>
            <SectionEyebrow>风险学生</SectionEyebrow>
            <div className="mt-1 text-base font-black text-slate-900 dark:text-white">现在需要优先跟进</div>
          </div>
          <StatusBadge tone={highRiskStudents.length ? 'danger' : 'success'} label={`高风险 ${highRiskStudents.length} 人`} />
        </div>
        {highRiskStudents.length ? (
          <div className="scroll-region mt-3 overflow-x-auto" tabIndex={0} role="region" aria-label="High risk students" onKeyDown={(event) => { if (event.key === 'ArrowRight' || event.key === 'ArrowLeft') { event.preventDefault(); event.currentTarget.scrollBy({ left: event.key === 'ArrowRight' ? 160 : -160, behavior: 'auto' }); } }}>
            <table className="w-full min-w-[660px] text-left text-sm">
              <thead className="text-[11px] font-bold text-slate-500 dark:text-white/40">
                <tr><th className="py-2 pr-4">学生</th><th className="px-4 py-2">风险等级</th><th className="px-4 py-2 text-right">负迁移风险</th><th className="px-4 py-2 text-right">近期正确率</th><th className="py-2 pl-4 text-right">操作</th></tr>
              </thead>
              <tbody className="divide-y divide-slate-200/70 dark:divide-white/10">
                {highRiskStudents.slice(0, 6).map((student) => (
                  <tr key={student.studentUserId}>
                    <td className="py-2.5 pr-4 font-black text-slate-900 dark:text-white">{maskStudentName(student.studentName)}</td>
                    <td className="px-4 py-2.5"><StatusBadge tone="danger" icon={<AlertTriangle size={11} />} label={riskLevelLabel(student.primaryRiskLevel)} /></td>
                    <td className="px-4 py-2.5 text-right font-black tabular-nums text-rose-600 dark:text-rose-300">{formatMaybePercent(student.recentNegativeTransferRisk)}</td>
                    <td className="px-4 py-2.5 text-right tabular-nums text-slate-600 dark:text-white/60">{formatMaybePercent(student.recentAccuracy)}</td>
                    <td className="py-2.5 pl-4 text-right"><Link to={`/teacher/classes/${classId}/students/${student.studentUserId}`} className="text-xs font-black text-primary">查看分析</Link></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="mt-3 rounded-xl border border-dashed border-slate-300 px-4 py-5 text-xs text-slate-500 dark:border-white/15 dark:text-white/45">当前没有标记为高风险的学生；仍可在下方名册查看全部学生指标。</div>
        )}
      </section>

      <div className="grid gap-5 xl:grid-cols-[0.95fr_1.05fr]">
        <ChartCard
          title={t('ui.charts.classRadar')}
          option={buildRadarOption(overviewQuery.data?.radar)}
          loading={overviewQuery.isLoading}
          isEmpty={!overviewQuery.data?.radar.length}
        />
        <ChartCard
          title={t('ui.charts.classHeatmap')}
          option={buildHeatmapOption(heatmapQuery.data)}
          loading={heatmapQuery.isLoading}
          isEmpty={!heatmapQuery.data?.cells.length}
        />
      </div>

      <div className="grid gap-5 xl:grid-cols-3">
        <ChartCard
          title={t('ui.charts.riskBuckets')}
          option={riskDistributionOption}
          loading={riskDistributionQuery.isLoading}
          isEmpty={!riskDistributionQuery.data?.length}
        />
        <ChartCard
          title={t('ui.sections.errorDistribution')}
          option={errorDistributionOption}
          loading={errorDistributionQuery.isLoading}
          isEmpty={!errorDistributionQuery.data?.length}
        />
        <ChartCard
          title={t('ui.charts.completionTrend')}
          option={buildTrendOption(completionRateQuery.data?.trend)}
          loading={completionRateQuery.isLoading}
          isEmpty={!completionRateQuery.data?.trend.series.length}
        />
      </div>

      <div className={cn('grid gap-5', isClassOwner && 'xl:grid-cols-[1.1fr_0.9fr]')}>
        <section className="rounded-2xl border border-slate-200/80 bg-white/70 p-4 dark:border-white/10 dark:bg-white/[0.03]">
          <div className="flex items-center justify-between gap-4">
            <div>
              <SectionEyebrow>在班学生</SectionEyebrow>
              <div className="mt-1 text-base font-black text-slate-900 dark:text-white">名册与学习指标</div>
            </div>
            <StatusBadge label={`共 ${detailQuery.data?.studentCount || 0} 人`} />
          </div>

          <div className="mt-3 flex flex-col gap-2 sm:flex-row">
            <label className="flex min-w-0 flex-1 items-center gap-2 rounded-xl border border-slate-200 bg-white px-3 py-2.5 dark:border-white/10 dark:bg-white/[0.04]">
              <Search size={14} className="text-slate-400" />
              <input value={studentKeyword} onChange={(event) => setStudentKeyword(event.target.value)} placeholder="搜索姓名、学号、账号或年级" className="min-w-0 flex-1 bg-transparent text-xs outline-none placeholder:text-slate-400" />
            </label>
            <div className="inline-flex self-start rounded-xl bg-slate-100 p-1 dark:bg-white/[0.06]">
              <button type="button" onClick={() => setRosterView('all')} className={cn('rounded-lg px-3 py-2 text-xs font-bold', rosterView === 'all' ? 'bg-white text-slate-900 shadow-sm dark:bg-slate-800 dark:text-white' : 'text-slate-500 dark:text-white/45')}>全部</button>
              <button type="button" onClick={() => setRosterView('risk')} className={cn('rounded-lg px-3 py-2 text-xs font-bold', rosterView === 'risk' ? 'bg-white text-rose-600 shadow-sm dark:bg-slate-800 dark:text-rose-300' : 'text-slate-500 dark:text-white/45')}>高风险</button>
            </div>
          </div>

          {visibleRosterStudents.length ? (
            <div className="scroll-region mt-3 overflow-x-auto rounded-xl border border-slate-200/70 dark:border-white/10" tabIndex={0} role="region" aria-label="Class roster" onKeyDown={(event) => { if (event.key === 'ArrowRight' || event.key === 'ArrowLeft') { event.preventDefault(); event.currentTarget.scrollBy({ left: event.key === 'ArrowRight' ? 160 : -160, behavior: 'auto' }); } }}>
              <table className="w-full min-w-[860px] text-left text-sm">
                <thead className="bg-slate-50/80 text-[11px] font-bold text-slate-500 dark:bg-white/[0.025] dark:text-white/40">
                  <tr><th className="px-3 py-2.5">学生</th><th className="px-3 py-2.5">档案</th><th className="px-3 py-2.5">风险</th><th className="px-3 py-2.5 text-right">正确率</th><th className="px-3 py-2.5 text-right">反应时</th><th className="px-3 py-2.5 text-right">操作</th></tr>
                </thead>
                <tbody className="divide-y divide-slate-200/70 dark:divide-white/10">
                  {visibleRosterStudents.map((student) => {
                    const analyticsStudent = analyticsStudentMap.get(student.studentUserId);
                    return (
                      <tr key={student.studentUserId} className="hover:bg-slate-50/70 dark:hover:bg-white/[0.025]">
                        <td className="max-w-[200px] px-3 py-2.5"><div className="truncate font-black text-slate-900 dark:text-white">{maskStudentName(student.studentName)}</div><div className="mt-0.5 text-[11px] text-slate-400">加入 {formatDateTime(student.joinedAt) || '--'}</div></td>
                        <td className="max-w-[220px] px-3 py-2.5"><div className="truncate text-xs text-slate-600 dark:text-white/60">{showIdentity ? [student.studentNo, student.gradeName, student.username].filter(Boolean).join(' · ') || '尚未补齐学生档案' : [student.gradeName].filter(Boolean).join(' · ') || '身份信息已遮蔽'}</div></td>
                        <td className="px-3 py-2.5"><StatusBadge tone={analyticsStudent?.primaryRiskLevel === 'HIGH' ? 'danger' : 'neutral'} label={`${riskLevelLabel(analyticsStudent?.primaryRiskLevel)} · ${formatMaybePercent(analyticsStudent?.recentNegativeTransferRisk)}`} /></td>
                        <td className="px-3 py-2.5 text-right tabular-nums text-slate-600 dark:text-white/60">{formatMaybePercent(analyticsStudent?.recentAccuracy)}</td>
                        <td className="px-3 py-2.5 text-right tabular-nums text-slate-600 dark:text-white/60">{formatMs(analyticsStudent?.recentAvgReactionTimeMs)}</td>
                        <td className="px-3 py-2.5"><div className="flex items-center justify-end gap-2"><Link to={`/teacher/classes/${classId}/students/${student.studentUserId}`} className="text-xs font-black text-primary">查看分析</Link>{isClassOwner ? <button type="button" onClick={() => setRemoveStudentConfirmId(student.studentUserId)} disabled={removeStudentMutation.isPending} className="rounded-lg p-1.5 text-rose-500 hover:bg-rose-500/10 disabled:opacity-50" aria-label={`移出学生 ${student.studentName}`} title="移出班级"><Trash2 size={14} /></button> : null}</div></td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          ) : (
            <div className="mt-3 rounded-xl border border-dashed border-slate-300 px-4 py-6 text-center text-xs leading-5 text-slate-500 dark:border-white/15 dark:text-white/45">
              {detailQuery.data?.students.length ? '没有符合当前搜索或风险筛选的学生。' : isClassOwner ? '当前班级还没有学生。可在右侧搜索并批量加入。' : '当前班级还没有学生。'}
            </div>
          )}
        </section>

        {isClassOwner ? <section className="rounded-2xl border border-slate-200/80 bg-white/70 p-4 dark:border-white/10 dark:bg-white/[0.03]">
          <div className="flex items-center justify-between gap-4">
            <div>
              <SectionEyebrow>学生分配</SectionEyebrow>
              <div className="mt-1 text-base font-black text-slate-900 dark:text-white">候选学生</div>
            </div>
            <button
              type="button"
              onClick={() => addStudentsMutation.mutate(selectedStudentIds)}
              disabled={!detailQuery.data?.active || !selectedStudentIds.length || addStudentsMutation.isPending}
              className="inline-flex items-center gap-2 rounded-xl bg-primary px-3 py-2.5 text-xs font-black text-white transition hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-60"
            >
              <UserPlus size={14} />
              加入 {selectedStudentIds.length || 0} 人
            </button>
          </div>

          {addStudentsMutation.error ? (
            <div className="mt-3 rounded-xl border border-rose-500/20 bg-rose-500/5 px-3 py-2.5 text-xs text-rose-600 dark:text-rose-300">
              <div>{addStudentsMutation.error instanceof Error ? addStudentsMutation.error.message : '批量加入失败'}</div>
              <button type="button" onClick={() => addStudentsMutation.mutate(selectedStudentIds)} disabled={!selectedStudentIds.length || addStudentsMutation.isPending} className="mt-2 inline-flex items-center gap-2 rounded-full border border-rose-500/30 px-3 py-1.5 font-bold disabled:opacity-50"><RefreshCw size={12} /> 重试批量加入</button>
            </div>
          ) : null}

          <label className="mt-3 flex items-center gap-2 rounded-xl border border-slate-200 bg-white/70 px-3 py-2.5 dark:border-white/10 dark:bg-white/5">
            <Search size={16} className="text-slate-400" />
            <input
              value={candidateKeyword}
              onChange={(event) => setCandidateKeyword(event.target.value)}
              placeholder="按学生姓名、学号、账号或年级搜索"
              className="w-full bg-transparent text-sm outline-none placeholder:text-slate-400"
            />
          </label>

          <div className="mt-3 flex items-center justify-between text-xs text-slate-500 dark:text-white/45">
            <span>已选择 {selectedStudentIds.length} 人。确认后会一次提交，失败时保留选择以便重试。</span>
            <button
              type="button"
              onClick={() => {
                const availableIds = (candidateQuery.data || []).filter((candidate) => !candidate.assigned).map((candidate) => candidate.studentUserId);
                setSelectedStudentIds((current) => current.length === availableIds.length ? [] : availableIds);
              }}
              disabled={!candidateQuery.data?.some((candidate) => !candidate.assigned) || !detailQuery.data?.active}
              className="rounded-full border border-slate-200 px-3 py-1.5 font-bold disabled:opacity-50 dark:border-white/10"
            >
              全选 / 清空
            </button>
          </div>

          <div className="mt-3 max-h-[420px] space-y-2 overflow-y-auto pr-1">
            {candidateQuery.error ? (
              <div className="rounded-xl border border-rose-500/20 bg-rose-500/5 px-3 py-2.5 text-xs text-rose-600 dark:text-rose-300">
                <div>候选学生加载失败，请稍后重试。</div>
                <button type="button" onClick={() => void candidateQuery.refetch()} className="mt-2 inline-flex items-center gap-2 rounded-full border border-rose-500/30 px-3 py-1.5 font-bold"><RefreshCw size={12} /> 重试</button>
              </div>
            ) : null}
            {(candidateQuery.data || []).map((candidate) => {
              const selected = selectedStudentIds.includes(candidate.studentUserId);
              return (
                <button
                  key={candidate.studentUserId}
                  type="button"
                  disabled={candidate.assigned}
                  onClick={() => toggleStudentSelection(candidate.studentUserId)}
                  className={`w-full rounded-xl border px-3 py-2.5 text-left transition ${
                    candidate.assigned
                      ? 'border-emerald-500/20 bg-emerald-500/10 text-slate-500 dark:text-white/55'
                      : selected
                        ? 'border-primary/40 bg-primary/10'
                        : 'border-slate-200/70 bg-white/60 hover:border-primary/30 dark:border-white/10 dark:bg-white/5'
                  }`}
                >
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <div className="font-semibold text-slate-900 dark:text-white">{maskStudentName(candidate.studentName)}</div>
                      <div className="mt-1 text-xs text-slate-500 dark:text-white/45">
                        {showIdentity ? [candidate.studentNo, candidate.gradeName, candidate.username].filter(Boolean).join(' · ') || '未补齐学生资料' : [candidate.gradeName].filter(Boolean).join(' · ') || '身份信息已遮蔽'}
                      </div>
                    </div>
                    <div className="flex flex-wrap gap-2">
                      {candidate.assigned && <StatusBadge tone="success" label="已在当前班级" />}
                      {!candidate.assigned && selected && <StatusBadge tone="info" label="待加入" />}
                      <StatusBadge label={`在读班级 ${candidate.activeClassCount}`} />
                    </div>
                  </div>
                </button>
              );
            })}

            {!candidateQuery.isLoading && !candidateQuery.data?.length && (
              <div className="rounded-xl border border-dashed border-slate-300 bg-white/40 p-5 text-xs leading-5 text-slate-500 dark:border-white/15 dark:bg-white/[0.03] dark:text-white/45">
                没有匹配到候选学生。可以先清空搜索词，或先到后台补齐学生账号和学生档案。
              </div>
            )}
          </div>
        </section> : null}
      </div>

      {reportGeneratedAt ? (
        <ClassAnalyticsPdfReport
          reportRef={reportRef}
          generatedAt={reportGeneratedAt}
          detail={detailQuery.data}
          overview={overviewQuery.data}
          riskDistribution={riskDistributionQuery.data}
          heatmap={heatmapQuery.data}
          errorDistribution={errorDistributionQuery.data}
          completionRate={completionRateQuery.data}
          students={analyticsStudentsQuery.data}
        />
      ) : null}
      <ConfirmationDialog
        open={archiveConfirmOpen}
        title={`确认归档班级“${detailQuery.data?.className || `#${classId}`}”？`}
        description="归档请求会将当前班级从教师班级列表中隐藏。"
        safety="历史测评和分析记录会保留；班级归档后不会自动删除学生或历史数据。"
        nextStep="先确认班级名称；如需继续管理请取消，确认后归档。"
        confirmLabel="确认归档班级"
        cancelLabel="取消，保留班级"
        pending={archiveMutation.isPending}
        pendingTitle="正在归档班级"
        pendingDescription="归档请求已经提交，请等待服务器确认。"
        onCancel={() => setArchiveConfirmOpen(false)}
        onConfirm={() => archiveMutation.mutate()}
      />
      <ConfirmationDialog
        open={removeStudentConfirmId !== null}
        title="确认移出当前班级？"
        description="该学生将从当前班级的成员列表中移除。"
        safety="学生账号、学习数据和历史测评不会被删除，只会解除当前班级关系。"
        nextStep="如需保留班级关系请取消；确认后可重新搜索并加入。"
        confirmLabel="确认移出学生"
        cancelLabel="取消，保留关系"
        pending={removeStudentMutation.isPending}
        pendingTitle="正在移出学生"
        pendingDescription="移出请求已经提交，请等待服务器确认。"
        onCancel={() => setRemoveStudentConfirmId(null)}
        onConfirm={() => {
          if (removeStudentConfirmId !== null) {
            removeStudentMutation.mutate(removeStudentConfirmId);
          }
        }}
      />
    </div>
  );
};

export default TeacherClassDetailPage;
