import { apiGet } from '../api';
import type { TeacherWorkspaceOverviewVO } from '../contracts/teacherWorkspace';

export const teacherWorkspaceService = {
  getOverview: (options?: { signal?: AbortSignal }) =>
    apiGet<TeacherWorkspaceOverviewVO>('/teacher/workspace/overview', options),
};
