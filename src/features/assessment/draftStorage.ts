export type AssessmentDraftPayload = {
  attemptId: number;
  updatedAt: string;
  responsesByOrder: Record<number, string[]>;
};

const STORAGE_PREFIX = 'ef-transfer-assessment-draft:';

function buildStorageKey(attemptId: number) {
  return `${STORAGE_PREFIX}${attemptId}`;
}

export function readAssessmentDraft(attemptId: number): AssessmentDraftPayload | null {
  if (typeof window === 'undefined') {
    return null;
  }
  const raw = window.localStorage.getItem(buildStorageKey(attemptId));
  if (!raw) {
    return null;
  }
  try {
    const parsed = JSON.parse(raw) as AssessmentDraftPayload;
    if (parsed.attemptId !== attemptId || !parsed.responsesByOrder || !parsed.updatedAt) {
      return null;
    }
    return parsed;
  } catch {
    window.localStorage.removeItem(buildStorageKey(attemptId));
    return null;
  }
}

export function writeAssessmentDraft(attemptId: number, responsesByOrder: Record<number, string[]>) {
  if (typeof window === 'undefined') {
    return;
  }
  const payload: AssessmentDraftPayload = {
    attemptId,
    updatedAt: new Date().toISOString(),
    responsesByOrder,
  };
  window.localStorage.setItem(buildStorageKey(attemptId), JSON.stringify(payload));
}

export function clearAssessmentDraft(attemptId: number) {
  if (typeof window === 'undefined') {
    return;
  }
  window.localStorage.removeItem(buildStorageKey(attemptId));
}
