import { afterEach, describe, expect, it, vi } from 'vitest';
import type { PublicAssessmentQuestionVO } from '@/lib/contracts';
import {
  forgetPublicSession,
  getAnswerProgress,
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
