import { describe, expect, it } from 'vitest';
import { diagnosisFlowReducer, initialDiagnosisFlowState } from './flow';

describe('diagnosisFlowReducer', () => {
  it('resumes an in-progress session into running state', () => {
    expect(
      diagnosisFlowReducer(initialDiagnosisFlowState, {
        type: 'resumeSession',
        sessionId: 42,
      })
    ).toEqual({
      phase: 'running',
      sessionId: 42,
    });
  });

  it('returns to select state on reset', () => {
    expect(
      diagnosisFlowReducer(
        {
          phase: 'result',
          sessionId: 17,
        },
        { type: 'reset' }
      )
    ).toEqual({
      phase: 'select',
      sessionId: null,
    });
  });
});
