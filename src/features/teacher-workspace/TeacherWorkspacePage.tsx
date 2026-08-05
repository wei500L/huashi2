import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowRight, ClipboardCheck, FileEdit, ListChecks, Users } from 'lucide-react';
import { PanelSkeleton } from '@/components/common';
import { diagnosisTemplateSyncStateLabel, formatDateTime, interventionPriorityLabel } from '@/lib/format';
import { teacherWorkspaceService } from '@/lib/services';
import { teacherWorkspaceQueryKeys } from './queryKeys';
import {
  buildTeacherWorkspaceEmptyState,
  buildTeacherWorkspaceOnboardingCard,
  buildTeacherWorkspaceTodoItems,
  buildWorkspaceLink,
} from './copy';
import {
  ActionGrid,
  MetricGrid,
  StatusBanner,
  WorkspaceEmptyState,
  WorkspaceHero,
  WorkspaceSectionHeader,
} from './components';

type RecentActivity = {
  id: string;
  label: string;
  title: string;
  meta: string;
  time?: string | null;
  to: string;
  icon: React.ComponentType<{ size?: number; className?: string }>;
};

const TeacherWorkspacePage: React.FC = () => {
  const { t } = useTranslation();
  const [showAllTodos, setShowAllTodos] = React.useState(false);
  const [showOnboardingCard, setShowOnboardingCard] = React.useState(false);
  const overviewQuery = useQuery({
    queryKey: teacherWorkspaceQueryKeys.overview(),
    queryFn: ({ signal }) => teacherWorkspaceService.getOverview({ signal }),
  });

  const todoItems = React.useMemo(() => buildTeacherWorkspaceTodoItems(t, overviewQuery.data), [overviewQuery.data, t]);
  const onboardingCard = React.useMemo(
    () => buildTeacherWorkspaceOnboardingCard(t, overviewQuery.data),
    [overviewQuery.data, t]
  );

  React.useEffect(() => {
    if (!onboardingCard) {
      setShowOnboardingCard(false);
      return;
    }
    if (typeof window === 'undefined') return;

    const storageKey = `teacher-workspace-onboarding-seen:${overviewQuery.data?.teacherName || 'default'}`;
    const seen = window.localStorage.getItem(storageKey) === '1';
    setShowOnboardingCard(!seen);
    if (!seen) window.localStorage.setItem(storageKey, '1');
  }, [onboardingCard, overviewQuery.data?.teacherName]);

  const dedupedTodoItems = React.useMemo(() => {
    if (!showOnboardingCard || !onboardingCard) return todoItems;
    const onboardingTodoIdMap: Record<string, string> = {
      'setup-classes': 'classes-empty',
      'setup-draft': 'drafts-empty',
      'setup-list': 'lists-empty',
      'setup-assessment': 'assessments-empty',
    };
    const duplicateTodoId = onboardingTodoIdMap[onboardingCard.id];
    return duplicateTodoId ? todoItems.filter((item) => item.id !== duplicateTodoId) : todoItems;
  }, [onboardingCard, showOnboardingCard, todoItems]);

  const visibleTodoItems = React.useMemo(
    () => (showAllTodos ? dedupedTodoItems : dedupedTodoItems.slice(0, 4)),
    [dedupedTodoItems, showAllTodos]
  );
  const overflowTodoCount = Math.max(dedupedTodoItems.length - 4, 0);

  const quickActions = React.useMemo(
    () => [
      {
        id: 'create-draft',
        label: t('teacherWorkspace.quickActions.createDraft'),
        description: t('teacherWorkspace.quickActions.createDraftDescription'),
        to: buildWorkspaceLink('/teacher/diagnosis-templates', { intent: 'create-draft', source: 'workspace' }),
      },
      {
        id: 'assessments',
        label: t('teacherWorkspace.quickActions.assessments'),
        description: t('teacherWorkspace.quickActions.assessmentsDescription'),
        to: buildWorkspaceLink('/teacher/assessments', { source: 'workspace' }),
      },
      {
        id: 'interventions',
        label: t('teacherWorkspace.quickActions.interventions'),
        description: t('teacherWorkspace.quickActions.interventionsDescription'),
        to: buildWorkspaceLink('/teacher/interventions', { view: 'pending', source: 'workspace' }),
      },
      {
        id: 'classes',
        label: t('teacherWorkspace.quickActions.classes'),
        description: t('teacherWorkspace.quickActions.classesDescription'),
        to: buildWorkspaceLink('/teacher/classes', { source: 'workspace' }),
      },
      {
        id: 'lexical-pairs',
        label: t('teacherWorkspace.quickActions.lexicalPairs'),
        description: t('teacherWorkspace.quickActions.lexicalPairsDescription'),
        to: buildWorkspaceLink('/teacher/lexical-pairs', { source: 'workspace' }),
      },
      {
        id: 'lexical-lists',
        label: t('teacherWorkspace.quickActions.lexicalLists'),
        description: t('teacherWorkspace.quickActions.lexicalListsDescription'),
        to: buildWorkspaceLink('/teacher/lexical-lists', { source: 'workspace' }),
      },
    ],
    [t]
  );

  const metricItems = React.useMemo(
    () => [
      {
        id: 'classes',
        label: t('teacherWorkspace.metrics.classes'),
        value: overviewQuery.data?.summary.classCount ?? '--',
        hint: t('teacherWorkspace.metrics.classesHint'),
      },
      {
        id: 'students',
        label: t('teacherWorkspace.metrics.students'),
        value: overviewQuery.data?.summary.studentCount ?? '--',
        hint: t('teacherWorkspace.metrics.studentsHint'),
      },
      {
        id: 'interventions',
        label: t('teacherWorkspace.sections.interventionsSubtitle'),
        value: overviewQuery.data?.summary.pendingInterventionCount ?? '--',
        hint: t('teacherWorkspace.emptyInterventionsDescription'),
      },
      {
        id: 'drafts',
        label: t('teacherWorkspace.metrics.drafts'),
        value: overviewQuery.data?.summary.draftTemplateCount ?? '--',
        hint: t('teacherWorkspace.metrics.draftsHint'),
      },
      {
        id: 'assessments',
        label: t('teacherWorkspace.metrics.assessments'),
        value: overviewQuery.data
          ? `${overviewQuery.data.summary.assessmentPaperCount}/${overviewQuery.data.summary.activeAssessmentPublishCount}`
          : '--',
        hint: t('teacherWorkspace.metrics.assessmentsHint', {
          pendingCount: overviewQuery.data?.summary.pendingAssessmentSubmissionCount ?? 0,
        }),
      },
    ],
    [overviewQuery.data, t]
  );

  const recentActivity = React.useMemo<RecentActivity[]>(() => {
    if (!overviewQuery.data) return [];
    const activities: RecentActivity[] = [
      ...overviewQuery.data.recentAssessmentPublishes.map((item) => ({
        id: `assessment-${item.publishId}`,
        label: t('teacherWorkspace.sections.assessments'),
        title: item.title,
        meta: `${item.className} · ${t('teacherWorkspace.assessmentMeta', {
          assignedCount: item.assignedCount,
          submittedCount: item.submittedCount,
          pendingCount: item.pendingCount,
        })}`,
        time: item.publishedAt || item.dueAt,
        to: buildWorkspaceLink(`/teacher/assessments/publishes/${item.publishId}`, { source: 'workspace' }),
        icon: ClipboardCheck,
      })),
      ...overviewQuery.data.draftTemplates.map((item) => ({
        id: `draft-${item.draftId}`,
        label: t('teacherWorkspace.sections.drafts'),
        title: item.templateName,
        meta: diagnosisTemplateSyncStateLabel(item.syncState),
        time: item.updatedAt,
        to: buildWorkspaceLink(`/teacher/diagnosis-template-drafts/${item.draftId}`, { step: 4, source: 'workspace' }),
        icon: FileEdit,
      })),
      ...overviewQuery.data.recentClasses.map((item) => ({
        id: `class-${item.classId}`,
        label: t('teacherWorkspace.sections.classes'),
        title: item.className,
        meta: t('teacherWorkspace.classMeta', {
          studentCount: item.studentCount,
          highRiskStudentCount: item.highRiskStudentCount,
        }),
        time: item.lastActiveAt,
        to: buildWorkspaceLink(`/teacher/classes/${item.classId}`, { source: 'workspace' }),
        icon: Users,
      })),
      ...overviewQuery.data.recentLexicalLists.map((item) => ({
        id: `list-${item.id}`,
        label: t('teacherWorkspace.sections.lexicalLists'),
        title: item.listName,
        meta: t('teacherWorkspace.lexicalListMeta', { itemCount: item.itemCount, updatedAt: formatDateTime(item.updatedAt) }),
        time: item.updatedAt,
        to: buildWorkspaceLink('/teacher/lexical-lists', { listId: item.id, source: 'workspace' }),
        icon: ListChecks,
      })),
    ];

    return activities
      .sort((left, right) => new Date(right.time || 0).getTime() - new Date(left.time || 0).getTime())
      .slice(0, 7);
  }, [overviewQuery.data, t]);

  const riskClasses = React.useMemo(
    () => (overviewQuery.data?.recentClasses || []).filter((item) => item.highRiskStudentCount > 0),
    [overviewQuery.data?.recentClasses]
  );

  return (
    <div className="space-y-6 pb-16">
      <WorkspaceHero
        eyebrow={t('teacherWorkspace.heroEyebrow')}
        title={t('teacherWorkspace.heroTitle', {
          teacherName: overviewQuery.data?.teacherName || t('teacherWorkspace.defaultTeacherName'),
        })}
        subtitle={t('teacherWorkspace.heroSubtitle')}
        meta={overviewQuery.data?.organizationLabel || t('teacherWorkspace.organizationFallback')}
        actions={
          <div className="flex flex-wrap gap-2">
            <Link
              to={buildWorkspaceLink('/teacher/classes', { source: 'workspace' })}
              className="rounded-xl border border-slate-200 px-3.5 py-2 text-xs font-bold text-slate-700 dark:border-white/10 dark:text-white/75"
            >
              {t('teacherWorkspace.actions.classes')}
            </Link>
            <Link
              to={buildWorkspaceLink('/teacher/diagnosis-templates', { intent: 'create-draft', source: 'workspace' })}
              className="rounded-xl bg-primary px-3.5 py-2 text-xs font-black text-white"
            >
              {t('teacherWorkspace.actions.templates')}
            </Link>
          </div>
        }
      />

      {overviewQuery.error ? (
        <div className="rounded-xl border border-rose-500/20 bg-rose-500/5 px-4 py-3 text-sm text-rose-500">
          {overviewQuery.error.message}
        </div>
      ) : null}

      {overviewQuery.isLoading && !overviewQuery.data ? (
        <div className="grid gap-4"><PanelSkeleton /><PanelSkeleton /></div>
      ) : (
        <>
          <MetricGrid items={metricItems} />

          {showOnboardingCard && onboardingCard ? (
            <section className="overflow-hidden rounded-2xl border border-slate-200/80 bg-white/70 dark:border-white/10 dark:bg-white/[0.03]">
              <StatusBanner {...onboardingCard} />
            </section>
          ) : null}

          <div className="grid gap-5 xl:grid-cols-[1.35fr_0.65fr]">
            <section className="overflow-hidden rounded-2xl border border-slate-200/80 bg-white/70 dark:border-white/10 dark:bg-white/[0.03]">
              <div className="border-b border-slate-200/70 px-4 py-3 dark:border-white/10">
                <WorkspaceSectionHeader
                  eyebrow={t('teacherWorkspace.todayTodoTitle')}
                  title={t('teacherWorkspace.todayTodoSubtitle')}
                  action={<span className="text-xs font-black tabular-nums text-amber-600 dark:text-amber-300">{dedupedTodoItems.length}</span>}
                />
              </div>
              {visibleTodoItems.map((item, index) => <StatusBanner key={item.id} {...item} index={index} />)}
              {overflowTodoCount > 0 ? (
                <button
                  type="button"
                  onClick={() => setShowAllTodos((current) => !current)}
                  className="w-full border-t border-slate-200/70 px-4 py-2.5 text-left text-xs font-bold text-primary dark:border-white/10"
                >
                  {showAllTodos
                    ? t('teacherWorkspace.todayTodoCollapse')
                    : t('teacherWorkspace.todayTodoExpand', { count: overflowTodoCount })}
                </button>
              ) : null}
            </section>

            <section className="rounded-2xl border border-slate-200/80 bg-white/70 p-4 dark:border-white/10 dark:bg-white/[0.03]">
              <WorkspaceSectionHeader
                eyebrow={t('teacherWorkspace.riskStudentsEyebrow')}
                title={t('teacherWorkspace.riskStudentsTitle')}
                action={
                  <Link to={buildWorkspaceLink('/teacher/interventions', { view: 'pending', source: 'workspace' })} className="text-xs font-bold text-primary">
                    {t('teacherWorkspace.viewAll')}
                  </Link>
                }
              />
              <div className="mt-3 divide-y divide-slate-200/70 dark:divide-white/10">
                {(overviewQuery.data?.pendingInterventions || []).slice(0, 4).map((item) => (
                  <Link
                    key={item.id}
                    to={buildWorkspaceLink('/teacher/interventions', {
                      view: 'pending', focusId: item.id, classId: item.classId, studentUserId: item.studentUserId, source: 'workspace',
                    })}
                    className="flex items-center justify-between gap-3 py-2.5 first:pt-0 last:pb-0"
                  >
                    <div className="min-w-0">
                      <div className="truncate text-sm font-bold text-slate-900 dark:text-white">{item.studentName || '--'}</div>
                      <div className="mt-0.5 text-xs text-slate-500 dark:text-white/45">{interventionPriorityLabel(item.priority)} · {formatDateTime(item.plannedAt)}</div>
                    </div>
                    <ArrowRight size={13} className="shrink-0 text-slate-400" />
                  </Link>
                ))}
                {!overviewQuery.data?.pendingInterventions.length && riskClasses.slice(0, 3).map((item) => (
                  <Link key={item.classId} to={buildWorkspaceLink(`/teacher/classes/${item.classId}`, { source: 'workspace' })} className="flex items-center justify-between gap-3 py-2.5 first:pt-0 last:pb-0">
                    <span className="min-w-0 truncate text-sm font-bold text-slate-900 dark:text-white" title={item.className}>{item.className}</span>
                    <span className="shrink-0 text-xs font-black text-rose-600 dark:text-rose-300">{t('teacherWorkspace.riskStudentCount', { count: item.highRiskStudentCount })}</span>
                  </Link>
                ))}
                {!overviewQuery.data?.pendingInterventions.length && !riskClasses.length ? (
                  <div className="py-5 text-xs leading-5 text-slate-500 dark:text-white/45">{t('teacherWorkspace.emptyInterventionsDescription')}</div>
                ) : null}
              </div>
            </section>
          </div>

          <div className="grid gap-5 xl:grid-cols-[1.35fr_0.65fr]">
            <section className="rounded-2xl border border-slate-200/80 bg-white/70 p-4 dark:border-white/10 dark:bg-white/[0.03]">
              <WorkspaceSectionHeader eyebrow={t('teacherWorkspace.recentActivityEyebrow')} title={t('teacherWorkspace.recentActivityTitle')} />
              <div className="mt-3 divide-y divide-slate-200/70 dark:divide-white/10">
                {recentActivity.map((item) => {
                  const Icon = item.icon;
                  return (
                    <Link key={item.id} to={item.to} className="grid grid-cols-[auto_minmax(0,1fr)_auto] items-center gap-3 py-2.5 first:pt-0 last:pb-0">
                      <span className="rounded-lg bg-slate-100 p-2 text-slate-500 dark:bg-white/[0.06] dark:text-white/50"><Icon size={14} /></span>
                      <span className="min-w-0">
                        <span className="block text-[10px] font-bold uppercase tracking-[0.16em] text-slate-400 dark:text-white/30">{item.label}</span>
                        <span className="mt-0.5 block truncate text-sm font-bold text-slate-900 dark:text-white" title={item.title}>{item.title}</span>
                        <span className="mt-0.5 block truncate text-xs text-slate-500 dark:text-white/45" title={item.meta}>{item.meta}</span>
                      </span>
                      <span className="hidden shrink-0 text-xs tabular-nums text-slate-400 sm:block">{formatDateTime(item.time)}</span>
                    </Link>
                  );
                })}
                {!recentActivity.length ? <div className="py-5 text-xs text-slate-500 dark:text-white/45">{t('teacherWorkspace.noRecentActivity')}</div> : null}
              </div>
            </section>

            <section className="rounded-2xl border border-slate-200/80 bg-white/70 p-4 dark:border-white/10 dark:bg-white/[0.03]">
              <WorkspaceSectionHeader
                eyebrow={t('teacherWorkspace.sections.classes')}
                title={t('teacherWorkspace.sections.classesSubtitle')}
                action={<Link to={buildWorkspaceLink('/teacher/classes', { source: 'workspace' })} className="text-xs font-bold text-primary">{t('teacherWorkspace.viewAll')}</Link>}
              />
              <div className="mt-3 space-y-2">
                {overviewQuery.data?.recentClasses.length ? overviewQuery.data.recentClasses.slice(0, 4).map((item) => (
                  <Link key={item.classId} to={buildWorkspaceLink(`/teacher/classes/${item.classId}`, { source: 'workspace' })} className="block rounded-xl border border-slate-200/70 px-3 py-2.5 transition-colors hover:border-primary/35 dark:border-white/10">
                    <div className="flex items-center justify-between gap-3">
                      <div className="min-w-0">
                        <div className="truncate text-sm font-bold text-slate-900 dark:text-white" title={item.className}>{item.className}</div>
                        <div className="mt-0.5 truncate text-xs text-slate-500 dark:text-white/45">{item.classCode} · {t('teacherWorkspace.classMeta', { studentCount: item.studentCount, highRiskStudentCount: item.highRiskStudentCount })}</div>
                      </div>
                      <ArrowRight size={13} className="shrink-0 text-slate-400" />
                    </div>
                  </Link>
                )) : <WorkspaceEmptyState {...buildTeacherWorkspaceEmptyState(t, 'classes', overviewQuery.data)} />}
              </div>
            </section>
          </div>

          <ActionGrid title={t('teacherWorkspace.quickActionsTitle')} description={t('teacherWorkspace.quickActionsSubtitle')} actions={quickActions} />
        </>
      )}
    </div>
  );
};

export default TeacherWorkspacePage;
