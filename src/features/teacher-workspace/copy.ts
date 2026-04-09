import type { TFunction } from 'i18next';
import type { TeacherWorkspaceOverviewVO, TeacherWorkspaceSummaryVO } from '@/lib/contracts';

export type WorkspaceFocusItem = {
  id: string;
  title: string;
  description: string;
  to: string;
  actionLabel: string;
  tone: 'action' | 'attention' | 'stable';
};

export type WorkspaceEmptyStateConfig = {
  title: string;
  description: string;
  actionLabel: string;
  to: string;
};

type WorkspaceSection = 'classes' | 'drafts' | 'interventions' | 'lexicalLists' | 'assessments';

export function buildWorkspaceLink(
  pathname: string,
  params: Record<string, string | number | null | undefined>
): string {
  const searchParams = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value === null || value === undefined || value === '') {
      return;
    }
    searchParams.set(key, String(value));
  });
  const search = searchParams.toString();
  return search ? `${pathname}?${search}` : pathname;
}

function getSummary(overview?: TeacherWorkspaceOverviewVO | null): TeacherWorkspaceSummaryVO | null {
  return overview?.summary || null;
}

function buildTeacherWorkspaceSteadyStateItem(
  t: TFunction,
  overview?: TeacherWorkspaceOverviewVO | null
): WorkspaceFocusItem {
  const firstClass = overview?.recentClasses[0];
  const firstDraft = overview?.draftTemplates[0];
  const steadyStatePath = firstDraft
    ? `/teacher/diagnosis-template-drafts/${firstDraft.draftId}`
    : firstClass
      ? `/teacher/classes/${firstClass.classId}`
      : '/teacher/classes';

  return {
    id: 'steady-state',
    title: t(
      firstDraft
        ? 'teacherWorkspace.generated.onboarding.steadyState.draftTitle'
        : 'teacherWorkspace.generated.onboarding.steadyState.classTitle'
    ),
    description: t(
      firstDraft
        ? 'teacherWorkspace.generated.onboarding.steadyState.draftDescription'
        : 'teacherWorkspace.generated.onboarding.steadyState.classDescription'
    ),
    actionLabel: t(
      firstDraft
        ? 'teacherWorkspace.generated.onboarding.steadyState.draftActionLabel'
        : 'teacherWorkspace.generated.onboarding.steadyState.classActionLabel'
    ),
    to: buildWorkspaceLink(steadyStatePath, {
      step: firstDraft ? 4 : undefined,
      source: 'workspace',
    }),
    tone: 'stable',
  };
}

export function buildTeacherWorkspaceTodoItems(
  t: TFunction,
  overview?: TeacherWorkspaceOverviewVO | null
): WorkspaceFocusItem[] {
  const summary = getSummary(overview);
  if (!summary) {
    return [];
  }

  if (summary.classCount <= 0) {
    return [
      {
        id: 'classes-empty',
        title: t('teacherWorkspace.generated.focus.classesEmpty.title'),
        description: t('teacherWorkspace.generated.focus.classesEmpty.description'),
        to: buildWorkspaceLink('/teacher/classes', { source: 'workspace-onboarding' }),
        actionLabel: t('teacherWorkspace.generated.focus.classesEmpty.actionLabel'),
        tone: 'attention',
      },
    ];
  }

  const items: WorkspaceFocusItem[] = [];
  const firstDraft = overview?.draftTemplates[0];
  const firstIntervention = overview?.pendingInterventions[0];
  const firstLexicalList = overview?.recentLexicalLists[0];
  const firstAssessmentPublish = overview?.recentAssessmentPublishes[0];

  if (summary.pendingInterventionCount > 0) {
    items.push({
      id: 'interventions',
      title: t('teacherWorkspace.generated.focus.interventions.title'),
      description: t('teacherWorkspace.generated.focus.interventions.description', {
        count: summary.pendingInterventionCount,
      }),
      to: buildWorkspaceLink('/teacher/interventions', {
        view: 'pending',
        focusId: firstIntervention?.id,
        classId: firstIntervention?.classId,
        studentUserId: firstIntervention?.studentUserId,
        source: 'workspace',
      }),
      actionLabel: t('teacherWorkspace.generated.focus.interventions.actionLabel'),
      tone: 'attention',
    });
  }

  if (summary.pendingAssessmentSubmissionCount > 0) {
    items.push({
      id: 'assessments-pending',
      title: t('teacherWorkspace.generated.focus.assessmentsPending.title'),
      description: t('teacherWorkspace.generated.focus.assessmentsPending.description', {
        count: summary.pendingAssessmentSubmissionCount,
      }),
      to: buildWorkspaceLink(
        firstAssessmentPublish
          ? `/teacher/assessments/publishes/${firstAssessmentPublish.publishId}`
          : '/teacher/assessments',
        { source: 'workspace' }
      ),
      actionLabel: t('teacherWorkspace.generated.focus.assessmentsPending.actionLabel'),
      tone: 'attention',
    });
  }

  if (summary.draftTemplateCount <= 0) {
    items.push({
      id: 'drafts-empty',
      title: t('teacherWorkspace.generated.focus.draftsEmpty.title'),
      description: t('teacherWorkspace.generated.focus.draftsEmpty.description'),
      to: buildWorkspaceLink('/teacher/diagnosis-templates', {
        intent: 'create-draft',
        source: 'workspace-onboarding',
      }),
      actionLabel: t('teacherWorkspace.generated.focus.draftsEmpty.actionLabel'),
      tone: 'action',
    });
  } else {
    items.push({
      id: 'drafts-existing',
      title: t('teacherWorkspace.generated.focus.draftsExisting.title'),
      description: t('teacherWorkspace.generated.focus.draftsExisting.description', {
        count: summary.draftTemplateCount,
      }),
      to: buildWorkspaceLink(
        firstDraft ? `/teacher/diagnosis-template-drafts/${firstDraft.draftId}` : '/teacher/diagnosis-templates',
        {
          step: firstDraft ? 4 : undefined,
          source: 'workspace',
        }
      ),
      actionLabel: t('teacherWorkspace.generated.focus.draftsExisting.actionLabel'),
      tone: 'action',
    });
  }

  if (summary.pendingImportBatchCount > 0) {
    items.push({
      id: 'imports',
      title: t('teacherWorkspace.generated.focus.imports.title'),
      description: t('teacherWorkspace.generated.focus.imports.description', {
        count: summary.pendingImportBatchCount,
      }),
      to: buildWorkspaceLink('/teacher/lexical-pairs/imports', {
        view: 'pending',
        source: 'workspace',
      }),
      actionLabel: t('teacherWorkspace.generated.focus.imports.actionLabel'),
      tone: 'attention',
    });
  }

  if (summary.draftTemplateCount > 0) {
    if (summary.lexicalListCount <= 0) {
      items.push({
        id: 'lists-empty',
        title: t('teacherWorkspace.generated.focus.listsEmpty.title'),
        description: t('teacherWorkspace.generated.focus.listsEmpty.description'),
        to: buildWorkspaceLink('/teacher/lexical-lists', {
          intent: 'create-list',
          source: 'workspace-onboarding',
        }),
        actionLabel: t('teacherWorkspace.generated.focus.listsEmpty.actionLabel'),
        tone: 'action',
      });
    } else if (items.length < 3) {
      items.push({
        id: 'lists-existing',
        title: t('teacherWorkspace.generated.focus.listsExisting.title'),
        description: t('teacherWorkspace.generated.focus.listsExisting.description', {
          count: summary.lexicalListCount,
        }),
        to: buildWorkspaceLink('/teacher/lexical-lists', {
          listId: firstLexicalList?.id,
          source: 'workspace',
        }),
        actionLabel: t('teacherWorkspace.generated.focus.listsExisting.actionLabel'),
        tone: 'stable',
      });
    }
  }

  if (summary.assessmentPaperCount <= 0) {
    items.push({
      id: 'assessments-empty',
      title: t('teacherWorkspace.generated.focus.assessmentsEmpty.title'),
      description: t('teacherWorkspace.generated.focus.assessmentsEmpty.description'),
      to: buildWorkspaceLink('/teacher/assessments/new', { source: 'workspace-onboarding' }),
      actionLabel: t('teacherWorkspace.generated.focus.assessmentsEmpty.actionLabel'),
      tone: 'action',
    });
  }

  if (!items.length) {
    items.push(buildTeacherWorkspaceSteadyStateItem(t, overview));
  }

  return items.slice(0, 6);
}

export function buildTeacherWorkspaceOnboardingCard(
  t: TFunction,
  overview?: TeacherWorkspaceOverviewVO | null
): WorkspaceFocusItem | null {
  const summary = getSummary(overview);
  if (!summary) {
    return null;
  }

  if (summary.classCount <= 0) {
    return {
      id: 'setup-classes',
      title: t('teacherWorkspace.generated.onboarding.setupClasses.title'),
      description: t('teacherWorkspace.generated.onboarding.setupClasses.description'),
      actionLabel: t('teacherWorkspace.generated.onboarding.setupClasses.actionLabel'),
      to: buildWorkspaceLink('/teacher/classes', { source: 'workspace-onboarding' }),
      tone: 'attention',
    };
  }

  if (summary.draftTemplateCount <= 0) {
    return {
      id: 'setup-draft',
      title: t('teacherWorkspace.generated.onboarding.setupDraft.title'),
      description: t('teacherWorkspace.generated.onboarding.setupDraft.description'),
      actionLabel: t('teacherWorkspace.generated.onboarding.setupDraft.actionLabel'),
      to: buildWorkspaceLink('/teacher/diagnosis-templates', {
        intent: 'create-draft',
        source: 'workspace-onboarding',
      }),
      tone: 'action',
    };
  }

  if (summary.lexicalListCount <= 0) {
    return {
      id: 'setup-list',
      title: t('teacherWorkspace.generated.onboarding.setupList.title'),
      description: t('teacherWorkspace.generated.onboarding.setupList.description'),
      actionLabel: t('teacherWorkspace.generated.onboarding.setupList.actionLabel'),
      to: buildWorkspaceLink('/teacher/lexical-lists', {
        intent: 'create-list',
        source: 'workspace-onboarding',
      }),
      tone: 'action',
    };
  }

  if (summary.assessmentPaperCount <= 0) {
    return {
      id: 'setup-assessment',
      title: t('teacherWorkspace.generated.onboarding.setupAssessment.title'),
      description: t('teacherWorkspace.generated.onboarding.setupAssessment.description'),
      actionLabel: t('teacherWorkspace.generated.onboarding.setupAssessment.actionLabel'),
      to: buildWorkspaceLink('/teacher/assessments/new', { source: 'workspace-onboarding' }),
      tone: 'action',
    };
  }

  return null;
}

export function buildTeacherWorkspaceFocusItems(
  t: TFunction,
  overview?: TeacherWorkspaceOverviewVO | null
): WorkspaceFocusItem[] {
  return buildTeacherWorkspaceTodoItems(t, overview);
}

export function buildTeacherWorkspaceOnboardingItems(
  t: TFunction,
  overview?: TeacherWorkspaceOverviewVO | null
): WorkspaceFocusItem[] {
  const card = buildTeacherWorkspaceOnboardingCard(t, overview);
  return card ? [card] : [];
}

export function buildTeacherWorkspaceEmptyState(
  t: TFunction,
  section: WorkspaceSection,
  overview?: TeacherWorkspaceOverviewVO | null
): WorkspaceEmptyStateConfig {
  const summary = getSummary(overview);
  const firstClass = overview?.recentClasses[0];

  if (!summary) {
    return {
      title: t('teacherWorkspace.generated.emptyState.loading.title'),
      description: t('teacherWorkspace.generated.emptyState.loading.description'),
      actionLabel: t('teacherWorkspace.generated.emptyState.loading.actionLabel'),
      to: buildWorkspaceLink('/teacher/classes', { source: 'workspace' }),
    };
  }

  if (section === 'classes') {
    if (summary.classCount <= 0) {
      return {
        title: t('teacherWorkspace.generated.emptyState.classes.blockedTitle'),
        description: t('teacherWorkspace.generated.emptyState.classes.blockedDescription'),
        actionLabel: t('teacherWorkspace.generated.emptyState.classes.blockedActionLabel'),
        to: buildWorkspaceLink('/teacher/classes', { source: 'workspace-onboarding' }),
      };
    }
    return {
      title: t('teacherWorkspace.generated.emptyState.classes.readyTitle'),
      description: t('teacherWorkspace.generated.emptyState.classes.readyDescription'),
      actionLabel: t('teacherWorkspace.generated.emptyState.classes.readyActionLabel'),
      to: buildWorkspaceLink('/teacher/classes', { source: 'workspace' }),
    };
  }

  if (section === 'drafts') {
    if (summary.classCount <= 0) {
      return {
        title: t('teacherWorkspace.generated.emptyState.drafts.blockedTitle'),
        description: t('teacherWorkspace.generated.emptyState.drafts.blockedDescription'),
        actionLabel: t('teacherWorkspace.generated.emptyState.drafts.blockedActionLabel'),
        to: buildWorkspaceLink('/teacher/classes', { source: 'workspace-onboarding' }),
      };
    }
    return {
      title: t('teacherWorkspace.generated.emptyState.drafts.readyTitle'),
      description: t('teacherWorkspace.generated.emptyState.drafts.readyDescription'),
      actionLabel: t('teacherWorkspace.generated.emptyState.drafts.readyActionLabel'),
      to: buildWorkspaceLink('/teacher/diagnosis-templates', {
        intent: 'create-draft',
        source: 'workspace-onboarding',
      }),
    };
  }

  if (section === 'interventions') {
    if (summary.classCount <= 0) {
      return {
        title: t('teacherWorkspace.generated.emptyState.interventions.blockedTitle'),
        description: t('teacherWorkspace.generated.emptyState.interventions.blockedDescription'),
        actionLabel: t('teacherWorkspace.generated.emptyState.interventions.blockedActionLabel'),
        to: buildWorkspaceLink('/teacher/classes', { source: 'workspace-onboarding' }),
      };
    }
    return {
      title: t('teacherWorkspace.generated.emptyState.interventions.readyTitle'),
      description: t('teacherWorkspace.generated.emptyState.interventions.readyDescription'),
      actionLabel: t(
        firstClass
          ? 'teacherWorkspace.generated.emptyState.interventions.recentClassActionLabel'
          : 'teacherWorkspace.generated.emptyState.interventions.readyActionLabel'
      ),
      to: buildWorkspaceLink(
        firstClass ? `/teacher/classes/${firstClass.classId}` : '/teacher/interventions',
        { source: 'workspace' }
      ),
    };
  }

  if (section === 'assessments') {
    if (summary.classCount <= 0) {
      return {
        title: t('teacherWorkspace.generated.emptyState.assessments.blockedTitle'),
        description: t('teacherWorkspace.generated.emptyState.assessments.blockedDescription'),
        actionLabel: t('teacherWorkspace.generated.emptyState.assessments.blockedActionLabel'),
        to: buildWorkspaceLink('/teacher/classes', { source: 'workspace-onboarding' }),
      };
    }
    if (summary.assessmentPaperCount <= 0) {
      return {
        title: t('teacherWorkspace.generated.emptyState.assessments.readyTitle'),
        description: t('teacherWorkspace.generated.emptyState.assessments.readyDescription'),
        actionLabel: t('teacherWorkspace.generated.emptyState.assessments.readyActionLabel'),
        to: buildWorkspaceLink('/teacher/assessments/new', { source: 'workspace-onboarding' }),
      };
    }
    return {
      title: t('teacherWorkspace.generated.emptyState.assessments.publishedTitle'),
      description: t('teacherWorkspace.generated.emptyState.assessments.publishedDescription'),
      actionLabel: t('teacherWorkspace.generated.emptyState.assessments.publishedActionLabel'),
      to: buildWorkspaceLink('/teacher/assessments', { source: 'workspace' }),
    };
  }

  if (summary.draftTemplateCount <= 0) {
    return {
      title: t('teacherWorkspace.generated.emptyState.lexicalLists.blockedTitle'),
      description: t('teacherWorkspace.generated.emptyState.lexicalLists.blockedDescription'),
      actionLabel: t('teacherWorkspace.generated.emptyState.lexicalLists.blockedActionLabel'),
      to: buildWorkspaceLink('/teacher/diagnosis-templates', {
        intent: 'create-draft',
        source: 'workspace-onboarding',
      }),
    };
  }

  return {
    title: t('teacherWorkspace.generated.emptyState.lexicalLists.readyTitle'),
    description: t('teacherWorkspace.generated.emptyState.lexicalLists.readyDescription'),
    actionLabel: t('teacherWorkspace.generated.emptyState.lexicalLists.readyActionLabel'),
    to: buildWorkspaceLink('/teacher/lexical-lists', {
      intent: 'create-list',
      source: 'workspace-onboarding',
    }),
  };
}
