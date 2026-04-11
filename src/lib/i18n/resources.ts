import { analyticsSlice } from './slices/analytics';
import { commonSlice } from './slices/common';
import { diagnosisSlice } from './slices/diagnosis';
import { loginSlice } from './slices/login';
import { shellSlice } from './slices/shell';
import { taskPagesSlice } from './slices/task-pages';
import { teacherWorkspaceSlice } from './slices/teacher-workspace';
import { trainingSlice } from './slices/training';
import { uiSlice } from './slices/ui';

export type SupportedLocale = 'zh-CN' | 'en-US';
export type TranslationSlice = Record<SupportedLocale, Record<string, unknown>>;

const slices: TranslationSlice[] = [
  commonSlice,
  loginSlice,
  shellSlice,
  taskPagesSlice,
  teacherWorkspaceSlice,
  diagnosisSlice,
  trainingSlice,
  analyticsSlice,
  uiSlice,
];

function mergeLocaleSlices(locale: SupportedLocale): Record<string, unknown> {
  return slices.reduce<Record<string, unknown>>((merged, slice) => ({ ...merged, ...slice[locale] }), {});
}

export const resources = {
  'zh-CN': {
    translation: mergeLocaleSlices('zh-CN'),
  },
  'en-US': {
    translation: mergeLocaleSlices('en-US'),
  },
} as const;
