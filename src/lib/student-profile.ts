import type { CurrentUserVO, StudentProfileVO } from './contracts';

export const studentLanguageLevelOptions = ['A1', 'A2', 'B1', 'B2', 'C1', 'C2'] as const;
export const studentCourseStageOptions = ['FOUNDATION', 'INTERMEDIATE', 'ADVANCED'] as const;

export function isStudentProfileIncomplete(
  profile?: Pick<StudentProfileVO, 'gradeName' | 'frenchLevel' | 'courseStage'> | null,
): boolean {
  if (!profile) {
    return true;
  }

  return [profile.gradeName, profile.frenchLevel, profile.courseStage]
    .some((value) => !value?.trim());
}

export function requiresStudentProfileCompletion(
  user?: Pick<CurrentUserVO, 'capabilities' | 'studentProfile'> | null,
): boolean {
  return Array.isArray(user?.capabilities)
    && user.capabilities.includes('STUDENT_WORKSPACE')
    && isStudentProfileIncomplete(user.studentProfile);
}
