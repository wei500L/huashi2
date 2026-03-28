import React from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useSearchParams } from 'react-router-dom';
import { PageHeader, PanelSkeleton } from '@/components/common';
import type { TeacherInterventionSummaryVO } from '@/lib/contracts';
import { teacherAnalyticsService, teacherInterventionService } from '@/lib/services';
import { formatDateTime } from '@/lib/format';
import { getApiErrorMessage } from '@/lib/api';

type InterventionFormState = {
  priority: string;
  status: string;
  plannedAt: string;
  teacherNote: string;
};

type InterventionView = 'all' | 'pending' | 'overdue' | 'completed';

const pageSize = 12;
const STATUS_OPTIONS = ['ALL', 'PENDING', 'IN_PROGRESS', 'COMPLETED'] as const;
const PRIORITY_OPTIONS = ['ALL', 'LOW', 'NORMAL', 'URGENT'] as const;
const VIEW_OPTIONS: Array<{ value: InterventionView; label: string }> = [
  { value: 'all', label: '全部记录' },
  { value: 'pending', label: '待处理' },
  { value: 'overdue', label: '逾期未完成' },
  { value: 'completed', label: '已完成' },
];

export function normalizeInterventionView(value?: string | null): InterventionView {
  if (value === 'pending' || value === 'overdue' || value === 'completed') {
    return value;
  }
  return 'all';
}

function toPositiveIntegerString(value?: string | null): string {
  const parsed = Number(value || '');
  return Number.isInteger(parsed) && parsed > 0 ? String(parsed) : '';
}

export function matchInterventionView(item: TeacherInterventionSummaryVO, view: InterventionView): boolean {
  if (view === 'all') {
    return true;
  }
  if (view === 'completed') {
    return item.status === 'COMPLETED';
  }
  if (view === 'pending') {
    return item.status !== 'COMPLETED';
  }
  return item.status !== 'COMPLETED' && !!item.plannedAt && new Date(item.plannedAt).getTime() < Date.now();
}

function buildInterventionSearch(params: {
  view: InterventionView;
  priority: string;
  classId: string;
  studentUserId: string;
  pageNo: number;
  focusId: number | null;
  source: string;
}): URLSearchParams {
  const next = new URLSearchParams();
  if (params.view !== 'all') {
    next.set('view', params.view);
  }
  if (params.priority !== 'ALL') {
    next.set('priority', params.priority);
  }
  if (params.classId) {
    next.set('classId', params.classId);
  }
  if (params.studentUserId) {
    next.set('studentUserId', params.studentUserId);
  }
  if (params.pageNo > 1) {
    next.set('pageNo', String(params.pageNo));
  }
  if (params.focusId) {
    next.set('focusId', String(params.focusId));
  }
  if (params.source) {
    next.set('source', params.source);
  }
  return next;
}

function buildBackendStatus(view: InterventionView): string | undefined {
  if (view === 'completed') {
    return 'COMPLETED';
  }
  return undefined;
}

function toDateTimeLocalValue(value?: string | null): string {
  if (!value) {
    return '';
  }
  return value.slice(0, 16);
}

function buildInterventionForm(item?: TeacherInterventionSummaryVO | null): InterventionFormState {
  return {
    priority: item?.priority || 'NORMAL',
    status: item?.status || 'PENDING',
    plannedAt: toDateTimeLocalValue(item?.plannedAt || item?.createdAt || new Date().toISOString()),
    teacherNote: item?.teacherNote || item?.suggestedAction || '',
  };
}

function totalPages(total = 0): number {
  return Math.max(1, Math.ceil(total / pageSize));
}

const TeacherInterventionsPage: React.FC = () => {
  const queryClient = useQueryClient();
  const [searchParams, setSearchParams] = useSearchParams();
  const [view, setView] = React.useState<InterventionView>(() => normalizeInterventionView(searchParams.get('view')));
  const [priority, setPriority] = React.useState<(typeof PRIORITY_OPTIONS)[number]>(() => {
    const value = searchParams.get('priority');
    return PRIORITY_OPTIONS.includes(value as (typeof PRIORITY_OPTIONS)[number])
      ? (value as (typeof PRIORITY_OPTIONS)[number])
      : 'ALL';
  });
  const [classId, setClassId] = React.useState(() => toPositiveIntegerString(searchParams.get('classId')));
  const [studentUserId, setStudentUserId] = React.useState(() => toPositiveIntegerString(searchParams.get('studentUserId')));
  const [pageNo, setPageNo] = React.useState(() => {
    const parsed = Number(searchParams.get('pageNo') || '1');
    return Number.isInteger(parsed) && parsed > 0 ? parsed : 1;
  });
  const [selectedInterventionId, setSelectedInterventionId] = React.useState<number | null>(() => {
    const parsed = Number(searchParams.get('focusId') || '');
    return Number.isInteger(parsed) && parsed > 0 ? parsed : null;
  });
  const [source] = React.useState(() => searchParams.get('source') || '');
  const [form, setForm] = React.useState<InterventionFormState>(buildInterventionForm());

  const classesQuery = useQuery({
    queryKey: ['teacher-classes'],
    queryFn: ({ signal }) => teacherAnalyticsService.listClasses({ signal }),
  });

  const interventionsQuery = useQuery({
    queryKey: ['teacher-interventions', classId, studentUserId, view, priority, pageNo],
    queryFn: ({ signal }) =>
      teacherInterventionService.list(
        {
          classId: classId ? Number(classId) : undefined,
          status: buildBackendStatus(view),
          priority: priority === 'ALL' ? undefined : priority,
          studentUserId: studentUserId ? Number(studentUserId) : undefined,
          pageNo,
          pageSize,
        },
        { signal }
      ),
  });

  const saveMutation = useMutation({
    mutationFn: () => {
      if (!selectedInterventionId) {
        throw new Error('No intervention selected');
      }
      return teacherInterventionService.update(selectedInterventionId, {
        priority: form.priority,
        status: form.status,
        plannedAt: form.plannedAt || null,
        teacherNote: form.teacherNote,
      });
    },
    onSuccess: async (updated) => {
      setForm(buildInterventionForm(updated));
      await queryClient.invalidateQueries({ queryKey: ['teacher-interventions'] });
    },
  });

  const completeMutation = useMutation({
    mutationFn: () => {
      if (!selectedInterventionId) {
        throw new Error('No intervention selected');
      }
      return teacherInterventionService.update(selectedInterventionId, {
        priority: form.priority,
        status: 'COMPLETED',
        plannedAt: form.plannedAt || null,
        teacherNote: form.teacherNote,
      });
    },
    onSuccess: async (updated) => {
      setForm(buildInterventionForm(updated));
      await queryClient.invalidateQueries({ queryKey: ['teacher-interventions'] });
    },
  });

  const records = React.useMemo(
    () => (interventionsQuery.data?.records || []).filter((item) => matchInterventionView(item, view)),
    [interventionsQuery.data?.records, view]
  );
  const selectedIntervention =
    records.find((item) => item.id === selectedInterventionId) || records[0] || null;
  const overdueCount = records.filter((item) => item.status !== 'COMPLETED' && !!item.plannedAt && new Date(item.plannedAt).getTime() < Date.now()).length;

  React.useEffect(() => {
    setPageNo(1);
  }, [classId, priority, studentUserId, view]);

  React.useEffect(() => {
    setSearchParams(
      buildInterventionSearch({
        view,
        priority,
        classId,
        studentUserId,
        pageNo,
        focusId: selectedInterventionId,
        source,
      }),
      { replace: true }
    );
  }, [classId, pageNo, priority, selectedInterventionId, setSearchParams, source, studentUserId, view]);

  React.useEffect(() => {
    if (!records.length) {
      setSelectedInterventionId(null);
      setForm(buildInterventionForm());
      return;
    }
    if (!selectedInterventionId || !records.some((item) => item.id === selectedInterventionId)) {
      setSelectedInterventionId(records[0].id);
    }
  }, [records, selectedInterventionId]);

  React.useEffect(() => {
    if (!selectedIntervention) {
      return;
    }
    setForm(buildInterventionForm(selectedIntervention));
  }, [selectedIntervention?.id]);

  return (
    <div className="space-y-8 pb-20">
      <PageHeader title="干预工作台" subtitle="从待办、排期到完成备注推进教师干预闭环；这里不只是看建议，而是把建议变成已执行动作。" />

      {source && (
        <div className="rounded-[1.8rem] border border-slate-200/80 bg-white/70 px-5 py-4 text-sm text-slate-600 dark:border-white/10 dark:bg-white/[0.03] dark:text-white/70">
          当前从教师工作台进入，筛选上下文会保留在 URL 里。你可以继续调整视图、班级或学生，刷新后仍会回到当前状态。
        </div>
      )}

      <section className="rounded-[2.5rem] liquid-glass-panel p-8">
        <div className="grid gap-4 lg:grid-cols-4">
          <select
            value={classId}
            onChange={(event) => setClassId(event.target.value)}
            className="w-full rounded-2xl border border-slate-200 dark:border-white/10 bg-white/70 dark:bg-white/5 px-4 py-3"
          >
            <option value="">全部班级</option>
            {(classesQuery.data || []).map((item) => (
              <option key={item.classId} value={item.classId}>
                {item.className}
              </option>
            ))}
          </select>

          <select
            value={view}
            onChange={(event) => setView(normalizeInterventionView(event.target.value))}
            className="w-full rounded-2xl border border-slate-200 dark:border-white/10 bg-white/70 dark:bg-white/5 px-4 py-3"
          >
            {VIEW_OPTIONS.map((item) => (
              <option key={item.value} value={item.value}>
                {item.label}
              </option>
            ))}
          </select>

          <select
            value={priority}
            onChange={(event) => setPriority(event.target.value as (typeof PRIORITY_OPTIONS)[number])}
            className="w-full rounded-2xl border border-slate-200 dark:border-white/10 bg-white/70 dark:bg-white/5 px-4 py-3"
          >
            {PRIORITY_OPTIONS.map((item) => (
              <option key={item} value={item}>
                {item === 'ALL' ? '全部优先级' : item}
              </option>
            ))}
          </select>

          <div className="rounded-2xl border border-slate-200/70 bg-white/60 px-4 py-3 text-sm text-slate-500 dark:border-white/10 dark:bg-white/5 dark:text-white/45">
            当前页 {records.length} 条，逾期 {overdueCount} 条
          </div>
        </div>

        {(studentUserId || classId) && (
          <div className="mt-4 flex flex-wrap gap-3 text-sm text-slate-500 dark:text-white/45">
            {classId && <span className="rounded-full border border-slate-200/70 px-3 py-1 dark:border-white/10">班级 #{classId}</span>}
            {studentUserId && (
              <span className="rounded-full border border-slate-200/70 px-3 py-1 dark:border-white/10">学生 #{studentUserId}</span>
            )}
            {studentUserId && (
              <button
                type="button"
                onClick={() => setStudentUserId('')}
                className="rounded-full border border-slate-200/70 px-3 py-1 dark:border-white/10"
              >
                清空学生上下文
              </button>
            )}
          </div>
        )}
      </section>

      <div className="grid gap-8 xl:grid-cols-[0.95fr_1.05fr]">
        <section className="rounded-[2.5rem] liquid-glass-panel p-8">
          <div className="space-y-4">
            {interventionsQuery.isLoading ? (
              <PanelSkeleton />
            ) : interventionsQuery.error ? (
              <div className="rounded-2xl border border-rose-500/20 bg-rose-500/5 px-4 py-3 text-sm text-rose-500">
                {getApiErrorMessage(interventionsQuery.error)}
              </div>
            ) : records.length ? (
              records.map((item) => (
                <button
                  key={item.id}
                  type="button"
                  onClick={() => setSelectedInterventionId(item.id)}
                  className={`w-full rounded-[1.8rem] border p-5 text-left transition-all ${
                    selectedIntervention?.id === item.id
                      ? 'border-primary/30 bg-primary/5'
                      : 'border-slate-200/70 bg-white/60 dark:border-white/10 dark:bg-white/5'
                  }`}
                >
                  <div className="flex flex-col gap-4 lg:flex-row lg:items-start justify-between">
                    <div className="space-y-2">
                      <div className="text-xs uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">
                        {item.className} · {item.priority} · {item.status}
                      </div>
                      <div className="text-xl font-black text-slate-900 dark:text-white">{item.studentName}</div>
                      <div className="text-sm font-bold text-slate-800 dark:text-white/85">{item.patternDetected}</div>
                      <div className="text-sm text-slate-500 dark:text-white/45 leading-6">{item.suggestedAction}</div>
                    </div>
                    <div className="text-right text-sm text-slate-500 dark:text-white/45">
                      <div>计划时间 {formatDateTime(item.plannedAt)}</div>
                      <div className="mt-2">完成时间 {formatDateTime(item.completedAt)}</div>
                    </div>
                  </div>
                </button>
              ))
            ) : (
              <div className="text-sm text-slate-500 dark:text-white/45">当前没有干预记录。</div>
            )}
          </div>

          <div className="mt-6 flex items-center justify-between text-sm text-slate-500 dark:text-white/45">
            <span>
              第 {pageNo}/{totalPages(interventionsQuery.data?.total)} 页
            </span>
            <div className="flex gap-3">
              <button
                type="button"
                onClick={() => setPageNo((current) => Math.max(1, current - 1))}
                disabled={pageNo === 1}
                className="rounded-full border border-slate-200 px-4 py-2 disabled:opacity-50 dark:border-white/10"
              >
                上一页
              </button>
              <button
                type="button"
                onClick={() => setPageNo((current) => Math.min(totalPages(interventionsQuery.data?.total), current + 1))}
                disabled={pageNo >= totalPages(interventionsQuery.data?.total)}
                className="rounded-full border border-slate-200 px-4 py-2 disabled:opacity-50 dark:border-white/10"
              >
                下一页
              </button>
            </div>
          </div>
        </section>

        <section className="rounded-[2.5rem] liquid-glass-panel p-8">
          <div className="flex items-center justify-between gap-4 mb-6">
            <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">干预编辑</div>
            {selectedIntervention && (
              <div className="text-sm text-slate-500 dark:text-white/45">最近更新 {formatDateTime(selectedIntervention.updatedAt)}</div>
            )}
          </div>

          {!selectedIntervention ? (
            <div className="rounded-2xl border border-slate-200 bg-white/70 px-4 py-8 text-sm text-slate-500 dark:border-white/10 dark:bg-white/5 dark:text-white/45">
              选择一条干预记录后，在这里编辑计划时间、优先级、状态和备注。
            </div>
          ) : (
            <div className="space-y-6">
              <div className="rounded-[1.8rem] border border-slate-200/70 bg-white/60 p-5 dark:border-white/10 dark:bg-white/5">
                <div className="flex flex-wrap items-center justify-between gap-4">
                  <div>
                    <div className="text-xl font-black text-slate-900 dark:text-white">{selectedIntervention.studentName}</div>
                    <div className="mt-2 text-sm text-slate-500 dark:text-white/45">{selectedIntervention.patternDetected}</div>
                  </div>
                  <div className="text-right text-sm text-slate-500 dark:text-white/45">
                    <div>{selectedIntervention.className}</div>
                    <div className="mt-2">{formatDateTime(selectedIntervention.plannedAt)}</div>
                  </div>
                </div>
              </div>

              <div className="grid gap-4 md:grid-cols-3">
                <label className="space-y-2 text-sm">
                  <span className="text-slate-500 dark:text-white/45">优先级</span>
                  <select
                    value={form.priority}
                    onChange={(event) => setForm((current) => ({ ...current, priority: event.target.value }))}
                    className="w-full rounded-2xl border border-slate-200 bg-white/70 px-4 py-3 dark:border-white/10 dark:bg-white/5"
                  >
                    {PRIORITY_OPTIONS.slice(1).map((item) => (
                      <option key={item} value={item}>
                        {item}
                      </option>
                    ))}
                  </select>
                </label>

                <label className="space-y-2 text-sm">
                  <span className="text-slate-500 dark:text-white/45">状态</span>
                  <select
                    value={form.status}
                    onChange={(event) => setForm((current) => ({ ...current, status: event.target.value }))}
                    className="w-full rounded-2xl border border-slate-200 bg-white/70 px-4 py-3 dark:border-white/10 dark:bg-white/5"
                  >
                    {STATUS_OPTIONS.slice(1).map((item) => (
                      <option key={item} value={item}>
                        {item}
                      </option>
                    ))}
                  </select>
                </label>

                <label className="space-y-2 text-sm">
                  <span className="text-slate-500 dark:text-white/45">计划时间</span>
                  <input
                    type="datetime-local"
                    value={form.plannedAt}
                    onChange={(event) => setForm((current) => ({ ...current, plannedAt: event.target.value }))}
                    className="w-full rounded-2xl border border-slate-200 bg-white/70 px-4 py-3 dark:border-white/10 dark:bg-white/5"
                  />
                </label>
              </div>

              <label className="block space-y-2 text-sm">
                <span className="text-slate-500 dark:text-white/45">教师备注</span>
                <textarea
                  value={form.teacherNote}
                  onChange={(event) => setForm((current) => ({ ...current, teacherNote: event.target.value }))}
                  rows={8}
                  className="w-full rounded-[1.8rem] border border-slate-200 bg-white/70 px-4 py-3 dark:border-white/10 dark:bg-white/5"
                  placeholder="记录执行方式、课堂安排、跟进时间或完成说明。"
                />
              </label>

              <div className="flex flex-wrap gap-3">
                <button
                  type="button"
                  onClick={() => saveMutation.mutate()}
                  disabled={saveMutation.isPending}
                  className="btn-liquid px-5 py-3 text-white disabled:opacity-60"
                >
                  保存更新
                </button>
                <button
                  type="button"
                  onClick={() => completeMutation.mutate()}
                  disabled={completeMutation.isPending}
                  className="rounded-full border border-slate-200 px-5 py-3 text-sm font-bold text-primary disabled:opacity-60 dark:border-white/10"
                >
                  标记完成
                </button>
              </div>

              {(saveMutation.error || completeMutation.error) && (
                <div className="rounded-2xl border border-rose-500/20 bg-rose-500/5 px-4 py-3 text-sm text-rose-500">
                  {getApiErrorMessage(saveMutation.error || completeMutation.error)}
                </div>
              )}
            </div>
          )}
        </section>
      </div>
    </div>
  );
};

export default TeacherInterventionsPage;
