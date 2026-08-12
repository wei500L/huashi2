import type { TeacherResearchAttemptDetailVO } from '@/lib/contracts';
import { parseBackendDateTime } from '@/lib/format';

export type ResearchAttemptQuestion = TeacherResearchAttemptDetailVO['questions'][number];

export function isProfileQuestion(question: ResearchAttemptQuestion): boolean {
  if (question.formalSection === false) return true;
  if (question.formalSection === true) return false;
  const section = (question.sectionTitle || question.questionCode || '').toUpperCase();
  if (section.startsWith('BASIC')) return true;
  return question.questionType === 'INSTRUCTION';
}

export function isInstructionQuestion(question: ResearchAttemptQuestion): boolean {
  return question.questionType === 'INSTRUCTION';
}

export function isQuestionUnanswered(question: ResearchAttemptQuestion): boolean {
  if (isInstructionQuestion(question)) return false;
  const hasText = question.responses.some((value) => value.trim().length > 0);
  const hasJustification = Boolean(question.justification?.trim());
  return !hasText && !hasJustification && question.attachments.length === 0;
}

export function hasScanIssue(question: ResearchAttemptQuestion): boolean {
  return question.attachments.some((file) => {
    const status = (file.scanStatus || '').toUpperCase();
    return status === 'INFECTED' || status === 'FAILED';
  });
}

export function isQuestionAnomalous(question: ResearchAttemptQuestion): boolean {
  return isQuestionUnanswered(question) || hasScanIssue(question);
}

export function sectionHeading(question: ResearchAttemptQuestion): string {
  if (isProfileQuestion(question)) return '资料收集';
  const title = question.sectionTitle?.trim();
  if (title && !/^[A-Z0-9_]+$/.test(title)) return title;
  return '正式题';
}

export function attemptDurationMs(detail: TeacherResearchAttemptDetailVO): number | null {
  if (detail.attempt.startedAt && detail.attempt.submittedAt) {
    const started = parseBackendDateTime(detail.attempt.startedAt).getTime();
    const submitted = parseBackendDateTime(detail.attempt.submittedAt).getTime();
    if (!Number.isNaN(started) && !Number.isNaN(submitted) && submitted >= started) {
      return submitted - started;
    }
  }
  const fromQuestions = detail.questions.reduce((sum, question) => sum + (question.effectiveDurationMs || 0), 0);
  return fromQuestions > 0 ? fromQuestions : null;
}
