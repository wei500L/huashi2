import type { AxiosRequestConfig } from 'axios';
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
  LexicalImportBatchCreatedVO,
  LexicalImportBatchDetailVO,
  LexicalImportBatchSummaryVO,
  LexicalImportRowUpdateRequest,
  LexicalImportRowVO,
  LexicalPairOverviewVO,
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
  TrainingHistorySummaryVO,
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
  AdminAiConfigSaveRequest,
  AdminAiConfigViewVO,
  AdminOutboxRecordVO,
  AiGatewayHealthResponse,
  AiOpsConfigValidationResponse,
  RagReindexJobResponse,
  RagReindexRequest,
  RagReindexResponse,
} from './contracts';

type RequestOptions = Pick<AxiosRequestConfig, 'signal' | 'timeout'>;

export const authService = {
  login: (payload: { usernameOrEmail: string; password: string }) =>
    apiPost<LoginResponse>('/auth/login', payload),
  refresh: (refreshToken: string) => apiPost<LoginResponse>('/auth/refresh', { refreshToken }),
  logout: () => apiPost<void>('/auth/logout'),
  me: (options?: RequestOptions) => apiGet<CurrentUserVO>('/auth/me', options),
};

export const studentService = {
  getOverview: (options?: RequestOptions) => apiGet<StudentAnalyticsOverviewVO>('/student/analytics/overview', options),
  getTrends: (range = '30d', bucket = 'day', options?: RequestOptions) =>
    apiGet<AnalyticsTrendVO>('/student/analytics/trends', { ...options, params: { range, bucket } }),
  getHeatmap: (range = '30d', options?: RequestOptions) =>
    apiGet<AnalyticsHeatmapVO>('/student/analytics/transfer-heatmap', { ...options, params: { range } }),
  getScatter: (range = '30d', options?: RequestOptions) =>
    apiGet<AnalyticsScatterVO>('/student/analytics/scatter', { ...options, params: { range } }),
  getHighRiskPairs: (range = '30d', limit = 10, options?: RequestOptions) =>
    apiGet<AnalyticsRiskPairVO[]>('/student/analytics/high-risk-pairs', { ...options, params: { range, limit } }),
  getErrorDistribution: (range = '30d', options?: RequestOptions) =>
    apiGet<AnalyticsErrorDistributionVO[]>('/student/analytics/error-distribution', { ...options, params: { range } }),
  exportCsv: (range = '30d', options?: RequestOptions) => apiDownload('/student/analytics/export.csv', { ...options, params: { range } }),
};

export const diagnosisTemplateService = {
  listPublished: (params: { pageNo?: number; pageSize?: number; keyword?: string }, options?: RequestOptions) =>
    apiGet<PageResult<DiagnosisTemplateSummaryVO>>('/student/diagnosis-templates', { ...options, params }),
  listTeacherTemplates: (params: { pageNo?: number; pageSize?: number; keyword?: string; status?: string; mineOnly?: boolean }, options?: RequestOptions) =>
    apiGet<PageResult<DiagnosisTemplateSummaryVO>>('/teacher/diagnosis-templates', { ...options, params }),
  getTeacherTemplate: (templateId: number, options?: RequestOptions) =>
    apiGet<DiagnosisTemplateDetailVO>(`/teacher/diagnosis-templates/${templateId}`, options),
  createTeacherTemplate: (payload: DiagnosisTemplateUpsertRequest) =>
    apiPost<number>('/teacher/diagnosis-templates', payload),
  updateTeacherTemplate: (templateId: number, payload: DiagnosisTemplateUpsertRequest) =>
    apiPut<number>(`/teacher/diagnosis-templates/${templateId}`, payload),
};

export const diagnosisSessionService = {
  listHistory: (params: { pageNo?: number; pageSize?: number; status?: string; templateId?: number }, options?: RequestOptions) =>
    apiGet<PageResult<DiagnosisHistorySummaryVO>>('/diagnosis/sessions', { ...options, params }),
  create: (templateId: number) => apiPost<DiagnosisSessionCreatedVO>('/diagnosis/sessions', { templateId }),
  getNextItem: (sessionId: number, options?: RequestOptions) => apiGet<DiagnosisNextItemVO>(`/diagnosis/sessions/${sessionId}/next-item`, options),
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
  getResult: (sessionId: number, options?: RequestOptions) => apiGet<DiagnosisResultDetailVO>(`/diagnosis/sessions/${sessionId}/result`, options),
};

export const trainingService = {
  getRecommendedPlan: (options?: RequestOptions) => apiGet<RecommendedTrainingPlanVO>('/training/plans/recommended', options),
  getWrongBook: (options?: RequestOptions) => apiGet<WrongBookItemVO[]>('/training/wrong-book', options),
  getReviewSchedule: (pendingOnly = true, options?: RequestOptions) =>
    apiGet<ReviewScheduleItemVO[]>('/training/review-schedule', { ...options, params: { pendingOnly } }),
  listHistory: (params: { pageNo?: number; pageSize?: number; status?: string; planId?: number }, options?: RequestOptions) =>
    apiGet<PageResult<TrainingHistorySummaryVO>>('/training/sessions', { ...options, params }),
  startSession: (payload: { planId: number; mode: string }) =>
    apiPost<TrainingSessionCreatedVO>('/training/sessions', payload),
  getNextItem: (sessionId: number, options?: RequestOptions) => apiGet<TrainingNextItemVO>(`/training/sessions/${sessionId}/next-item`, options),
  saveProgress: (sessionId: number, progressSnapshot: Record<string, unknown>) =>
    apiPost<TrainingSessionProgressVO>(`/training/sessions/${sessionId}/progress`, { progressSnapshot }),
  submitAnswer: (
    sessionId: number,
    payload: { itemResultId: number; selectedAnswerKey: string; reactionTimeMs: number; hesitationTimeMs: number }
  ) => apiPost<TrainingSessionProgressVO>(`/training/sessions/${sessionId}/answers`, payload),
  complete: (sessionId: number) => apiPost<TrainingSessionProgressVO>(`/training/sessions/${sessionId}/complete`),
  getSummary: (sessionId: number, options?: RequestOptions) => apiGet<TrainingSessionSummaryVO>(`/training/sessions/${sessionId}/summary`, options),
};

export const aiService = {
  explainDiagnosis: (diagnosisSummaryId?: number | null, options?: RequestOptions) =>
    apiPost<AiGuidanceResponseVO>('/ai/explain-diagnosis', diagnosisSummaryId ? { diagnosisSummaryId } : undefined, options),
  recommendTraining: (diagnosisSummaryId?: number | null, options?: RequestOptions) =>
    apiPost<AiGuidanceResponseVO>('/ai/recommend-training', diagnosisSummaryId ? { diagnosisSummaryId } : undefined, options),
  queryLexicalRag: (query: string) => apiPost<LexicalRagAnswerVO>('/ai/lexical-rag/query', { query }),
  suggestTeacherIntervention: (payload: { classId: number; studentUserId: number; diagnosisSummaryId?: number | null }) =>
    apiPost<AiGuidanceResponseVO>('/teacher/intervention-suggest', payload),
};

export const teacherAnalyticsService = {
  listClasses: (options?: RequestOptions) => apiGet<TeachingClassSummaryVO[]>('/teacher/analytics/classes', options),
  getClassOverview: (classId: number, range = '30d', options?: RequestOptions) =>
    apiGet<ClassAnalyticsOverviewVO>(`/teacher/analytics/classes/${classId}/overview`, { ...options, params: { range } }),
  getRiskDistribution: (classId: number, options?: RequestOptions) =>
    apiGet<AnalyticsRiskBucketVO[]>(`/teacher/analytics/classes/${classId}/risk-distribution`, options),
  getHeatmap: (classId: number, range = '30d', options?: RequestOptions) =>
    apiGet<AnalyticsHeatmapVO>(`/teacher/analytics/classes/${classId}/transfer-heatmap`, { ...options, params: { range } }),
  getErrorDistribution: (classId: number, range = '30d', options?: RequestOptions) =>
    apiGet<AnalyticsErrorDistributionVO[]>(`/teacher/analytics/classes/${classId}/error-distribution`, { ...options, params: { range } }),
  getCompletionRate: (classId: number, range = '30d', bucket = 'day', options?: RequestOptions) =>
    apiGet<ClassCompletionRateVO>(`/teacher/analytics/classes/${classId}/completion-rate`, { ...options, params: { range, bucket } }),
  listStudents: (classId: number, options?: RequestOptions) =>
    apiGet<StudentProfileSummaryVO[]>(`/teacher/analytics/classes/${classId}/students`, options),
  getStudentDetail: (classId: number, studentUserId: number, options?: RequestOptions) =>
    apiGet<TeacherStudentDetailVO>(`/teacher/analytics/classes/${classId}/students/${studentUserId}`, options),
  exportClassCsv: (classId: number, range = '30d', options?: RequestOptions) =>
    apiDownload(`/teacher/analytics/classes/${classId}/export.csv`, { ...options, params: { range } }),
};

export const teacherInterventionService = {
  list: (params: { classId?: number; status?: string; pageNo?: number; pageSize?: number }, options?: RequestOptions) =>
    apiGet<PageResult<TeacherInterventionSummaryVO>>('/teacher/interventions', { ...options, params }),
  complete: (interventionId: number) => apiPost<void>(`/teacher/interventions/${interventionId}/complete`),
};

export const lexicalPairService = {
  pageQuery: (params: { pageNo?: number; pageSize?: number; keyword?: string; lexicalPairType?: string; riskLevel?: string; contextSupportLevel?: string; active?: boolean; embeddingStatus?: string }, options?: RequestOptions) =>
    apiGet<PageResult<LexicalPairSummaryVO>>('/lexical-pairs', { ...options, params }),
  getOverview: (options?: RequestOptions) => apiGet<LexicalPairOverviewVO>('/lexical-pairs/overview', options),
  getDetail: (lexicalPairId: number, options?: RequestOptions) => apiGet<LexicalPairDetailVO>(`/lexical-pairs/${lexicalPairId}`, options),
  create: (payload: LexicalPairUpsertRequest) => apiPost<number>('/lexical-pairs', payload),
  update: (lexicalPairId: number, payload: LexicalPairUpsertRequest) => apiPut<number>(`/lexical-pairs/${lexicalPairId}`, payload),
  delete: (lexicalPairId: number) => apiDelete<void>(`/lexical-pairs/${lexicalPairId}`),
  getImportTemplate: (options?: RequestOptions) => apiGet<CsvImportTemplateVO>('/lexical-pairs/import-template', options),
  importCsv: (formData: FormData) => apiUpload<CsvImportResultVO>('/lexical-pairs/import', formData),
  createImportBatch: (formData: FormData, options?: RequestOptions) =>
    apiUpload<LexicalImportBatchCreatedVO>('/lexical-pairs/import-batches', formData, options),
  listImportBatches: (
    params: { pageNo?: number; pageSize?: number; status?: string; keyword?: string; ownerUserId?: number },
    options?: RequestOptions
  ) => apiGet<PageResult<LexicalImportBatchSummaryVO>>('/lexical-pairs/import-batches', { ...options, params }),
  getImportBatch: (batchId: number, options?: RequestOptions) =>
    apiGet<LexicalImportBatchDetailVO>(`/lexical-pairs/import-batches/${batchId}`, options),
  listImportRows: (
    batchId: number,
    params: { pageNo?: number; pageSize?: number; status?: string },
    options?: RequestOptions
  ) => apiGet<PageResult<LexicalImportRowVO>>(`/lexical-pairs/import-batches/${batchId}/rows`, { ...options, params }),
  updateImportRow: (batchId: number, rowId: number, payload: LexicalImportRowUpdateRequest) =>
    apiPut<LexicalImportRowVO>(`/lexical-pairs/import-batches/${batchId}/rows/${rowId}`, payload),
  commitImportBatch: (batchId: number) =>
    apiPost<LexicalImportBatchCreatedVO>(`/lexical-pairs/import-batches/${batchId}/commit`),
  downloadImportFile: (batchId: number, options?: RequestOptions) =>
    apiDownload(`/lexical-pairs/import-batches/${batchId}/file`, options),
};

export const lexicalListService = {
  pageQuery: (params: { pageNo?: number; pageSize?: number; keyword?: string }, options?: RequestOptions) =>
    apiGet<PageResult<LexicalListSummaryVO>>('/lexical-lists', { ...options, params }),
  getDetail: (lexicalListId: number, options?: RequestOptions) => apiGet<LexicalListDetailVO>(`/lexical-lists/${lexicalListId}`, options),
  create: (payload: CreateLexicalListRequest) => apiPost<number>('/lexical-lists', payload),
  addItems: (lexicalListId: number, payload: AddLexicalListItemsRequest) =>
    apiPost<AddLexicalListItemsResultVO>(`/lexical-lists/${lexicalListId}/items`, payload),
  deleteItem: (lexicalListId: number, itemId: number) => apiDelete<void>(`/lexical-lists/${lexicalListId}/items/${itemId}`),
};

export const adminService = {
  listUsers: (options?: RequestOptions) => apiGet<UserSummaryVO[]>('/admin/users', options),
  getAiConfig: (options?: RequestOptions) => apiGet<AdminAiConfigViewVO>('/admin/ai-config', options),
  validateAiConfig: (payload: AdminAiConfigSaveRequest) => apiPost<AiOpsConfigValidationResponse>('/admin/ai-config/validate', payload),
  saveAiConfig: (payload: AdminAiConfigSaveRequest) => apiPut<AdminAiConfigViewVO>('/admin/ai-config', payload),
  getOutboxRecords: (status?: string, limit?: number, options?: RequestOptions) =>
    apiGet<AdminOutboxRecordVO[]>('/admin/ai-config/outbox', { ...options, params: { status, limit } }),
  replayOutboxRecord: (id: number) => apiPost<AdminOutboxRecordVO>(`/admin/ai-config/outbox/${id}/replay`),
  getAiHealth: (options?: RequestOptions) => apiGet<AiGatewayHealthResponse>('/admin/ai-config/health', options),
  triggerRagReindex: (payload: RagReindexRequest) => apiPost<RagReindexResponse>('/admin/ai-config/reindex', payload),
  getRagReindexJob: (jobId: number, options?: RequestOptions) => apiGet<RagReindexJobResponse>(`/admin/ai-config/reindex-jobs/${jobId}`, options),
};
