import type { PublicAssessmentQuestionVO } from '@/lib/contracts';

const PUBLIC_SESSION_MARKER_PREFIX = 'ef-transfer-public-assessment-session:';
const PUBLIC_SESSION_MARKER_TTL_MS = 12 * 60 * 60 * 1000;

type DisplayCondition = { fieldCode: string; operator: string; value: string };

function parseDisplayCondition(question: PublicAssessmentQuestionVO): DisplayCondition | null {
  if (!question.displayCondition) return null;
  try {
    const parsed = JSON.parse(question.displayCondition) as Partial<DisplayCondition>;
    if (parsed && typeof parsed === 'object' && typeof parsed.fieldCode === 'string') {
      return parsed as DisplayCondition;
    }
  } catch {
    // Malformed seed data remains visible instead of blocking the questionnaire.
  }
  return null;
}

export function isResearchQuestionVisible(
  question: PublicAssessmentQuestionVO,
  responsesByOrder: Record<number, string[]>,
  questionOrderByItemCode: Map<string | null | undefined, number>
): boolean {
  const condition = parseDisplayCondition(question);
  if (!condition || condition.operator !== 'EQ') return true;
  const fieldOrder = questionOrderByItemCode.get(condition.fieldCode);
  if (fieldOrder == null) return true;
  return (responsesByOrder[fieldOrder] || [])[0] === condition.value;
}

export function getHiddenQuestionOrders(
  questions: PublicAssessmentQuestionVO[],
  responsesByOrder: Record<number, string[]>
): number[] {
  const questionOrderByItemCode = new Map<string | null | undefined, number>();
  questions.forEach((question) => questionOrderByItemCode.set(question.itemCode, question.questionOrder));
  return questions
    .filter((question) => !isResearchQuestionVisible(question, responsesByOrder, questionOrderByItemCode))
    .map((question) => question.questionOrder);
}

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

export function getStagedAnswerProgress(
  questions: PublicAssessmentQuestionVO[],
  responsesByOrder: Record<number, string[]>
): {
  profileAnsweredCount: number;
  profileFieldCount: number;
  formalAnsweredCount: number;
  formalQuestionCount: number;
} {
  const isAnswered = (question: PublicAssessmentQuestionVO) =>
    Boolean(responsesByOrder[question.questionOrder]?.some((value) => value.trim().length > 0));
  const profileQuestions = questions.filter((question) =>
    question.formalSection === false && question.questionType !== 'INSTRUCTION'
  );
  const formalQuestions = questions.filter((question) => question.formalSection === true);
  return {
    profileAnsweredCount: profileQuestions.filter(isAnswered).length,
    profileFieldCount: profileQuestions.length,
    formalAnsweredCount: formalQuestions.filter(isAnswered).length,
    formalQuestionCount: formalQuestions.length,
  };
}
