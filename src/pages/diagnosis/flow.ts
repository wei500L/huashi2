export type DiagnosisPhase = 'boot' | 'select' | 'running' | 'result';

export type DiagnosisFlowState = {
  phase: DiagnosisPhase;
  sessionId: number | null;
};

export type DiagnosisFlowAction =
  | { type: 'resumeSession'; sessionId: number }
  | { type: 'readyToSelect' }
  | { type: 'startSession'; sessionId: number }
  | { type: 'showResult' }
  | { type: 'reset' };

export const initialDiagnosisFlowState: DiagnosisFlowState = {
  phase: 'boot',
  sessionId: null,
};

export function diagnosisFlowReducer(
  state: DiagnosisFlowState,
  action: DiagnosisFlowAction
): DiagnosisFlowState {
  switch (action.type) {
    case 'resumeSession':
      return {
        phase: 'running',
        sessionId: action.sessionId,
      };
    case 'readyToSelect':
      return {
        phase: 'select',
        sessionId: null,
      };
    case 'startSession':
      return {
        phase: 'running',
        sessionId: action.sessionId,
      };
    case 'showResult':
      if (state.sessionId === null) {
        return state;
      }
      return {
        phase: 'result',
        sessionId: state.sessionId,
      };
    case 'reset':
      return {
        phase: 'select',
        sessionId: null,
      };
    default:
      return state;
  }
}
