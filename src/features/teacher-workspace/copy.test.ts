import { describe, expect, it } from 'vitest';
import {
  buildTeacherWorkspaceEmptyState,
  buildTeacherWorkspaceFocusItems,
  buildTeacherWorkspaceOnboardingItems,
} from './copy';

describe('teacher workspace copy helpers', () => {
  it('prioritizes intervention, draft, import, and lexical-list actions', () => {
    const items = buildTeacherWorkspaceFocusItems({
      teacherName: 'Teacher',
      organizationLabel: 'Org',
      summary: {
        classCount: 2,
        studentCount: 48,
        draftTemplateCount: 0,
        pendingInterventionCount: 3,
        lexicalPairCount: 120,
        lexicalListCount: 0,
        pendingImportBatchCount: 1,
      },
      recentClasses: [],
      draftTemplates: [],
      pendingInterventions: [{ id: 11, priority: 'URGENT', status: 'PENDING', plannedAt: null }],
      recentLexicalLists: [],
    });

    expect(items.map((item) => item.id)).toEqual([
      'interventions',
      'drafts-empty',
      'imports',
      'lists-empty',
    ]);
  });

  it('returns a stable maintenance cue when assets already exist', () => {
    const items = buildTeacherWorkspaceFocusItems({
      teacherName: 'Teacher',
      organizationLabel: 'Org',
      summary: {
        classCount: 1,
        studentCount: 20,
        draftTemplateCount: 2,
        pendingInterventionCount: 0,
        lexicalPairCount: 80,
        lexicalListCount: 3,
        pendingImportBatchCount: 0,
      },
      recentClasses: [],
      draftTemplates: [{ draftId: 5, templateName: 'Draft', syncState: 'DIRTY', updatedAt: null }],
      pendingInterventions: [],
      recentLexicalLists: [{ id: 9, listName: 'Starter', itemCount: 8, updatedAt: null }],
    });

    expect(items.some((item) => item.id === 'lists-existing')).toBe(true);
    expect(items.some((item) => item.id === 'classes-empty')).toBe(false);
  });

  it('returns a blocking onboarding step when class context is missing', () => {
    const items = buildTeacherWorkspaceOnboardingItems({
      teacherName: 'Teacher',
      organizationLabel: 'Org',
      summary: {
        classCount: 0,
        studentCount: 0,
        draftTemplateCount: 0,
        pendingInterventionCount: 0,
        lexicalPairCount: 0,
        lexicalListCount: 0,
        pendingImportBatchCount: 0,
      },
      recentClasses: [],
      draftTemplates: [],
      pendingInterventions: [],
      recentLexicalLists: [],
    });

    expect(items).toHaveLength(1);
    expect(items[0]?.id).toBe('setup-classes');
  });

  it('maps lexical-list empty state to template creation when drafts are missing', () => {
    const state = buildTeacherWorkspaceEmptyState('lexicalLists', {
      teacherName: 'Teacher',
      organizationLabel: 'Org',
      summary: {
        classCount: 1,
        studentCount: 12,
        draftTemplateCount: 0,
        pendingInterventionCount: 0,
        lexicalPairCount: 10,
        lexicalListCount: 0,
        pendingImportBatchCount: 0,
      },
      recentClasses: [],
      draftTemplates: [],
      pendingInterventions: [],
      recentLexicalLists: [],
    });

    expect(state.actionLabel).toBe('进入模板与草稿');
    expect(state.to).toContain('/teacher/diagnosis-templates');
  });
});
