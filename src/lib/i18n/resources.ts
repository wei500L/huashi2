import { analyticsSlice } from './slices/analytics';
import { commonSlice } from './slices/common';
import { diagnosisSlice } from './slices/diagnosis';
import { loginSlice } from './slices/login';
import { shellSlice } from './slices/shell';
import { teacherWorkspaceSlice } from './slices/teacher-workspace';
import { trainingSlice } from './slices/training';

export type SupportedLocale = 'zh-CN' | 'en-US';
export type TranslationSlice = Record<SupportedLocale, Record<string, any>>;

const slices: TranslationSlice[] = [
  commonSlice,
  loginSlice,
  shellSlice,
  teacherWorkspaceSlice,
  diagnosisSlice,
  trainingSlice,
  analyticsSlice,
];

function mergeLocaleSlices(locale: SupportedLocale): Record<string, any> {
  return slices.reduce<Record<string, any>>((merged, slice) => ({ ...merged, ...slice[locale] }), {});
}

export const resources = {
  'zh-CN': {
    translation: mergeLocaleSlices('zh-CN'),
  },
  'en-US': {
    translation: mergeLocaleSlices('en-US'),
  },
} as const;
