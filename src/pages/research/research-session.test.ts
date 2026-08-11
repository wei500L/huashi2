import { afterEach, describe, expect, it, vi } from 'vitest';
import type { PublicAssessmentQuestionVO } from '@/lib/contracts';
import {
  forgetPublicSession,
  getAnswerProgress,
  getHiddenQuestionOrders,
  getStagedAnswerProgress,
  hasPublicSessionMarker,
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
