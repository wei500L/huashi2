import type { AxiosRequestConfig } from 'axios';
import { apiDelete, apiDownload, apiGet, apiPost, apiPostKeepalive, apiPut, apiUpload } from './api';
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
  DiagnosisTemplateDeleteResultVO,
  DiagnosisTemplateDetailVO,
  DiagnosisTemplateDraftDetailVO,
  DiagnosisTemplateDraftSaveRequest,
  DiagnosisTemplateDraftSummaryVO,
  DiagnosisTemplateDraftValidationResponseVO,
  DiagnosisTemplateSummaryVO,
  LexicalListDetailVO,
  LexicalListItemVO,
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
  RegisterStudentRequest,
  NotificationItemVO,
  NotificationUnreadCountVO,
  PageResult,
  RecommendedTrainingPlanVO,
  ReorderLexicalListItemsRequest,
  ReviewScheduleItemVO,
  SessionOverviewVO,
  StudentAnalyticsOverviewVO,
  StudentProfileSummaryVO,
  TeacherInterventionSummaryVO,
  TeacherClassDetailVO,
  TeacherClassStudentBatchRequest,
  TeacherClassStudentCandidateVO,
  TeacherClassUpsertRequest,
  TeacherInterventionUpdateRequest,
  TeacherStudentDetailVO,
  TeachingClassSummaryVO,
  TrainingHistorySummaryVO,
  TrainingNextItemVO,
  TrainingSessionCreatedVO,
  TrainingSessionProgressVO,
  TrainingSessionSummaryVO,
  UpdateLexicalListRequest,
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
  AdminUserAccessUpdateRequest,
  AdminUserCreateRequest,
  AdminAiConfigViewVO,
  AdminUserProvisionResultVO,
  AccountActionLinkVO,
  AccountActionPreviewVO,
  AssessmentAttemptDetailVO,
  AssessmentAttemptProgressVO,
  AssessmentAttemptResultVO,
  AssessmentAttemptStartVO,
  AssessmentAttemptSubmitVO,
  AssessmentPaperDetailVO,
  AssessmentPaperSaveRequest,
  AssessmentPaperSummaryVO,
  AssessmentPublishDetailVO,
  AssessmentPublishRequest,
  AssessmentPublishSummaryVO,
  CompleteAccountActionRequest,
  AdminOutboxRecordVO,
  AiGatewayHealthResponse,
  AiOpsConfigValidationResponse,
  RagReindexJobResponse,
  RagReindexRequest,
  RagReindexResponse,
  SaveAssessmentResponsesRequest,
  StudentAssessmentHistorySummaryVO,
  StudentRegistrationContextVO,
  StudentAssessmentSummaryVO,
  TeacherAssessmentAttemptResultVO,
} from './contracts';

type RequestOptions = Pick<AxiosRequestConfig, 'signal' | 'timeout'>;

export const authService = {
  login: (payload: { usernameOrEmail: string; password: string }) =>
    apiPost<LoginResponse>('/auth/login', payload),
  registerStudent: (payload: RegisterStudentRequest) =>
    apiPost<LoginResponse>('/auth/register', payload),
  getRegistrationContext: (classCode: string, options?: RequestOptions) =>
    apiGet<StudentRegistrationContextVO>(`/auth/register/context/${encodeURIComponent(classCode)}`, options),
  refresh: (refreshToken: string) => apiPost<LoginResponse>('/auth/refresh', { refreshToken }),
  logout: () => apiPost<void>('/auth/logout'),
  me: (options?: RequestOptions) => apiGet<CurrentUserVO>('/auth/me', options),
  getSessionOverview: (options?: RequestOptions) => apiGet<SessionOverviewVO>('/auth/session-overview', options),
  previewAccountAction: (token: string, options?: RequestOptions) => apiGet<AccountActionPreviewVO>(`/auth/account-actions/${token}`, options),
  completeAccountAction: (token: string, payload: CompleteAccountActionRequest) =>
    apiPost<void>(`/auth/account-actions/${token}/complete`, payload),
};

export const notificationService = {
  list: (params: { pageNo?: number; pageSize?: number; unreadOnly?: boolean }, options?: RequestOptions) =>
    apiGet<PageResult<NotificationItemVO>>('/notifications', { ...options, params }),
  getUnreadCount: (options?: RequestOptions) => apiGet<NotificationUnreadCountVO>('/notifications/unread-count', options),
  markRead: (notificationId: number) => apiPost<NotificationItemVO>(`/notifications/${notificationId}/read`),
  markAllRead: () => apiPost<NotificationUnreadCountVO>('/notifications/read-all'),
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
  listDrafts: (params: { pageNo?: number; pageSize?: number; keyword?: string }, options?: RequestOptions) =>
    apiGet<PageResult<DiagnosisTemplateDraftSummaryVO>>('/teacher/diagnosis-template-drafts', { ...options, params }),
  createDraft: () => apiPost<DiagnosisTemplateDraftDetailVO>('/teacher/diagnosis-template-drafts'),
  createDraftFromTemplate: (templateId: number) =>
    apiPost<DiagnosisTemplateDraftDetailVO>(`/teacher/diagnosis-template-drafts/from-template/${templateId}`),
  getDraft: (draftId: number, options?: RequestOptions) =>
    apiGet<DiagnosisTemplateDraftDetailVO>(`/teacher/diagnosis-template-drafts/${draftId}`, options),
  saveDraft: (draftId: number, payload: DiagnosisTemplateDraftSaveRequest) =>
    apiPut<DiagnosisTemplateDraftDetailVO>(`/teacher/diagnosis-template-drafts/${draftId}`, payload),
  validateDraft: (draftId: number) =>
    apiPost<DiagnosisTemplateDraftValidationResponseVO>(`/teacher/diagnosis-template-drafts/${draftId}/validate`),
  publishDraft: (draftId: number) =>
    apiPost<DiagnosisTemplateDetailVO>(`/teacher/diagnosis-template-drafts/${draftId}/publish`),
  deleteDraft: (draftId: number) => apiDelete<void>(`/teacher/diagnosis-template-drafts/${draftId}`),
  getTeacherTemplate: (templateId: number, options?: RequestOptions) =>
    apiGet<DiagnosisTemplateDetailVO>(`/teacher/diagnosis-templates/${templateId}`, options),
  createTeacherTemplate: (payload: DiagnosisTemplateUpsertRequest) =>
    apiPost<number>('/teacher/diagnosis-templates', payload),
  updateTeacherTemplate: (templateId: number, payload: DiagnosisTemplateUpsertRequest) =>
    apiPut<number>(`/teacher/diagnosis-templates/${templateId}`, payload),
  deleteTeacherTemplate: (templateId: number) =>
    apiDelete<DiagnosisTemplateDeleteResultVO>(`/teacher/diagnosis-templates/${templateId}`),
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
  saveProgressKeepalive: (sessionId: number, progressSnapshot: Record<string, unknown>) =>
    apiPostKeepalive<DiagnosisSessionProgressVO>(`/diagnosis/sessions/${sessionId}/progress`, { progressSnapshot }),
  complete: (sessionId: number) => apiPost<DiagnosisSessionProgressVO>(`/diagnosis/sessions/${sessionId}/complete`),
  getResult: (sessionId: number, options?: RequestOptions) => apiGet<DiagnosisResultDetailVO>(`/diagnosis/sessions/${sessionId}/result`, options),
};

export const trainingService = {
  getRecommendedPlan: (params?: { diagnosisSummaryId?: number | null }, options?: RequestOptions) =>
    apiGet<RecommendedTrainingPlanVO>('/training/plans/recommended', { ...options, params }),
  getWrongBook: (options?: RequestOptions) => apiGet<WrongBookItemVO[]>('/training/wrong-book', options),
  getReviewSchedule: (pendingOnly = true, options?: RequestOptions) =>
    apiGet<ReviewScheduleItemVO[]>('/training/review-schedule', { ...options, params: { pendingOnly } }),
  listHistory: (params: { pageNo?: number; pageSize?: number; status?: string; planId?: number }, options?: RequestOptions) =>
    apiGet<PageResult<TrainingHistorySummaryVO>>('/training/sessions', { ...options, params }),
  startSession: (payload: {
    planId: number;
    mode: string;
    launchSource?: string;
    diagnosisSummaryId?: number | null;
    lexicalPairId?: number | null;
    wrongBookId?: number | null;
    reviewScheduleId?: number | null;
  }) =>
    apiPost<TrainingSessionCreatedVO>('/training/sessions', payload),
  getNextItem: (sessionId: number, options?: RequestOptions) => apiGet<TrainingNextItemVO>(`/training/sessions/${sessionId}/next-item`, options),
  saveProgress: (sessionId: number, progressSnapshot: Record<string, unknown>) =>
    apiPost<TrainingSessionProgressVO>(`/training/sessions/${sessionId}/progress`, { progressSnapshot }),
  saveProgressKeepalive: (sessionId: number, progressSnapshot: Record<string, unknown>) =>
    apiPostKeepalive<TrainingSessionProgressVO>(`/training/sessions/${sessionId}/progress`, { progressSnapshot }),
  submitAnswer: (
    sessionId: number,
    payload: { itemResultId: number; selectedAnswerKey: string; reactionTimeMs: number; hesitationTimeMs: number }
  ) => apiPost<TrainingSessionProgressVO>(`/training/sessions/${sessionId}/answers`, payload),
  complete: (sessionId: number) => apiPost<TrainingSessionProgressVO>(`/training/sessions/${sessionId}/complete`),
  getSummary: (sessionId: number, options?: RequestOptions) => apiGet<TrainingSessionSummaryVO>(`/training/sessions/${sessionId}/summary`, options),
};

export const assessmentService = {
  listTeacherPapers: (options?: RequestOptions) => apiGet<AssessmentPaperSummaryVO[]>('/teacher/assessments/papers', options),
  getTeacherPaper: (paperId: number, options?: RequestOptions) =>
    apiGet<AssessmentPaperDetailVO>(`/teacher/assessments/papers/${paperId}`, options),
  createTeacherPaper: (payload: AssessmentPaperSaveRequest) =>
    apiPost<AssessmentPaperDetailVO>('/teacher/assessments/papers', payload),
  updateTeacherPaper: (paperId: number, payload: AssessmentPaperSaveRequest) =>
    apiPut<AssessmentPaperDetailVO>(`/teacher/assessments/papers/${paperId}`, payload),
  publishTeacherPaper: (paperId: number, payload: AssessmentPublishRequest) =>
    apiPost<AssessmentPublishSummaryVO>(`/teacher/assessments/papers/${paperId}/publish`, payload),
  getTeacherPublish: (publishId: number, options?: RequestOptions) =>
    apiGet<AssessmentPublishDetailVO>(`/teacher/assessments/publishes/${publishId}`, options),
  getTeacherAttemptResult: (attemptId: number, options?: RequestOptions) =>
    apiGet<TeacherAssessmentAttemptResultVO>(`/teacher/assessments/attempts/${attemptId}/result`, options),
  listStudentAssessments: (options?: RequestOptions) =>
    apiGet<StudentAssessmentSummaryVO[]>('/student/assessments', options),
  listStudentHistory: (
    params: { pageNo?: number; pageSize?: number; status?: string },
    options?: RequestOptions
  ) => apiGet<PageResult<StudentAssessmentHistorySummaryVO>>('/student/assessments/history', { ...options, params }),
  startStudentAttempt: (publishId: number) =>
    apiPost<AssessmentAttemptStartVO>(`/student/assessments/publishes/${publishId}/start`),
  getStudentAttempt: (attemptId: number, options?: RequestOptions) =>
    apiGet<AssessmentAttemptDetailVO>(`/student/assessments/attempts/${attemptId}`, options),
  saveStudentResponses: (attemptId: number, payload: SaveAssessmentResponsesRequest) =>
    apiPost<AssessmentAttemptProgressVO>(`/student/assessments/attempts/${attemptId}/responses`, payload),
  saveStudentResponsesKeepalive: (attemptId: number, payload: SaveAssessmentResponsesRequest) =>
    apiPostKeepalive<AssessmentAttemptProgressVO>(`/student/assessments/attempts/${attemptId}/responses`, payload),
  submitStudentAttempt: (attemptId: number) =>
    apiPost<AssessmentAttemptSubmitVO>(`/student/assessments/attempts/${attemptId}/submit`),
  getStudentAttemptResult: (attemptId: number, options?: RequestOptions) =>
    apiGet<AssessmentAttemptResultVO>(`/student/assessments/attempts/${attemptId}/result`, options),
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

export const teacherClassService = {
  listClasses: (options?: RequestOptions) => apiGet<TeachingClassSummaryVO[]>('/teacher/classes', options),
  createClass: (payload: TeacherClassUpsertRequest) =>
    apiPost<TeacherClassDetailVO>('/teacher/classes', payload),
  getDetail: (classId: number, options?: RequestOptions) =>
    apiGet<TeacherClassDetailVO>(`/teacher/classes/${classId}`, options),
  updateClass: (classId: number, payload: TeacherClassUpsertRequest) =>
    apiPut<TeacherClassDetailVO>(`/teacher/classes/${classId}`, payload),
  archiveClass: (classId: number) => apiDelete<void>(`/teacher/classes/${classId}`),
  listStudentCandidates: (classId: number, keyword?: string, options?: RequestOptions) =>
    apiGet<TeacherClassStudentCandidateVO[]>(`/teacher/classes/${classId}/student-candidates`, {
      ...options,
      params: keyword?.trim() ? { keyword: keyword.trim() } : undefined,
    }),
  addStudents: (classId: number, payload: TeacherClassStudentBatchRequest) =>
    apiPost<TeacherClassDetailVO>(`/teacher/classes/${classId}/students`, payload),
  removeStudents: (classId: number, payload: TeacherClassStudentBatchRequest) =>
    apiPost<TeacherClassDetailVO>(`/teacher/classes/${classId}/students/remove`, payload),
};

export const teacherInterventionService = {
  list: (
    params: { classId?: number; view?: string; status?: string; priority?: string; studentUserId?: number; pageNo?: number; pageSize?: number },
    options?: RequestOptions
  ) =>
    apiGet<PageResult<TeacherInterventionSummaryVO>>('/teacher/interventions', { ...options, params }),
  update: (interventionId: number, payload: TeacherInterventionUpdateRequest) =>
    apiPut<TeacherInterventionSummaryVO>(`/teacher/interventions/${interventionId}`, payload),
  complete: (interventionId: number) => apiPost<void>(`/teacher/interventions/${interventionId}/complete`),
};

export const lexicalPairService = {
  pageQuery: (params: { pageNo?: number; pageSize?: number; keyword?: string; lexicalPairType?: string; riskLevel?: string; contextSupportLevel?: string; active?: boolean; embeddingStatus?: string }, options?: RequestOptions) =>
    apiGet<PageResult<LexicalPairSummaryVO>>('/lexical-pairs', { ...options, params }),
  getOverview: (options?: RequestOptions) => apiGet<LexicalPairOverviewVO>('/lexical-pairs/overview', options),
  getDetail: (lexicalPairId: number, options?: RequestOptions) => apiGet<LexicalPairDetailVO>(`/lexical-pairs/${lexicalPairId}`, options),
  exportCsv: (params: { keyword?: string; lexicalPairType?: string; riskLevel?: string; contextSupportLevel?: string; active?: boolean; embeddingStatus?: string }, options?: RequestOptions) =>
    apiDownload('/lexical-pairs/export.csv', { ...options, params }),
  create: (payload: LexicalPairUpsertRequest) => apiPost<number>('/lexical-pairs', payload),
  update: (lexicalPairId: number, payload: LexicalPairUpsertRequest) => apiPut<number>(`/lexical-pairs/${lexicalPairId}`, payload),
  delete: (lexicalPairId: number) => apiDelete<void>(`/lexical-pairs/${lexicalPairId}`),
  getImportTemplate: (options?: RequestOptions) => apiGet<CsvImportTemplateVO>('/lexical-pairs/import-template', options),
  importCsv: (formData: FormData) => apiUpload<CsvImportResultVO>('/lexical-pairs/import', formData),
  createImportBatch: (formData: FormData, options?: RequestOptions) =>
    apiUpload<LexicalImportBatchCreatedVO>('/lexical-pairs/import-batches', formData, options),
  listImportBatches: (
    params: { pageNo?: number; pageSize?: number; view?: string; status?: string; keyword?: string; ownerUserId?: number },
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
  update: (lexicalListId: number, payload: UpdateLexicalListRequest) =>
    apiPut<LexicalListDetailVO>(`/lexical-lists/${lexicalListId}`, payload),
  delete: (lexicalListId: number) => apiDelete<void>(`/lexical-lists/${lexicalListId}`),
  addItems: (lexicalListId: number, payload: AddLexicalListItemsRequest) =>
    apiPost<AddLexicalListItemsResultVO>(`/lexical-lists/${lexicalListId}/items`, payload),
  deleteItem: (lexicalListId: number, itemId: number) => apiDelete<void>(`/lexical-lists/${lexicalListId}/items/${itemId}`),
  pageItems: (
    lexicalListId: number,
    params: { pageNo?: number; pageSize?: number; keyword?: string },
    options?: RequestOptions
  ) => apiGet<PageResult<LexicalListItemVO>>(`/lexical-lists/${lexicalListId}/items`, { ...options, params }),
  reorderItems: (lexicalListId: number, payload: ReorderLexicalListItemsRequest) =>
    apiPut<LexicalListDetailVO>(`/lexical-lists/${lexicalListId}/items/reorder`, payload),
};

export const adminService = {
  listUsers: (
    params: { pageNo?: number; pageSize?: number; keyword?: string; role?: string; enabled?: boolean; invitationStatus?: string; profileLinkStatus?: string },
    options?: RequestOptions
  ) => apiGet<PageResult<UserSummaryVO>>('/admin/users', { ...options, params }),
  createUser: (payload: AdminUserCreateRequest) => apiPost<AdminUserProvisionResultVO>('/admin/users', payload),
  updateUserAccess: (userId: number, payload: AdminUserAccessUpdateRequest) =>
    apiPut<UserSummaryVO>(`/admin/users/${userId}/access`, payload),
  createInviteLink: (userId: number) => apiPost<AccountActionLinkVO>(`/admin/users/${userId}/invite-link`),
  createPasswordResetLink: (userId: number) => apiPost<AccountActionLinkVO>(`/admin/users/${userId}/password-reset-link`),
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

export { teacherWorkspaceService } from './services/teacherWorkspace';
