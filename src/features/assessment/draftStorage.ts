export type AssessmentDraftPayload = {
  attemptId: number;
  baseVersion: number;
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
    if (parsed.attemptId !== attemptId || !parsed.responsesByOrder || !parsed.updatedAt || !Number.isInteger(parsed.baseVersion)) {
      return null;
    }
    return parsed;
  } catch {
    window.localStorage.removeItem(buildStorageKey(attemptId));
    return null;
  }
}

export function writeAssessmentDraft(
  attemptId: number,
  baseVersion: number,
  responsesByOrder: Record<number, string[]>
) {
  if (typeof window === 'undefined') {
    return;
  }
  const payload: AssessmentDraftPayload = {
    attemptId,
    baseVersion,
    updatedAt: new Date().toISOString(),
    responsesByOrder,
  };
  window.localStorage.setItem(buildStorageKey(attemptId), JSON.stringify(payload));
}

export function markAssessmentDraftSaved(
  attemptId: number,
  savedResponsesByOrder: Record<number, string[]>,
  nextVersion: number
) {
  const currentDraft = readAssessmentDraft(attemptId);
  if (!currentDraft) {
    return;
  }
  if (JSON.stringify(currentDraft.responsesByOrder) === JSON.stringify(savedResponsesByOrder)) {
    clearAssessmentDraft(attemptId);
    return;
  }
  writeAssessmentDraft(attemptId, nextVersion, currentDraft.responsesByOrder);
}

export function clearAssessmentDraft(attemptId: number) {
  if (typeof window === 'undefined') {
    return;
  }
  window.localStorage.removeItem(buildStorageKey(attemptId));
}
