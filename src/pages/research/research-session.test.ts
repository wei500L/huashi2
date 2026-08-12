import { afterEach, describe, expect, it, vi } from 'vitest';
import type { PublicAssessmentQuestionVO } from '@/lib/contracts';
import {
  forgetPublicSession,
  getAnswerProgress,
  getHiddenQuestionOrders,
  getStagedAnswerProgress,
  hasPublicSessionMarker,
  isResearchQuestionVisible,
  rememberPublicSession,
} from './research-session';

afterEach(() => {
  vi.restoreAllMocks();
  window.localStorage.clear();
});

describe('public research session markers', () => {
  it('stores, reads, and expires a marker without exposing the session token', () => {
    rememberPublicSession('res-test', 1_000);
    expect(hasPublicSessionMarker('RES-TEST', 2_000)).toBe(true);
    expect(hasPublicSessionMarker('RES-TEST', 1_000 + 12 * 60 * 60 * 1000)).toBe(false);
  });

  it('falls back safely when browser storage is unavailable', () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new DOMException('Storage disabled', 'SecurityError');
    });
    expect(hasPublicSessionMarker('RES-TEST')).toBe(false);
    expect(() => rememberPublicSession('RES-TEST')).not.toThrow();
    expect(() => forgetPublicSession('RES-TEST')).not.toThrow();
  });

  it('separates profile fields from formal questions', () => {
    const questions = [
      { questionOrder: 1, questionType: 'INSTRUCTION', formalSection: false },
      { questionOrder: 2, questionType: 'NUMBER', formalSection: false },
      { questionOrder: 3, questionType: 'SINGLE_CHOICE', formalSection: false },
      { questionOrder: 4, questionType: 'SINGLE_CHOICE', formalSection: true },
    ] as PublicAssessmentQuestionVO[];

    expect(getStagedAnswerProgress(questions, { 2: ['130'], 4: ['A'] })).toEqual({
      profileAnsweredCount: 1,
      profileFieldCount: 2,
      formalAnsweredCount: 1,
      formalQuestionCount: 1,
    });
  });

  it('hides English-major-only fields after switching to non-English major', () => {
    const questions = [
      { questionOrder: 1, itemCode: 'BASIC-ENGLISH-MAJOR' },
      {
        questionOrder: 2,
        itemCode: 'BASIC-TEM4',
        displayCondition: JSON.stringify({
          fieldCode: 'BASIC-ENGLISH-MAJOR',
          operator: 'EQ',
          value: 'ENGLISH_MAJOR',
        }),
      },
      {
        questionOrder: 3,
        itemCode: 'BASIC-TEM8',
        displayCondition: JSON.stringify({
          fieldCode: 'BASIC-ENGLISH-MAJOR',
          operator: 'EQ',
          value: 'ENGLISH_MAJOR',
        }),
      },
    ] as PublicAssessmentQuestionVO[];

    expect(getHiddenQuestionOrders(questions, { 1: ['ENGLISH_MAJOR'], 2: ['80'], 3: ['75'] })).toEqual([]);
    expect(getHiddenQuestionOrders(questions, { 1: ['NON_ENGLISH_MAJOR'], 2: ['80'], 3: ['75'] })).toEqual([2, 3]);
  });
});

describe('public research profile fields', () => {
  const englishMajorCondition = JSON.stringify({
    fieldCode: 'BASIC-ENGLISH-MAJOR',
    operator: 'EQ',
    value: 'ENGLISH_MAJOR',
  });

  const buildV1Questions = (): PublicAssessmentQuestionVO[] => [
    { questionOrder: 1, itemCode: 'BASIC-INSTRUCTION', questionType: 'INSTRUCTION', formalSection: false },
    { questionOrder: 2, itemCode: 'BASIC-NAME', questionType: 'SHORT_TEXT', formalSection: false },
    { questionOrder: 3, itemCode: 'BASIC-CONTACT', questionType: 'SHORT_TEXT', formalSection: false },
    { questionOrder: 4, itemCode: 'BASIC-GAOKAO-ENGLISH', questionType: 'NUMBER', formalSection: false },
    { questionOrder: 5, itemCode: 'BASIC-ENGLISH-MAJOR', questionType: 'SINGLE_CHOICE', formalSection: false },
    { questionOrder: 6, itemCode: 'BASIC-CET4', questionType: 'NUMBER', formalSection: false },
    { questionOrder: 7, itemCode: 'BASIC-CET6', questionType: 'NUMBER', formalSection: false },
    { questionOrder: 8, itemCode: 'BASIC-TEM4', questionType: 'NUMBER', formalSection: false, displayCondition: englishMajorCondition },
    { questionOrder: 9, itemCode: 'BASIC-TEM8', questionType: 'NUMBER', formalSection: false, displayCondition: englishMajorCondition },
    ...Array.from({ length: 60 }, (_, index) => ({
      questionOrder: 10 + index,
      itemCode: `P1A-${String(index + 1).padStart(2, '0')}`,
      questionType: 'SINGLE_CHOICE',
      formalSection: true,
    })),
  ] as PublicAssessmentQuestionVO[];

  const visibleQuestions = (questions: PublicAssessmentQuestionVO[], responsesByOrder: Record<number, string[]>) => {
    const questionOrderByItemCode = new Map(questions.map((q) => [q.itemCode, q.questionOrder]));
    return questions.filter((question) => isResearchQuestionVisible(question, responsesByOrder, questionOrderByItemCode));
  };

  it('counts 6 profile fields for non-English majors and keeps 60 formal questions', () => {
    const responses = { 5: ['NON_ENGLISH_MAJOR'] };
    const progress = getStagedAnswerProgress(visibleQuestions(buildV1Questions(), responses), responses);
    expect(progress.profileFieldCount).toBe(6);
    expect(progress.formalQuestionCount).toBe(60);
    expect(progress.profileAnsweredCount).toBe(1);
  });

  it('counts 8 profile fields for English majors', () => {
    const responses = { 5: ['ENGLISH_MAJOR'] };
    const progress = getStagedAnswerProgress(visibleQuestions(buildV1Questions(), responses), responses);
    expect(progress.profileFieldCount).toBe(8);
    expect(progress.formalQuestionCount).toBe(60);
  });

  it('lists name and contact before any score field and never counts profile fields as formal questions', () => {
    const questions = buildV1Questions();
    const visible = visibleQuestions(questions, { 5: ['ENGLISH_MAJOR'] });
    const profileFields = visible.filter((question) => question.formalSection === false && question.questionType !== 'INSTRUCTION');
    expect(profileFields.map((question) => question.itemCode)).toEqual([
      'BASIC-NAME',
      'BASIC-CONTACT',
      'BASIC-GAOKAO-ENGLISH',
      'BASIC-ENGLISH-MAJOR',
      'BASIC-CET4',
      'BASIC-CET6',
      'BASIC-TEM4',
      'BASIC-TEM8',
    ]);
    expect(visible.filter((question) => question.formalSection === true)).toHaveLength(60);
  });

  it('clears hidden TEM answers when switching from English major to non-English major', () => {
    const questions = buildV1Questions();
    const hiddenOrders = getHiddenQuestionOrders(questions, { 5: ['NON_ENGLISH_MAJOR'], 8: ['80'], 9: ['75'] });
    expect(hiddenOrders).toEqual([8, 9]);
  });
});

describe('public research progress', () => {
  it('does not count instruction blocks as unanswered questions', () => {
    const questions = [
      { questionOrder: 1, questionType: 'INSTRUCTION' },
      { questionOrder: 2, questionType: 'SHORT_TEXT' },
      { questionOrder: 3, questionType: 'SINGLE_CHOICE' },
    ] as PublicAssessmentQuestionVO[];

    expect(getAnswerProgress(questions, { 2: ['answer'], 3: ['A'] })).toEqual({
      answeredCount: 2,
      questionCount: 2,
    });
  });
});
