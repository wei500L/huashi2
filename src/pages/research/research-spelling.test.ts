import { describe, expect, it } from 'vitest';
import type { PublicAssessmentQuestionVO, SpellingAttemptVO } from '@/lib/contracts';
import {
  applySpellingOutcome,
  emptySpellingHintState,
  hintStateOf,
  spellingTimingEventType,
} from './research-spelling';

describe('research spelling hint state', () => {
  it('starts empty when the server exposes no spelling metadata', () => {
    const question = { questionOrder: 1, questionType: 'SPELLING' } as PublicAssessmentQuestionVO;
    expect(hintStateOf(question)).toEqual({ hintShown: false, hintFirstLetter: null, wrongAttemptCount: 0 });
  });

  it('restores server-side hint state after refresh', () => {
    const question = {
      questionOrder: 1,
      questionType: 'SPELLING',
      spellingHintShown: true,
      spellingHintFirstLetter: 'p',
      spellingWrongAttemptCount: 2,
    } as PublicAssessmentQuestionVO;
    expect(hintStateOf(question)).toEqual({ hintShown: true, hintFirstLetter: 'p', wrongAttemptCount: 2 });
  });

  it('reveals the first letter only after the first wrong attempt', () => {
    const initial = emptySpellingHintState();
    const firstWrong: SpellingAttemptVO = { correct: false, hintShown: true, hintFirstLetter: 'p', wrongAttemptCount: 1 };
    const afterFirst = applySpellingOutcome(initial, firstWrong);
    expect(afterFirst).toEqual({ hintShown: true, hintFirstLetter: 'p', wrongAttemptCount: 1 });

    const secondWrong: SpellingAttemptVO = { correct: false, hintShown: true, hintFirstLetter: 'p', wrongAttemptCount: 2 };
    expect(applySpellingOutcome(afterFirst, secondWrong)).toEqual({
      hintShown: true,
      hintFirstLetter: 'p',
      wrongAttemptCount: 2,
    });
  });

  it('keeps the previous hint letter when the server omits it after a correct answer', () => {
    const afterFirst = { hintShown: true, hintFirstLetter: 'p', wrongAttemptCount: 1 };
    const correct: SpellingAttemptVO = { correct: true, hintShown: true, hintFirstLetter: null, wrongAttemptCount: 1 };
    expect(applySpellingOutcome(afterFirst, correct)).toEqual({ hintShown: true, hintFirstLetter: 'p', wrongAttemptCount: 1 });
  });

  it('selects the correct timing segment before and after the hint', () => {
    expect(spellingTimingEventType(false)).toBe('SPELLING_PRE_HINT_DELTA');
    expect(spellingTimingEventType(true)).toBe('SPELLING_POST_HINT_DELTA');
  });
});
