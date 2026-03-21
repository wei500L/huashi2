import { describe, expect, it } from 'vitest';
import { initialTrainingFlowState, trainingFlowReducer } from './flow';

describe('trainingFlowReducer', () => {
  it('resumes an in-progress session into running state', () => {
    expect(
      trainingFlowReducer(initialTrainingFlowState, {
        type: 'resumeSession',
        sessionId: 8,
      })
    ).toEqual({
      phase: 'running',
      sessionId: 8,
      summarySessionId: null,
    });
  });

  it('moves a completed session into summary state', () => {
    expect(
      trainingFlowReducer(
        {
          phase: 'running',
          sessionId: 8,
          summarySessionId: null,
        },
        { type: 'showSummary', sessionId: 8 }
      )
    ).toEqual({
      phase: 'summary',
      sessionId: null,
      summarySessionId: 8,
    });
  });
});
