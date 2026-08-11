import type { PublicAssessmentQuestionVO, SpellingAttemptVO } from '@/lib/contracts';

export interface SpellingHintState {
  hintShown: boolean;
  hintFirstLetter: string | null;
  wrongAttemptCount: number;
}

export function emptySpellingHintState(): SpellingHintState {
  return { hintShown: false, hintFirstLetter: null, wrongAttemptCount: 0 };
}

export function hintStateOf(question: PublicAssessmentQuestionVO): SpellingHintState {
  return {
    hintShown: Boolean(question.spellingHintShown),
    hintFirstLetter: question.spellingHintFirstLetter ?? null,
    wrongAttemptCount: question.spellingWrongAttemptCount ?? 0,
  };
}

export function applySpellingOutcome(previous: SpellingHintState, outcome: SpellingAttemptVO): SpellingHintState {
  return {
    hintShown: outcome.hintShown,
    hintFirstLetter: outcome.hintFirstLetter ?? previous.hintFirstLetter,
    wrongAttemptCount: outcome.wrongAttemptCount,
  };
}

export function spellingTimingEventType(hintShown: boolean): 'SPELLING_PRE_HINT_DELTA' | 'SPELLING_POST_HINT_DELTA' {
  return hintShown ? 'SPELLING_POST_HINT_DELTA' : 'SPELLING_PRE_HINT_DELTA';
}
