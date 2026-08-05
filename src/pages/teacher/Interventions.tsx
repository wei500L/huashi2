/* eslint-disable react-refresh/only-export-components */
import React from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Eye, EyeOff, RefreshCw, ShieldCheck } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useSearchParams } from 'react-router-dom';
import { PageHeader, PanelSkeleton, SectionEyebrow, StatusBadge } from '@/components/common';
import InterventionEffectPanel from '@/features/interventions/InterventionEffectPanel';
import type { TeacherInterventionSummaryVO } from '@/lib/contracts';
import { teacherAnalyticsService, teacherInterventionService } from '@/lib/services';
import { formatDateTime, interventionPriorityLabel, interventionStatusLabel, interventionStatusTone } from '@/lib/format';
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

export function buildInterventionListParams(params: {
  classId: string;
  studentUserId: string;
  view: InterventionView;
  priority: string;
  pageNo: number;
  pageSize: number;
}) {
  return {
    classId: params.classId ? Number(params.classId) : undefined,
    studentUserId: params.studentUserId ? Number(params.studentUserId) : undefined,
    view: params.view,
    priority: params.priority === 'ALL' ? undefined : params.priority,
    pageNo: params.pageNo,
    pageSize: params.pageSize,
  };
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

function priorityTone(priority?: string | null): React.ComponentProps<typeof StatusBadge>['tone'] {
  switch (priority) {
    case 'URGENT':
      return 'danger';
    case 'NORMAL':
      return 'warning';
    default:
      return 'neutral';
  }
}

const TeacherInterventionsPage: React.FC = () => {
  const { t } = useTranslation();
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
  const [showIdentity, setShowIdentity] = React.useState(false);

  const maskStudentName = React.useCallback((name?: string | null) => {
    if (showIdentity || !name) return name || '学生';
    const chars = Array.from(name);
    return chars.length <= 1 ? '•' : `${chars[0]}${'•'.repeat(Math.min(chars.length - 1, 3))}`;
  }, [showIdentity]);

  const classesQuery = useQuery({
    queryKey: ['teacher-classes'],
    queryFn: ({ signal }) => teacherAnalyticsService.listClasses({ signal }),
  });

  const interventionsQuery = useQuery({
    queryKey: ['teacher-interventions', classId, studentUserId, view, priority, pageNo],
    queryFn: ({ signal }) =>
      teacherInterventionService.list(
        buildInterventionListParams({
          classId,
          studentUserId,
          view,
          priority,
          pageNo,
          pageSize,
        }),
        { signal }
      ),
  });

  const saveMutation = useMutation({
    mutationFn: () => {
      if (!selectedInterventionId) {
        throw new Error('请先选择一条干预记录。');
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
        throw new Error('请先选择一条干预记录。');
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

  const records = React.useMemo(() => interventionsQuery.data?.records || [], [interventionsQuery.data?.records]);
  const selectedIntervention = React.useMemo(
    () => records.find((item) => item.id === selectedInterventionId) || records[0] || null,
    [records, selectedInterventionId]
  );
  const overdueCount = React.useMemo(
    () => records.filter((item) => item.status !== 'COMPLETED' && !!item.plannedAt && new Date(item.plannedAt).getTime() < Date.now()).length,
    [records]
  );

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
  }, [selectedIntervention]);

  return (
    <div className="space-y-8 pb-20">
      <PageHeader
        eyebrow={t('taskPages.teacherInterventions.eyebrow')}
        title={t('taskPages.teacherInterventions.pageTitle')}
        subtitle={t('taskPages.teacherInterventions.pageSubtitle')}
        actions={
          <button
            type="button"
            onClick={() => setShowIdentity((current) => !current)}
            className="inline-flex items-center gap-2 rounded-xl border border-slate-200 px-3 py-2.5 text-xs font-bold text-slate-600 dark:border-white/10 dark:text-white/65"
            aria-pressed={showIdentity}
          >
            {showIdentity ? <EyeOff size={14} /> : <Eye size={14} />}
            {showIdentity ? '隐藏身份' : '显示身份'}
          </button>
        }
      />

      {source && (
        <div className="rounded-[1.8rem] border border-slate-200/80 bg-white/70 px-5 py-4 text-sm text-slate-600 dark:border-white/10 dark:bg-white/[0.03] dark:text-white/70">
          {t('taskPages.teacherInterventions.sourceHint')}
        </div>
      )}

      <section className="rounded-[2.5rem] liquid-glass-panel p-8">
        <div className="grid gap-4 lg:grid-cols-4">
          <select
            value={classId}
            onChange={(event) => setClassId(event.target.value)}
            className="native-select w-full rounded-2xl border border-slate-200 dark:border-white/10 bg-white/70 dark:bg-white/5 px-4 py-3"
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
            className="native-select w-full rounded-2xl border border-slate-200 dark:border-white/10 bg-white/70 dark:bg-white/5 px-4 py-3"
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
            className="native-select w-full rounded-2xl border border-slate-200 dark:border-white/10 bg-white/70 dark:bg-white/5 px-4 py-3"
          >
            {PRIORITY_OPTIONS.map((item) => (
              <option key={item} value={item}>
                {item === 'ALL' ? '全部优先级' : interventionPriorityLabel(item)}
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
          <div className="max-h-[760px] space-y-4 overflow-y-auto pr-1">
            {interventionsQuery.isLoading ? (
              <PanelSkeleton />
            ) : interventionsQuery.error ? (
              <div className="rounded-2xl border border-rose-500/20 bg-rose-500/5 px-4 py-3 text-sm text-rose-500">
                <div>{getApiErrorMessage(interventionsQuery.error)}</div>
                <button type="button" onClick={() => void interventionsQuery.refetch()} className="mt-3 inline-flex items-center gap-2 rounded-full border border-rose-500/30 px-4 py-2 text-xs font-bold"><RefreshCw size={13} /> 重试加载</button>
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
                      <div className="flex flex-wrap items-center gap-2 text-sm text-slate-500 dark:text-white/45">
                        <span>{item.className}</span>
                        <StatusBadge label={interventionPriorityLabel(item.priority)} tone={priorityTone(item.priority)} />
                        <StatusBadge label={interventionStatusLabel(item.status)} tone={interventionStatusTone(item.status)} />
                      </div>
                      <div className="text-xl font-black text-slate-900 dark:text-white">{maskStudentName(item.studentName)}</div>
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

        <section className="rounded-[2.5rem] liquid-glass-panel p-8 xl:sticky xl:top-6 xl:self-start">
          <div className="flex items-center justify-between gap-4 mb-6">
            <SectionEyebrow>{t('taskPages.teacherInterventions.editorEyebrow')}</SectionEyebrow>
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
                    <div className="text-xl font-black text-slate-900 dark:text-white">{maskStudentName(selectedIntervention.studentName)}</div>
                    <div className="mt-2 text-sm text-slate-500 dark:text-white/45">{selectedIntervention.patternDetected}</div>
                  </div>
                  <div className="text-right text-sm text-slate-500 dark:text-white/45">
                    <div>{selectedIntervention.className}</div>
                    <div className="mt-2">{formatDateTime(selectedIntervention.plannedAt)}</div>
                  </div>
                </div>
              </div>

              <InterventionEffectPanel effectTracking={selectedIntervention.effectTracking} />

              <div className="grid gap-4 md:grid-cols-3">
                <label className="space-y-2 text-sm">
                  <span className="text-slate-500 dark:text-white/45">优先级</span>
                  <select
                    value={form.priority}
                    onChange={(event) => setForm((current) => ({ ...current, priority: event.target.value }))}
                    className="native-select w-full rounded-2xl border border-slate-200 bg-white/70 px-4 py-3 dark:border-white/10 dark:bg-white/5"
                  >
                    {PRIORITY_OPTIONS.slice(1).map((item) => (
                      <option key={item} value={item}>
                        {interventionPriorityLabel(item)}
                      </option>
                    ))}
                  </select>
                </label>

                <label className="space-y-2 text-sm">
                  <span className="text-slate-500 dark:text-white/45">状态</span>
                  <select
                    value={form.status}
                    onChange={(event) => setForm((current) => ({ ...current, status: event.target.value }))}
                    className="native-select w-full rounded-2xl border border-slate-200 bg-white/70 px-4 py-3 dark:border-white/10 dark:bg-white/5"
                  >
                    {STATUS_OPTIONS.slice(1).map((item) => (
                      <option key={item} value={item}>
                        {interventionStatusLabel(item)}
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
                  <div className="flex items-start gap-2"><ShieldCheck size={15} className="mt-0.5" /><span>{getApiErrorMessage(saveMutation.error || completeMutation.error)}</span></div>
                  <button type="button" onClick={() => (saveMutation.error ? saveMutation.mutate() : completeMutation.mutate())} className="mt-3 inline-flex items-center gap-2 rounded-full border border-rose-500/30 px-4 py-2 text-xs font-bold"><RefreshCw size={13} /> 重试保存</button>
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
