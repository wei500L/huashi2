import React from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Download, PencilLine, Search, Trash2, UserPlus, Users } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { ChartCard } from '@/components/common/ChartCard';
import { PageHeader, SectionEyebrow, StatCard, StatusBadge } from '@/components/common';
import { saveBlob } from '@/lib/api';
import type { AppChartOption } from '@/lib/echarts';
import { teacherAnalyticsService, teacherClassService } from '@/lib/services';
import { buildHeatmapOption, buildRadarOption, buildTrendOption, formatDateTime, formatMaybePercent, formatMs, trainingModeLabel } from '@/lib/format';

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
  const [selectedStudentIds, setSelectedStudentIds] = React.useState<number[]>([]);
  const [feedback, setFeedback] = React.useState<string | null>(null);
  const [errorMessage, setErrorMessage] = React.useState<string | null>(null);

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
    enabled: hasValidClassId,
  });

  const analyticsStudentMap = React.useMemo(
    () => new Map((analyticsStudentsQuery.data || []).map((item) => [item.studentUserId, item])),
    [analyticsStudentsQuery.data]
  );

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
      setFeedback('学生已移出当前班级。');
      setErrorMessage(null);
      await invalidateClassQueries();
    },
    onError: (error) => {
      setFeedback(null);
      setErrorMessage(error instanceof Error ? error.message : '学生移出班级失败');
    },
  });

  const archiveMutation = useMutation({
    mutationFn: () => teacherClassService.archiveClass(classId),
    onSuccess: async () => {
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
    const blob = await teacherAnalyticsService.exportClassCsv(classId);
    saveBlob(blob, `class-${classId}-analytics.csv`);
  };

  const handleArchive = () => {
    const label = detailQuery.data?.className || `#${classId}`;
    if (!window.confirm(`确认归档班级“${label}”吗？归档后它会从教师班级列表中隐藏，但历史测评和分析记录会保留。`)) {
      return;
    }
    archiveMutation.mutate();
  };

  const toggleStudentSelection = (studentUserId: number) => {
    setSelectedStudentIds((current) =>
      current.includes(studentUserId) ? current.filter((item) => item !== studentUserId) : [...current, studentUserId]
    );
    setFeedback(null);
    setErrorMessage(null);
  };

  const managementErrorMessage = React.useMemo(() => {
    if (errorMessage) {
      return errorMessage;
    }
    if (detailQuery.error instanceof Error) {
      return detailQuery.error.message;
    }
    return null;
  }, [detailQuery.error, errorMessage]);

  return (
    <div className="space-y-10 pb-20">
      <PageHeader
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
          <div className="flex flex-wrap gap-3">
            <button type="button" onClick={() => void handleExport()} className="btn-liquid flex items-center gap-2 px-5 py-3 text-white">
              <Download size={14} /> {t('common.actions.exportCsv')}
            </button>
            <button
              type="button"
              onClick={() =>
                navigate(source ? `/teacher/classes/${classId}/edit?source=${encodeURIComponent(source)}` : `/teacher/classes/${classId}/edit`)
              }
              className="inline-flex items-center gap-2 rounded-full border border-slate-200 px-4 py-3 text-sm font-semibold text-slate-700 transition hover:border-primary/40 hover:text-primary dark:border-white/10 dark:text-white/80"
            >
              <PencilLine size={14} />
              编辑班级
            </button>
            <button
              type="button"
              onClick={handleArchive}
              disabled={archiveMutation.isPending}
              className="inline-flex items-center gap-2 rounded-full border border-rose-500/20 px-4 py-3 text-sm font-semibold text-rose-600 transition hover:bg-rose-500/10 disabled:cursor-not-allowed disabled:opacity-60 dark:text-rose-300"
            >
              <Trash2 size={14} />
              归档班级
            </button>
          </div>
        }
      />

      {managementErrorMessage && (
        <div className="rounded-[2rem] border border-rose-500/20 bg-rose-500/5 p-6 text-rose-500">
          {managementErrorMessage}
        </div>
      )}

      {feedback && (
        <div className="rounded-[2rem] border border-emerald-500/20 bg-emerald-500/10 p-6 text-emerald-700 dark:text-emerald-300">
          {feedback}
        </div>
      )}

      <section className="rounded-[2.4rem] liquid-glass-panel p-6">
        <div className="flex flex-wrap items-center gap-3">
          <StatusBadge label={`建档 ${formatDateTime(detailQuery.data?.createdAt) || '--'}`} />
          <StatusBadge label={`最近更新 ${formatDateTime(detailQuery.data?.updatedAt) || '--'}`} />
          <StatusBadge label={`年级 ${detailQuery.data?.gradeName || '--'}`} />
          <StatusBadge label={`邀请码 ${detailQuery.data?.classCode || '--'}`} />
        </div>
        <div className="mt-4 text-sm leading-6 text-slate-500 dark:text-white/45">
          把邀请码发给学生即可。学生在 `/register` 完成注册后，会自动进入当前班级。
        </div>
      </section>

      <div className="grid grid-cols-1 gap-6 md:grid-cols-2 xl:grid-cols-4">
        {(overviewQuery.data?.cards || []).slice(0, 4).map((card) => (
          <StatCard key={card.key} title={card.label} value={`${card.value}${card.unit || ''}`} icon={Users} />
        ))}
      </div>

      <div className="grid gap-8 xl:grid-cols-[0.95fr_1.05fr]">
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

      <div className="grid gap-8 xl:grid-cols-3">
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

      <div className="grid gap-8 xl:grid-cols-[1.1fr_0.9fr]">
        <section className="rounded-[2.5rem] liquid-glass-panel p-8">
          <div className="flex items-center justify-between gap-4">
            <div>
              <SectionEyebrow>在班学生</SectionEyebrow>
              <div className="mt-3 text-2xl font-black text-slate-900 dark:text-white">名册管理</div>
              <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                这里负责班级名册本身。查看学生分析仍然从每位学生详情继续下钻。
              </div>
            </div>
            <StatusBadge label={`共 ${detailQuery.data?.studentCount || 0} 人`} />
          </div>

          <div className="mt-6 space-y-4">
            {(detailQuery.data?.students || []).map((student) => {
              const analyticsStudent = analyticsStudentMap.get(student.studentUserId);
              return (
                <div
                  key={student.studentUserId}
                  className="rounded-[1.6rem] border border-slate-200/70 bg-white/60 p-5 dark:border-white/10 dark:bg-white/5"
                >
                  <div className="flex flex-wrap items-start justify-between gap-4">
                    <div>
                      <div className="font-black text-slate-900 dark:text-white">{student.studentName}</div>
                      <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                        {[student.studentNo, student.gradeName, student.username].filter(Boolean).join(' · ') || '尚未补齐学生档案'}
                      </div>
                      <div className="mt-2 text-xs text-slate-400 dark:text-white/30">
                        加入时间 {formatDateTime(student.joinedAt) || '--'}
                      </div>
                    </div>
                    <div className="text-right text-sm text-slate-500 dark:text-white/45">
                      <div>正确率 {formatMaybePercent(analyticsStudent?.recentAccuracy)}</div>
                      <div>风险 {formatMaybePercent(analyticsStudent?.recentNegativeTransferRisk)}</div>
                      <div>{formatMs(analyticsStudent?.recentAvgReactionTimeMs)}</div>
                    </div>
                  </div>

                  <div className="mt-4 flex flex-wrap gap-3">
                    <Link
                      to={`/teacher/classes/${classId}/students/${student.studentUserId}`}
                      className="inline-flex items-center gap-2 rounded-full bg-slate-900 px-4 py-2 text-sm font-semibold text-white transition hover:bg-slate-700 dark:bg-white dark:text-slate-900 dark:hover:bg-slate-200"
                    >
                      查看学生分析
                    </Link>
                    <button
                      type="button"
                      onClick={() => {
                        if (!window.confirm(`确认将 ${student.studentName} 移出当前班级吗？`)) {
                          return;
                        }
                        removeStudentMutation.mutate(student.studentUserId);
                      }}
                      disabled={removeStudentMutation.isPending}
                      className="inline-flex items-center gap-2 rounded-full border border-rose-500/20 px-4 py-2 text-sm font-semibold text-rose-600 transition hover:bg-rose-500/10 disabled:cursor-not-allowed disabled:opacity-60 dark:text-rose-300"
                    >
                      <Trash2 size={14} />
                      移出班级
                    </button>
                  </div>
                </div>
              );
            })}

            {!detailQuery.isLoading && !detailQuery.data?.students.length && (
              <div className="rounded-[1.6rem] border border-dashed border-slate-300 bg-white/40 p-6 text-sm text-slate-500 dark:border-white/15 dark:bg-white/[0.03] dark:text-white/45">
                当前班级还没有学生。先在右侧搜索并分配学生。
              </div>
            )}
          </div>
        </section>

        <section className="rounded-[2.5rem] liquid-glass-panel p-8">
          <div className="flex items-center justify-between gap-4">
            <div>
              <SectionEyebrow>学生分配</SectionEyebrow>
              <div className="mt-3 text-2xl font-black text-slate-900 dark:text-white">候选学生</div>
              <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                搜索学生后批量加入当前班级。已在班学生会自动标记，不会重复加入。
              </div>
            </div>
            <button
              type="button"
              onClick={() => addStudentsMutation.mutate(selectedStudentIds)}
              disabled={!selectedStudentIds.length || addStudentsMutation.isPending}
              className="inline-flex items-center gap-2 rounded-full bg-primary px-4 py-3 text-sm font-semibold text-white transition hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-60"
            >
              <UserPlus size={14} />
              加入 {selectedStudentIds.length || 0} 人
            </button>
          </div>

          <label className="mt-6 flex items-center gap-3 rounded-[1.4rem] border border-slate-200 bg-white/70 px-4 py-3 dark:border-white/10 dark:bg-white/5">
            <Search size={16} className="text-slate-400" />
            <input
              value={candidateKeyword}
              onChange={(event) => setCandidateKeyword(event.target.value)}
              placeholder="按学生姓名、学号、账号或年级搜索"
              className="w-full bg-transparent text-sm outline-none placeholder:text-slate-400"
            />
          </label>

          <div className="mt-6 space-y-3">
            {(candidateQuery.data || []).map((candidate) => {
              const selected = selectedStudentIds.includes(candidate.studentUserId);
              return (
                <button
                  key={candidate.studentUserId}
                  type="button"
                  disabled={candidate.assigned}
                  onClick={() => toggleStudentSelection(candidate.studentUserId)}
                  className={`w-full rounded-[1.4rem] border p-4 text-left transition ${
                    candidate.assigned
                      ? 'border-emerald-500/20 bg-emerald-500/10 text-slate-500 dark:text-white/55'
                      : selected
                        ? 'border-primary/40 bg-primary/10'
                        : 'border-slate-200/70 bg-white/60 hover:border-primary/30 dark:border-white/10 dark:bg-white/5'
                  }`}
                >
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <div className="font-semibold text-slate-900 dark:text-white">{candidate.studentName}</div>
                      <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                        {[candidate.studentNo, candidate.gradeName, candidate.username].filter(Boolean).join(' · ') || '未补齐学生资料'}
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
              <div className="rounded-[1.6rem] border border-dashed border-slate-300 bg-white/40 p-6 text-sm text-slate-500 dark:border-white/15 dark:bg-white/[0.03] dark:text-white/45">
                没有匹配到候选学生。可以先清空搜索词，或先到后台补齐学生账号和学生档案。
              </div>
            )}
          </div>
        </section>
      </div>
    </div>
  );
};

export default TeacherClassDetailPage;
