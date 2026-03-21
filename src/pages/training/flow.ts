export type TrainingPhase = 'boot' | 'home' | 'running' | 'summary';

export type TrainingFlowState = {
  phase: TrainingPhase;
  sessionId: number | null;
  summarySessionId: number | null;
};

export type TrainingFlowAction =
  | { type: 'resumeSession'; sessionId: number }
  | { type: 'readyHome' }
  | { type: 'startSession'; sessionId: number }
  | { type: 'showSummary'; sessionId: number }
  | { type: 'resetHome' };

export const initialTrainingFlowState: TrainingFlowState = {
  phase: 'boot',
  sessionId: null,
  summarySessionId: null,
};

export function trainingFlowReducer(
  state: TrainingFlowState,
  action: TrainingFlowAction
): TrainingFlowState {
  switch (action.type) {
    case 'resumeSession':
      return {
        phase: 'running',
        sessionId: action.sessionId,
        summarySessionId: null,
      };
    case 'readyHome':
      return {
        phase: 'home',
        sessionId: null,
        summarySessionId: null,
      };
    case 'startSession':
      return {
        phase: 'running',
        sessionId: action.sessionId,
        summarySessionId: null,
      };
    case 'showSummary':
      return {
        phase: 'summary',
        sessionId: null,
        summarySessionId: action.sessionId,
      };
    case 'resetHome':
      return {
        phase: 'home',
        sessionId: null,
        summarySessionId: null,
      };
    default:
      return state;
  }
}
