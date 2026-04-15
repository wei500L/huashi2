import type { StudentProfileVO } from './contracts';

export const studentLanguageLevelOptions = ['A1', 'A2', 'B1', 'B2', 'C1', 'C2'] as const;
export const studentCourseStageOptions = ['FOUNDATION', 'INTERMEDIATE', 'ADVANCED'] as const;

export function isStudentProfileIncomplete(
  profile?: Pick<StudentProfileVO, 'gradeName' | 'englishLevel' | 'frenchLevel' | 'courseStage'> | null,
): boolean {
  if (!profile) {
    return true;
  }

  return [profile.gradeName, profile.englishLevel, profile.frenchLevel, profile.courseStage]
    .some((value) => !value?.trim());
}
