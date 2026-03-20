import { apiDelete, apiDownload, apiGet, apiPost, apiPut, apiUpload } from './api';
import type {
  AddLexicalListItemsRequest,
  AddLexicalListItemsResultVO,
  AiGuidanceResponseVO,
  ClassAnalyticsOverviewVO,
  ClassCompletionRateVO,
  CreateLexicalListRequest,
  CsvImportResultVO,
  CsvImportTemplateVO,
  CurrentUserVO,
  DiagnosisHistorySummaryVO,
  DiagnosisResultDetailVO,
  DiagnosisSessionCreatedVO,
  DiagnosisSessionProgressVO,
  DiagnosisTemplateDetailVO,
  DiagnosisTemplateSummaryVO,
  LexicalListDetailVO,
  LexicalListSummaryVO,
  LexicalPairDetailVO,
  LexicalPairSummaryVO,
  LexicalPairUpsertRequest,
  LexicalRagAnswerVO,
  LoginResponse,
  PageResult,
  RecommendedTrainingPlanVO,
  ReviewScheduleItemVO,
  StudentAnalyticsOverviewVO,
  StudentProfileSummaryVO,
  TeacherInterventionSummaryVO,
  TeacherStudentDetailVO,
  TeachingClassSummaryVO,
  TrainingNextItemVO,
  TrainingSessionCreatedVO,
  TrainingSessionProgressVO,
  TrainingSessionSummaryVO,
  UserSummaryVO,
  WrongBookItemVO,
  AnalyticsTrendVO,
  AnalyticsHeatmapVO,
  AnalyticsScatterVO,
  AnalyticsRiskPairVO,
  AnalyticsErrorDistributionVO,
  AnalyticsRiskBucketVO,
  DiagnosisNextItemVO,
  DiagnosisTemplateUpsertRequest,
} from './contracts';

export const authService = {
  login: (payload: { usernameOrEmail: string; password: string }) =>
    apiPost<LoginResponse>('/auth/login', payload),
  refresh: (refreshToken: string) => apiPost<LoginResponse>('/auth/refresh', { refreshToken }),
  logout: () => apiPost<void>('/auth/logout'),
  me: () => apiGet<CurrentUserVO>('/auth/me'),
};

export const studentService = {
  getOverview: () => apiGet<StudentAnalyticsOverviewVO>('/student/analytics/overview'),
  getTrends: (range = '30d', bucket = 'day') =>
    apiGet<AnalyticsTrendVO>('/student/analytics/trends', { params: { range, bucket } }),
  getHeatmap: (range = '30d') =>
    apiGet<AnalyticsHeatmapVO>('/student/analytics/transfer-heatmap', { params: { range } }),
  getScatter: (range = '30d') =>
    apiGet<AnalyticsScatterVO>('/student/analytics/scatter', { params: { range } }),
  getHighRiskPairs: (range = '30d', limit = 10) =>
    apiGet<AnalyticsRiskPairVO[]>('/student/analytics/high-risk-pairs', { params: { range, limit } }),
  getErrorDistribution: (range = '30d') =>
    apiGet<AnalyticsErrorDistributionVO[]>('/student/analytics/error-distribution', { params: { range } }),
  exportCsv: (range = '30d') => apiDownload('/student/analytics/export.csv', { params: { range } }),
};

export const diagnosisTemplateService = {
  listPublished: (params: { pageNo?: number; pageSize?: number; keyword?: string }) =>
    apiGet<PageResult<DiagnosisTemplateSummaryVO>>('/student/diagnosis-templates', { params }),
  listTeacherTemplates: (params: { pageNo?: number; pageSize?: number; keyword?: string; status?: string; mineOnly?: boolean }) =>
    apiGet<PageResult<DiagnosisTemplateSummaryVO>>('/teacher/diagnosis-templates', { params }),
  getTeacherTemplate: (templateId: number) =>
    apiGet<DiagnosisTemplateDetailVO>(`/teacher/diagnosis-templates/${templateId}`),
  createTeacherTemplate: (payload: DiagnosisTemplateUpsertRequest) =>
    apiPost<number>('/teacher/diagnosis-templates', payload),
  updateTeacherTemplate: (templateId: number, payload: DiagnosisTemplateUpsertRequest) =>
    apiPut<number>(`/teacher/diagnosis-templates/${templateId}`, payload),
};

export const diagnosisSessionService = {
  listHistory: (params: { pageNo?: number; pageSize?: number; status?: string; templateId?: number }) =>
    apiGet<PageResult<DiagnosisHistorySummaryVO>>('/diagnosis/sessions', { params }),
  create: (templateId: number) => apiPost<DiagnosisSessionCreatedVO>('/diagnosis/sessions', { templateId }),
  getNextItem: (sessionId: number) => apiGet<DiagnosisNextItemVO>(`/diagnosis/sessions/${sessionId}/next-item`),
  submitAnswer: (
    sessionId: number,
    payload: {
      itemResultId: number;
      selectedSemanticMatch?: boolean;
      selectedAnswerKey?: string;
      reactionTimeMs: number;
      hesitationTimeMs: number;
    }
  ) => apiPost<DiagnosisSessionProgressVO>(`/diagnosis/sessions/${sessionId}/answers`, payload),
  saveProgress: (sessionId: number, progressSnapshot: Record<string, unknown>) =>
    apiPost<DiagnosisSessionProgressVO>(`/diagnosis/sessions/${sessionId}/progress`, { progressSnapshot }),
  complete: (sessionId: number) => apiPost<DiagnosisSessionProgressVO>(`/diagnosis/sessions/${sessionId}/complete`),
  getResult: (sessionId: number) => apiGet<DiagnosisResultDetailVO>(`/diagnosis/sessions/${sessionId}/result`),
};

export const trainingService = {
  getRecommendedPlan: () => apiGet<RecommendedTrainingPlanVO>('/training/plans/recommended'),
  getWrongBook: () => apiGet<WrongBookItemVO[]>('/training/wrong-book'),
  getReviewSchedule: (pendingOnly = true) =>
    apiGet<ReviewScheduleItemVO[]>('/training/review-schedule', { params: { pendingOnly } }),
  startSession: (payload: { planId: number; mode: string }) =>
    apiPost<TrainingSessionCreatedVO>('/training/sessions', payload),
  getNextItem: (sessionId: number) => apiGet<TrainingNextItemVO>(`/training/sessions/${sessionId}/next-item`),
  submitAnswer: (
    sessionId: number,
    payload: { itemResultId: number; selectedAnswerKey: string; reactionTimeMs: number; hesitationTimeMs: number }
  ) => apiPost<TrainingSessionProgressVO>(`/training/sessions/${sessionId}/answers`, payload),
  complete: (sessionId: number) => apiPost<TrainingSessionProgressVO>(`/training/sessions/${sessionId}/complete`),
  getSummary: (sessionId: number) => apiGet<TrainingSessionSummaryVO>(`/training/sessions/${sessionId}/summary`),
};

export const aiService = {
  explainDiagnosis: (diagnosisSummaryId?: number | null) =>
    apiPost<AiGuidanceResponseVO>('/ai/explain-diagnosis', diagnosisSummaryId ? { diagnosisSummaryId } : undefined),
  recommendTraining: (diagnosisSummaryId?: number | null) =>
    apiPost<AiGuidanceResponseVO>('/ai/recommend-training', diagnosisSummaryId ? { diagnosisSummaryId } : undefined),
  queryLexicalRag: (query: string) => apiPost<LexicalRagAnswerVO>('/ai/lexical-rag/query', { query }),
  suggestTeacherIntervention: (payload: { classId: number; studentUserId: number; diagnosisSummaryId?: number | null }) =>
    apiPost<AiGuidanceResponseVO>('/teacher/intervention-suggest', payload),
};

export const teacherAnalyticsService = {
  listClasses: () => apiGet<TeachingClassSummaryVO[]>('/teacher/analytics/classes'),
  getClassOverview: (classId: number, range = '30d') =>
    apiGet<ClassAnalyticsOverviewVO>(`/teacher/analytics/classes/${classId}/overview`, { params: { range } }),
  getRiskDistribution: (classId: number) =>
    apiGet<AnalyticsRiskBucketVO[]>(`/teacher/analytics/classes/${classId}/risk-distribution`),
  getHeatmap: (classId: number, range = '30d') =>
    apiGet<AnalyticsHeatmapVO>(`/teacher/analytics/classes/${classId}/transfer-heatmap`, { params: { range } }),
  getErrorDistribution: (classId: number, range = '30d') =>
    apiGet<AnalyticsErrorDistributionVO[]>(`/teacher/analytics/classes/${classId}/error-distribution`, { params: { range } }),
  getCompletionRate: (classId: number, range = '30d', bucket = 'day') =>
    apiGet<ClassCompletionRateVO>(`/teacher/analytics/classes/${classId}/completion-rate`, { params: { range, bucket } }),
  listStudents: (classId: number) =>
    apiGet<StudentProfileSummaryVO[]>(`/teacher/analytics/classes/${classId}/students`),
  getStudentDetail: (classId: number, studentUserId: number) =>
    apiGet<TeacherStudentDetailVO>(`/teacher/analytics/classes/${classId}/students/${studentUserId}`),
  exportClassCsv: (classId: number, range = '30d') =>
    apiDownload(`/teacher/analytics/classes/${classId}/export.csv`, { params: { range } }),
};

export const teacherInterventionService = {
  list: (params: { classId?: number; status?: string; pageNo?: number; pageSize?: number }) =>
    apiGet<PageResult<TeacherInterventionSummaryVO>>('/teacher/interventions', { params }),
  complete: (interventionId: number) => apiPost<void>(`/teacher/interventions/${interventionId}/complete`),
};

export const lexicalPairService = {
  pageQuery: (params: { pageNo?: number; pageSize?: number; keyword?: string; lexicalPairType?: string; riskLevel?: string; contextSupportLevel?: string; active?: boolean }) =>
    apiGet<PageResult<LexicalPairSummaryVO>>('/lexical-pairs', { params }),
  getDetail: (lexicalPairId: number) => apiGet<LexicalPairDetailVO>(`/lexical-pairs/${lexicalPairId}`),
  create: (payload: LexicalPairUpsertRequest) => apiPost<number>('/lexical-pairs', payload),
  update: (lexicalPairId: number, payload: LexicalPairUpsertRequest) => apiPut<number>(`/lexical-pairs/${lexicalPairId}`, payload),
  delete: (lexicalPairId: number) => apiDelete<void>(`/lexical-pairs/${lexicalPairId}`),
  getImportTemplate: () => apiGet<CsvImportTemplateVO>('/lexical-pairs/import-template'),
  importCsv: (formData: FormData) => apiUpload<CsvImportResultVO>('/lexical-pairs/import', formData),
};

export const lexicalListService = {
  pageQuery: (params: { pageNo?: number; pageSize?: number; keyword?: string }) =>
    apiGet<PageResult<LexicalListSummaryVO>>('/lexical-lists', { params }),
  getDetail: (lexicalListId: number) => apiGet<LexicalListDetailVO>(`/lexical-lists/${lexicalListId}`),
  create: (payload: CreateLexicalListRequest) => apiPost<number>('/lexical-lists', payload),
  addItems: (lexicalListId: number, payload: AddLexicalListItemsRequest) =>
    apiPost<AddLexicalListItemsResultVO>(`/lexical-lists/${lexicalListId}/items`, payload),
  deleteItem: (lexicalListId: number, itemId: number) => apiDelete<void>(`/lexical-lists/${lexicalListId}/items/${itemId}`),
};

export const adminService = {
  listUsers: () => apiGet<UserSummaryVO[]>('/admin/users'),
};
