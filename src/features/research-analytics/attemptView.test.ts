import { describe, expect, it } from 'vitest';
import type { TeacherResearchAttemptDetailVO } from '@/lib/contracts';
import {
  attemptDurationMs,
  isProfileQuestion,
  isQuestionAnomalous,
  isQuestionUnanswered,
  sectionHeading,
} from './attemptView';

const question = (overrides: Partial<TeacherResearchAttemptDetailVO['questions'][number]>): TeacherResearchAttemptDetailVO['questions'][number] => ({
  questionId: 1,
  questionOrder: 2,
  questionType: 'SHORT_TEXT',
  questionCode: 'BASIC-NAME',
  sectionTitle: 'BASIC_INFO',
  formalSection: false,
  stemText: '您的姓名：',
  promptText: null,
  options: [],
  responses: [],
  correctAnswers: [],
  justification: null,
  correct: null,
  scoreAwarded: null,
  questionScore: 0,
  explanationText: null,
  effectiveDurationMs: null,
  responseChangeCount: 0,
  attachments: [],
  ...overrides,
});

describe('research attempt view helpers', () => {
  it('treats BASIC and explicit formalSection=false as profile questions', () => {
    expect(isProfileQuestion(question({}))).toBe(true);
    expect(isProfileQuestion(question({ formalSection: true, sectionTitle: 'P1', questionType: 'SINGLE_CHOICE' }))).toBe(false);
    expect(sectionHeading(question({}))).toBe('资料收集');
  });

  it('treats unanswered and infected attachments as anomalies, not wrong answers', () => {
    expect(isQuestionUnanswered(question({ responses: [] }))).toBe(true);
    expect(isQuestionUnanswered(question({ responses: ['张三'] }))).toBe(false);
    expect(isQuestionAnomalous(question({ responses: [], correct: false }))).toBe(true);
    expect(isQuestionAnomalous(question({
      responses: ['A'],
      correct: false,
      formalSection: true,
      questionType: 'SINGLE_CHOICE',
    }))).toBe(false);
  });

  it('computes attempt duration from UTC timestamps', () => {
    const duration = attemptDurationMs({
      participant: { participantCode: 'P-000018', participantType: 'PUBLIC_CODE', consentedAt: null },
      attempt: {
        attemptId: 21,
        publishId: 1,
        paperId: 1,
        paperTitle: 'V1',
        status: 'SUBMITTED',
        answeredCount: 60,
        questionCount: 60,
        startedAt: '2026-08-12T05:17:30',
        lastSavedAt: '2026-08-12T05:40:08',
        submittedAt: '2026-08-12T05:40:08',
        submitReason: 'MANUAL',
      },
      result: { qualityFlags: [] },
      ai: {},
      questions: [],
    });
    expect(duration).toBe((22 * 60 + 38) * 1000);
  });
});
