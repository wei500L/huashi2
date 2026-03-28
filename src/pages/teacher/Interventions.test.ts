import { describe, expect, it } from 'vitest';
import { matchInterventionView, normalizeInterventionView } from './Interventions';

const baseRecord = {
  id: 1,
  studentUserId: 2,
  studentName: 'Student',
  classId: 3,
  className: 'Class',
  priority: 'NORMAL',
  status: 'PENDING',
  plannedAt: null,
  completedAt: null,
  teacherNote: '',
  createdAt: '2026-03-29T09:00:00',
  updatedAt: '2026-03-29T09:00:00',
  patternDetected: 'pattern',
  suggestedAction: 'action',
};

describe('Interventions helpers', () => {
  it('normalizes unsupported view values to all', () => {
    expect(normalizeInterventionView('unknown')).toBe('all');
    expect(normalizeInterventionView('pending')).toBe('pending');
  });

  it('matches overdue records by time and completion state', () => {
    expect(
      matchInterventionView(
        {
          ...baseRecord,
          plannedAt: '2026-03-20T09:00:00',
        },
        'overdue'
      )
    ).toBe(true);

    expect(
      matchInterventionView(
        {
          ...baseRecord,
          status: 'COMPLETED',
          plannedAt: '2026-03-20T09:00:00',
        },
        'overdue'
      )
    ).toBe(false);
  });
});
