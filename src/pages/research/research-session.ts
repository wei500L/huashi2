import type { PublicAssessmentQuestionVO } from '@/lib/contracts';

const PUBLIC_SESSION_MARKER_PREFIX = 'ef-transfer-public-assessment-session:';
const PUBLIC_SESSION_MARKER_TTL_MS = 12 * 60 * 60 * 1000;

function publicSessionMarkerKey(releaseCode: string): string {
  return `${PUBLIC_SESSION_MARKER_PREFIX}${releaseCode.toUpperCase()}`;
}

export function hasPublicSessionMarker(releaseCode: string, now = Date.now()): boolean {
  try {
    const raw = window.localStorage.getItem(publicSessionMarkerKey(releaseCode));
    const verifiedAt = raw ? Number(raw) : Number.NaN;
    if (!Number.isFinite(verifiedAt) || now - verifiedAt >= PUBLIC_SESSION_MARKER_TTL_MS) {
      window.localStorage.removeItem(publicSessionMarkerKey(releaseCode));
      return false;
    }
    return true;
  } catch {
    return false;
  }
}

export function rememberPublicSession(releaseCode: string, now = Date.now()): void {
  try {
    window.localStorage.setItem(publicSessionMarkerKey(releaseCode), String(now));
  } catch {
    // The HttpOnly cookie remains authoritative when browser storage is unavailable.
  }
}

export function forgetPublicSession(releaseCode: string): void {
  try {
    window.localStorage.removeItem(publicSessionMarkerKey(releaseCode));
  } catch {
    // Nothing else is required; an expired cookie will still be rejected by the API.
  }
}

export function getAnswerProgress(
  questions: PublicAssessmentQuestionVO[],
  responsesByOrder: Record<number, string[]>
): { answeredCount: number; questionCount: number } {
  const answerable = questions.filter((question) => question.questionType !== 'INSTRUCTION');
  const answeredCount = answerable.filter((question) =>
    Boolean(responsesByOrder[question.questionOrder]?.some((value) => value.trim().length > 0))
  ).length;
  return { answeredCount, questionCount: answerable.length };
}
