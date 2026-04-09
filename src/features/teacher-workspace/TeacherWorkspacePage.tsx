import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { PageHeader, PanelSkeleton } from '@/components/common';
import {
  diagnosisTemplateSyncStateLabel,
  formatDateTime,
  interventionPriorityLabel,
  interventionStatusLabel,
} from '@/lib/format';
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
} from './components';

const TeacherWorkspacePage: React.FC = () => {
  const { t } = useTranslation();
  const [showAllTodos, setShowAllTodos] = React.useState(false);
  const [showOnboardingCard, setShowOnboardingCard] = React.useState(false);
  const overviewQuery = useQuery({
    queryKey: teacherWorkspaceQueryKeys.overview(),
    queryFn: ({ signal }) => teacherWorkspaceService.getOverview({ signal }),
  });

  const todoItems = React.useMemo(
    () => buildTeacherWorkspaceTodoItems(t, overviewQuery.data),
    [overviewQuery.data, t]
  );

  const onboardingCard = React.useMemo(
    () => buildTeacherWorkspaceOnboardingCard(t, overviewQuery.data),
    [overviewQuery.data, t]
  );

  React.useEffect(() => {
    if (!onboardingCard) {
      setShowOnboardingCard(false);
      return;
    }
    if (typeof window === 'undefined') {
      return;
    }

    const storageKey = `teacher-workspace-onboarding-seen:${overviewQuery.data?.teacherName || 'default'}`;
    const seen = window.localStorage.getItem(storageKey) === '1';
    setShowOnboardingCard(!seen);
    if (!seen) {
      window.localStorage.setItem(storageKey, '1');
    }
  }, [onboardingCard, overviewQuery.data?.teacherName]);

  const dedupedTodoItems = React.useMemo(() => {
    if (!showOnboardingCard || !onboardingCard) {
      return todoItems;
    }

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
        to: buildWorkspaceLink('/teacher/diagnosis-templates', {
          intent: 'create-draft',
          source: 'workspace',
        }),
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
      {
        id: 'interventions',
        label: t('teacherWorkspace.quickActions.interventions'),
        description: t('teacherWorkspace.quickActions.interventionsDescription'),
        to: buildWorkspaceLink('/teacher/interventions', {
          view: 'pending',
          source: 'workspace',
        }),
      },
      {
        id: 'assessments',
        label: t('teacherWorkspace.quickActions.assessments'),
        description: t('teacherWorkspace.quickActions.assessmentsDescription'),
        to: buildWorkspaceLink('/teacher/assessments', { source: 'workspace' }),
      },
      {
        id: 'classes',
        label: t('teacherWorkspace.quickActions.classes'),
        description: t('teacherWorkspace.quickActions.classesDescription'),
        to: buildWorkspaceLink('/teacher/classes', { source: 'workspace' }),
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
        id: 'drafts',
        label: t('teacherWorkspace.metrics.drafts'),
        value: overviewQuery.data?.summary.draftTemplateCount ?? '--',
        hint: t('teacherWorkspace.metrics.draftsHint'),
      },
      {
        id: 'assets',
        label: t('teacherWorkspace.metrics.assets'),
        value:
          overviewQuery.data
            ? `${overviewQuery.data.summary.lexicalPairCount}/${overviewQuery.data.summary.lexicalListCount}`
            : '--',
        hint: t('teacherWorkspace.metrics.assetsHint'),
      },
      {
        id: 'assessments',
        label: t('teacherWorkspace.metrics.assessments'),
        value:
          overviewQuery.data
            ? `${overviewQuery.data.summary.assessmentPaperCount}/${overviewQuery.data.summary.activeAssessmentPublishCount}`
            : '--',
        hint: t('teacherWorkspace.metrics.assessmentsHint', {
          pendingCount: overviewQuery.data?.summary.pendingAssessmentSubmissionCount ?? 0,
        }),
      },
    ],
    [overviewQuery.data, t]
  );

  return (
    <div className="space-y-8 pb-20">
      <PageHeader
        title={t('teacherWorkspace.pageTitle')}
        subtitle={t('teacherWorkspace.pageSubtitle')}
        actions={
          <div className="flex flex-wrap gap-3">
            <Link
              to={buildWorkspaceLink('/teacher/diagnosis-templates', { source: 'workspace' })}
              className="rounded-full border border-slate-200 px-4 py-3 text-sm dark:border-white/10"
            >
              {t('teacherWorkspace.actions.templates')}
            </Link>
            <Link to={buildWorkspaceLink('/teacher/classes', { source: 'workspace' })} className="btn-liquid px-5 py-3 text-white">
              {t('teacherWorkspace.actions.classes')}
            </Link>
          </div>
        }
      />

      {overviewQuery.error && (
        <div className="rounded-[2rem] border border-rose-500/20 bg-rose-500/5 p-6 text-rose-500">
          {overviewQuery.error.message}
        </div>
      )}

      {overviewQuery.isLoading && !overviewQuery.data ? (
        <div className="grid gap-8">
          <PanelSkeleton />
          <PanelSkeleton />
        </div>
      ) : (
        <>
          <WorkspaceHero
            eyebrow={t('teacherWorkspace.heroEyebrow')}
            title={t('teacherWorkspace.heroTitle', { teacherName: overviewQuery.data?.teacherName || t('teacherWorkspace.defaultTeacherName') })}
            subtitle={t('teacherWorkspace.heroSubtitle')}
            meta={overviewQuery.data?.organizationLabel || t('teacherWorkspace.organizationFallback')}
          />

          {showOnboardingCard && onboardingCard && (
            <section className="space-y-4">
              <div className="space-y-2">
                <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">
                  {t('teacherWorkspace.onboardingCardTitle')}
                </div>
                <div className="text-sm text-slate-500 dark:text-white/45">
                  {t('teacherWorkspace.onboardingCardSubtitle')}
                </div>
              </div>
              <StatusBanner
                title={onboardingCard.title}
                description={onboardingCard.description}
                actionLabel={onboardingCard.actionLabel}
                to={onboardingCard.to}
                tone={onboardingCard.tone}
              />
            </section>
          )}

          {!!dedupedTodoItems.length && (
            <section className="space-y-4">
              <div className="space-y-2">
                <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">
                  {t('teacherWorkspace.todayTodoTitle')}
                </div>
                <div className="text-sm text-slate-500 dark:text-white/45">
                  {t('teacherWorkspace.todayTodoSubtitle')}
                </div>
              </div>
              {visibleTodoItems.map((item) => (
                <StatusBanner
                  key={item.id}
                  title={item.title}
                  description={item.description}
                  actionLabel={item.actionLabel}
                  to={item.to}
                  tone={item.tone}
                />
              ))}
              {overflowTodoCount > 0 && (
                <button
                  type="button"
                  onClick={() => setShowAllTodos((current) => !current)}
                  className="inline-flex items-center rounded-full border border-slate-200/80 bg-white/70 px-4 py-2 text-sm font-bold text-slate-600 transition-all hover:border-primary/40 hover:text-primary dark:border-white/10 dark:bg-white/5 dark:text-white/60"
                >
                  {showAllTodos
                    ? t('teacherWorkspace.todayTodoCollapse')
                    : t('teacherWorkspace.todayTodoExpand', { count: overflowTodoCount })}
                </button>
              )}
            </section>
          )}

          <ActionGrid
            title={t('teacherWorkspace.quickActionsTitle')}
            description={t('teacherWorkspace.quickActionsSubtitle')}
            actions={quickActions}
          />

          <MetricGrid items={metricItems} />

          <section className="rounded-[2.5rem] liquid-glass-panel p-8">
            <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">
              {t('teacherWorkspace.sections.assessments')}
            </div>
            <div className="mt-3 text-2xl font-black text-slate-900 dark:text-white">
              {t('teacherWorkspace.sections.assessmentsSubtitle')}
            </div>
            <div className="mt-6 space-y-4">
              {overviewQuery.data?.recentAssessmentPublishes.length ? (
                overviewQuery.data.recentAssessmentPublishes.map((item) => (
                  <Link
                    key={item.publishId}
                    to={buildWorkspaceLink(`/teacher/assessments/publishes/${item.publishId}`, { source: 'workspace' })}
                    className="block rounded-[1.8rem] border border-slate-200/80 bg-white/65 p-5 transition-all hover:border-primary/40 dark:border-white/10 dark:bg-white/5"
                  >
                    <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                      <div>
                        <div className="text-xs uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">
                          {item.className}
                        </div>
                        <div className="mt-2 text-xl font-black text-slate-900 dark:text-white">{item.title}</div>
                        <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                          {t('teacherWorkspace.assessmentMeta', {
                            assignedCount: item.assignedCount,
                            submittedCount: item.submittedCount,
                            pendingCount: item.pendingCount,
                          })}
                        </div>
                      </div>
                      <div className="text-sm text-slate-500 dark:text-white/45">
                        {t('teacherWorkspace.assessmentDue', { dueAt: formatDateTime(item.dueAt) })}
                      </div>
                    </div>
                  </Link>
                ))
              ) : (
                <WorkspaceEmptyState
                  {...buildTeacherWorkspaceEmptyState(t, 'assessments', overviewQuery.data)}
                />
              )}
            </div>
          </section>

          <div className="grid gap-8 xl:grid-cols-[1.05fr_0.95fr]">
            <section className="rounded-[2.5rem] liquid-glass-panel p-8">
              <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">
                {t('teacherWorkspace.sections.classes')}
              </div>
              <div className="mt-3 text-2xl font-black text-slate-900 dark:text-white">
                {t('teacherWorkspace.sections.classesSubtitle')}
              </div>
              <div className="mt-6 space-y-4">
                {overviewQuery.data?.recentClasses.length ? (
                  overviewQuery.data.recentClasses.map((item) => (
                    <Link
                      key={item.classId}
                      to={buildWorkspaceLink(`/teacher/classes/${item.classId}`, { source: 'workspace' })}
                      className="block rounded-[1.8rem] border border-slate-200/80 bg-white/65 p-5 transition-all hover:border-primary/40 dark:border-white/10 dark:bg-white/5"
                    >
                      <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
                        <div>
                          <div className="text-xs uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">
                            {item.classCode}
                          </div>
                          <div className="mt-2 text-xl font-black text-slate-900 dark:text-white">{item.className}</div>
                          <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                            {t('teacherWorkspace.classMeta', {
                              studentCount: item.studentCount,
                              highRiskStudentCount: item.highRiskStudentCount,
                            })}
                          </div>
                        </div>
                        <div className="text-sm text-slate-500 dark:text-white/45">
                          {t('teacherWorkspace.lastActive', { time: formatDateTime(item.lastActiveAt) })}
                        </div>
                      </div>
                    </Link>
                  ))
                ) : (
                  <WorkspaceEmptyState
                    {...buildTeacherWorkspaceEmptyState(t, 'classes', overviewQuery.data)}
                  />
                )}
              </div>
            </section>

            <section className="space-y-8">
              <section className="rounded-[2.5rem] liquid-glass-panel p-8">
                <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">
                  {t('teacherWorkspace.sections.drafts')}
                </div>
                <div className="mt-3 text-2xl font-black text-slate-900 dark:text-white">
                  {t('teacherWorkspace.sections.draftsSubtitle')}
                </div>
                <div className="mt-6 space-y-4">
                  {overviewQuery.data?.draftTemplates.length ? (
                    overviewQuery.data.draftTemplates.map((draft) => (
                      <Link
                        key={draft.draftId}
                        to={buildWorkspaceLink(`/teacher/diagnosis-template-drafts/${draft.draftId}`, {
                          step: 4,
                          source: 'workspace',
                        })}
                        className="block rounded-[1.6rem] border border-slate-200/80 bg-white/65 p-4 transition-all hover:border-primary/40 dark:border-white/10 dark:bg-white/5"
                      >
                        <div className="flex items-start justify-between gap-4">
                          <div>
                            <div className="font-black text-slate-900 dark:text-white">{draft.templateName}</div>
                            <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                              {t('teacherWorkspace.draftMeta', {
                                syncState: diagnosisTemplateSyncStateLabel(draft.syncState),
                                updatedAt: formatDateTime(draft.updatedAt),
                              })}
                            </div>
                          </div>
                        </div>
                      </Link>
                    ))
                  ) : (
                    <WorkspaceEmptyState
                      {...buildTeacherWorkspaceEmptyState(t, 'drafts', overviewQuery.data)}
                    />
                  )}
                </div>
              </section>

              <section className="rounded-[2.5rem] liquid-glass-panel p-8">
                <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">
                  {t('teacherWorkspace.sections.interventions')}
                </div>
                <div className="mt-3 text-2xl font-black text-slate-900 dark:text-white">
                  {t('teacherWorkspace.sections.interventionsSubtitle')}
                </div>
                <div className="mt-6 space-y-4">
                  {overviewQuery.data?.pendingInterventions.length ? (
                    overviewQuery.data.pendingInterventions.map((item) => (
                      <Link
                        key={item.id}
                        to={buildWorkspaceLink('/teacher/interventions', {
                          view: 'pending',
                          focusId: item.id,
                          classId: item.classId,
                          studentUserId: item.studentUserId,
                          source: 'workspace',
                        })}
                        className="block rounded-[1.6rem] border border-slate-200/80 bg-white/65 p-4 transition-all hover:border-primary/40 dark:border-white/10 dark:bg-white/5"
                      >
                        <div className="font-black text-slate-900 dark:text-white">{item.studentName || '--'}</div>
                        <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                          {t('teacherWorkspace.interventionMeta', {
                            priority: interventionPriorityLabel(item.priority),
                            status: interventionStatusLabel(item.status),
                            plannedAt: formatDateTime(item.plannedAt),
                          })}
                        </div>
                      </Link>
                    ))
                  ) : (
                    <WorkspaceEmptyState
                      {...buildTeacherWorkspaceEmptyState(t, 'interventions', overviewQuery.data)}
                    />
                  )}
                </div>
              </section>

              <section className="rounded-[2.5rem] liquid-glass-panel p-8">
                <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">
                  {t('teacherWorkspace.sections.lexicalLists')}
                </div>
                <div className="mt-3 text-2xl font-black text-slate-900 dark:text-white">
                  {t('teacherWorkspace.sections.lexicalListsSubtitle')}
                </div>
                <div className="mt-6 space-y-4">
                  {overviewQuery.data?.recentLexicalLists.length ? (
                    overviewQuery.data.recentLexicalLists.map((item) => (
                      <Link
                        key={item.id}
                        to={buildWorkspaceLink('/teacher/lexical-lists', {
                          listId: item.id,
                          source: 'workspace',
                        })}
                        className="block rounded-[1.6rem] border border-slate-200/80 bg-white/65 p-4 transition-all hover:border-primary/40 dark:border-white/10 dark:bg-white/5"
                      >
                        <div className="font-black text-slate-900 dark:text-white">{item.listName}</div>
                        <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                          {t('teacherWorkspace.lexicalListMeta', {
                            itemCount: item.itemCount,
                            updatedAt: formatDateTime(item.updatedAt),
                          })}
                        </div>
                      </Link>
                    ))
                  ) : (
                    <WorkspaceEmptyState
                      {...buildTeacherWorkspaceEmptyState(t, 'lexicalLists', overviewQuery.data)}
                    />
                  )}
                </div>
              </section>
            </section>
          </div>
        </>
      )}
    </div>
  );
};

export default TeacherWorkspacePage;
