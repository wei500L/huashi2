import { describe, expect, it } from 'vitest';
import { buildTeacherWorkspaceFocusItems } from './copy';

describe('teacher workspace copy helpers', () => {
  it('prioritizes intervention, draft, import, and lexical-list actions', () => {
    const items = buildTeacherWorkspaceFocusItems({
      classCount: 2,
      studentCount: 48,
      draftTemplateCount: 0,
      pendingInterventionCount: 3,
      lexicalPairCount: 120,
      lexicalListCount: 0,
      pendingImportBatchCount: 1,
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
      classCount: 1,
      studentCount: 20,
      draftTemplateCount: 2,
      pendingInterventionCount: 0,
      lexicalPairCount: 80,
      lexicalListCount: 3,
      pendingImportBatchCount: 0,
    });

    expect(items.some((item) => item.id === 'lists-existing')).toBe(true);
    expect(items.some((item) => item.id === 'classes-empty')).toBe(false);
  });
});
