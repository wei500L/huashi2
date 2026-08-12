import type { AxiosRequestConfig } from 'axios';
import { apiDelete, apiDownload, apiGet, apiPatch, apiPost, apiPostKeepalive, apiPut, apiUpload } from './api';
import type {
  AddLexicalListItemsRequest,
  AddLexicalListItemsResultVO,
  AiAsyncJobSubmitVO,
  AiAsyncJobVO,
  AiGuidanceResponseVO,
  ClassAnalyticsOverviewVO,
  ClassCompletionRateVO,
  CreateLexicalListRequest,
  CsvImportResultVO,
  CsvImportTemplateVO,
  CurrentUserVO,
  CreateDiagnosisSessionRequest,
  DiagnosisHistorySummaryVO,
  DiagnosisResultDetailVO,
  DiagnosisSessionCreatedVO,
  DiagnosisSessionHeartbeatVO,
  DiagnosisSessionProgressVO,
  DiagnosisTemplateDeleteResultVO,
  DiagnosisTemplateDetailVO,
  DiagnosisTemplateDraftDetailVO,
  DiagnosisTemplateDraftSaveRequest,
  DiagnosisTemplateDraftSummaryVO,
  DiagnosisTemplateDraftValidationResponseVO,
  DiagnosisTemplateShareUpdateRequest,
  DiagnosisTemplateSummaryVO,
  LexicalListDetailVO,
  LexicalListItemVO,
  LexicalListSummaryVO,
  LexicalPairDetailVO,
  LexicalPairSuggestionVO,
  LexicalImportBatchCreatedVO,
  LexicalImportBatchDetailVO,
  LexicalImportBatchSummaryVO,
  LexicalImportRowUpdateRequest,
  LexicalImportRowVO,
  LexicalPairOverviewVO,
  LexicalPairSummaryVO,
  LexicalPairUpsertRequest,
  LexicalRagAnswerVO,
  LexicalRagConversationDetailVO,
  LexicalRagConversationSummaryVO,
  LexicalRagQueryRequest,
  LoginRequest,
  LoginResponse,
  RegisterStudentRequest,
  NotificationItemVO,
  NotificationUnreadCountVO,
  PageResult,
  PracticeBankVO,
  PracticeHistoryVO,
  PracticeProgressVO,
  PracticeQuestionTutorRequest,
  PracticeQuestionTutorVO,
  PracticeResultVO,
  PracticeSessionCreatedVO,
  PracticeSessionDetailVO,
  PracticeSessionPageQuery,
  PracticeSpellingCheckRequest,
  PracticeSpellingCheckVO,
  PracticeTutoringRequest,
  RecommendedTrainingPlanVO,
  ReorderLexicalListItemsRequest,
  ResolveStudentRegistrationContextRequest,
  ReviewScheduleItemVO,
  SessionOverviewVO,
  StudentAnalyticsOverviewVO,
  StudentLearningGoalVO,
  StudentProfileSummaryVO,
  TeacherInterventionSummaryVO,
  TeacherClassDetailVO,
  TeacherClassInviteCodeVO,
  TeacherClassStudentBatchRequest,
  TeacherClassStudentCandidateVO,
  TeacherClassUpsertRequest,
  TeacherInterventionUpdateRequest,
  TeacherStudentDetailVO,
  TeachingClassSummaryVO,
  TrainingHistorySummaryVO,
  TrainingAnswerSubmissionVO,
  TrainingNextItemVO,
  TrainingSessionCreatedVO,
  TrainingSessionHeartbeatVO,
  TrainingSessionProgressVO,
  TrainingSessionSummaryVO,
  UpdateLexicalListRequest,
  UpdateStudentLearningGoalRequest,
  UpdateStudentProfileRequest,
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
  AdminDashboardVO,
  AdminAiConfigSaveRequest,
  AdminAiRuntimeSyncRequest,
  AdminAuditLogItemVO,
  AdminUserBatchRequest,
  AdminUserBatchResultVO,
  AdminUserAccessUpdateRequest,
  AdminUserCreateRequest,
  AdminAiConfigViewVO,
  AdminAiConfigDriftVO,
  AdminAiEmbeddingProbeVO,
  AdminAiRerankProbeVO,
  AdminUserProvisionResultVO,
  AccountActionLinkVO,
  AccountActionPreviewVO,
  AssessmentAttemptDetailVO,
  AssessmentAttemptHeartbeatVO,
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
  PublicAssessmentAttemptVO,
  PublicAssessmentQrEntryRequest,
  PublicAssessmentReleaseSummaryVO,
  PublicAssessmentMetadataVO,
  PublicAssessmentProgressVO,
  PublicAssessmentResultVO,
  PublicAssessmentSaveRequest,
  PublicAssessmentSessionVO,
  PublicAssessmentSubmitRequest,
  PublicAssessmentSubmitVO,
  PublicAssessmentTimingRequest,
  PublicAssessmentVerifyRequest,
  SpellingAttemptRequest,
  SpellingAttemptVO,
  ParticipationCodeBatchCreatedVO,
  ParticipationCodeItemVO,
  ParticipationCodeRevokeResultVO,
  QuestionBankImportPreflightVO,
  QuestionBankImportReviewVO,
  ContentReviewResolutionRequest,
  QuestionBankImportCommitRequest,
  QuestionBankImportCommitVO,
  QuestionBankItemSummaryVO,
  AssessmentPaperPurpose,
  QuestionBankListParams,
  ChangePasswordRequest,
  CompleteAccountActionRequest,
  AdminOutboxRecordVO,
  ExplainDiagnosisRequest,
  AiGatewayHealthResponse,
  AiOpsConfigValidationResponse,
  RagReindexJobResponse,
  RagReindexRequest,
  RagReindexResponse,
  RefreshTokenRequest,
  RecommendTrainingRequest,
  SaveAssessmentResponsesRequest,
  SubmitAssessmentAttemptRequest,
  SaveDiagnosisProgressRequest,
  SaveTrainingProgressRequest,
  StartTrainingSessionRequest,
  StudentAssessmentHistorySummaryVO,
  StudentRegistrationContextVO,
  StudentAssessmentSummaryVO,
  SubmitDiagnosisAnswerRequest,
  SubmitTrainingAnswerRequest,
  TeacherAssessmentAttemptResultVO,
  TeacherInterventionSuggestRequest,
  ResearchAnalyticsFilter,
  ResearchReleaseListItemVO,
  ResearchPublishOverviewVO,
  ResearchAttemptSummaryVO,
  TeacherResearchAttemptDetailVO,
  ResearchQuestionStatisticsVO,
  ResearchOptionStatisticsVO,
  ResearchDimensionStatisticsVO,
  ResearchReactionTimeStatisticsVO,
  ResearchQualityStatisticVO,
  ResearchTextThemeStatisticsVO,
  ResearchAiReportVO,
  ResearchExportJobVO,
  ResearchAttachmentVO,
  ResearchFileInitiateVO,
} from './contracts';

type RequestOptions = Pick<AxiosRequestConfig, 'signal' | 'timeout'>;

export const authService = {
  login: (payload: LoginRequest) => apiPost<LoginResponse>('/auth/login', payload),
  registerStudent: (payload: RegisterStudentRequest) =>
    apiPost<LoginResponse>('/auth/register', payload),
  resolveRegistrationContext: (payload: ResolveStudentRegistrationContextRequest, options?: RequestOptions) =>
    apiPost<StudentRegistrationContextVO>('/auth/register/context', payload, options),
  refresh: (refreshToken: string) => {
    const payload: RefreshTokenRequest = { refreshToken };
    return apiPost<LoginResponse>('/auth/refresh', payload);
  },
  logout: () => apiPost<void>('/auth/logout'),
  changePassword: (payload: ChangePasswordRequest) => apiPost<void>('/auth/change-password', payload),
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
  updateProfile: (payload: UpdateStudentProfileRequest) => apiPut<CurrentUserVO['studentProfile']>('/student/profile', payload),
  getLearningGoals: (options?: RequestOptions) => apiGet<StudentLearningGoalVO>('/student/profile/goals', options),
  updateLearningGoals: (payload: UpdateStudentLearningGoalRequest) =>
    apiPut<StudentLearningGoalVO>('/student/profile/goals', payload),
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
  listMarketTemplates: (params: { pageNo?: number; pageSize?: number; keyword?: string }, options?: RequestOptions) =>
    apiGet<PageResult<DiagnosisTemplateSummaryVO>>('/teacher/diagnosis-templates/market', { ...options, params }),
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
  updateTeacherTemplateSharing: (templateId: number, payload: DiagnosisTemplateShareUpdateRequest) =>
    apiPut<DiagnosisTemplateDetailVO>(`/teacher/diagnosis-templates/${templateId}/sharing`, payload),
  deleteTeacherTemplate: (templateId: number) =>
    apiDelete<DiagnosisTemplateDeleteResultVO>(`/teacher/diagnosis-templates/${templateId}`),
};

export const diagnosisSessionService = {
  listHistory: (params: { pageNo?: number; pageSize?: number; status?: string; templateId?: number }, options?: RequestOptions) =>
    apiGet<PageResult<DiagnosisHistorySummaryVO>>('/diagnosis/sessions', { ...options, params }),
  create: (payload: CreateDiagnosisSessionRequest) => apiPost<DiagnosisSessionCreatedVO>('/diagnosis/sessions', payload),
  getNextItem: (sessionId: number, options?: RequestOptions) => apiGet<DiagnosisNextItemVO>(`/diagnosis/sessions/${sessionId}/next-item`, options),
  submitAnswer: (sessionId: number, payload: SubmitDiagnosisAnswerRequest) =>
    apiPost<DiagnosisSessionProgressVO>(`/diagnosis/sessions/${sessionId}/answers`, payload),
  saveProgress: (sessionId: number, progressSnapshot: Record<string, unknown>) => {
    const payload: SaveDiagnosisProgressRequest = { progressSnapshot };
    return apiPost<DiagnosisSessionProgressVO>(`/diagnosis/sessions/${sessionId}/progress`, payload);
  },
  heartbeat: (sessionId: number) => apiPost<DiagnosisSessionHeartbeatVO>(`/diagnosis/sessions/${sessionId}/heartbeat`),
  saveProgressKeepalive: (sessionId: number, progressSnapshot: Record<string, unknown>) => {
    const payload: SaveDiagnosisProgressRequest = { progressSnapshot };
    return apiPostKeepalive<DiagnosisSessionProgressVO>(`/diagnosis/sessions/${sessionId}/progress`, payload);
  },
  complete: (sessionId: number) => apiPost<DiagnosisSessionProgressVO>(`/diagnosis/sessions/${sessionId}/complete`),
  abandon: (sessionId: number) => apiPost<DiagnosisSessionProgressVO>(`/diagnosis/sessions/${sessionId}/abandon`),
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
  startSession: (payload: StartTrainingSessionRequest) => apiPost<TrainingSessionCreatedVO>('/training/sessions', payload),
  getNextItem: (sessionId: number, options?: RequestOptions) => apiGet<TrainingNextItemVO>(`/training/sessions/${sessionId}/next-item`, options),
  saveProgress: (sessionId: number, progressSnapshot: Record<string, unknown>) => {
    const payload: SaveTrainingProgressRequest = { progressSnapshot };
    return apiPost<TrainingSessionProgressVO>(`/training/sessions/${sessionId}/progress`, payload);
  },
  heartbeat: (sessionId: number) => apiPost<TrainingSessionHeartbeatVO>(`/training/sessions/${sessionId}/heartbeat`),
  saveProgressKeepalive: (sessionId: number, progressSnapshot: Record<string, unknown>) => {
    const payload: SaveTrainingProgressRequest = { progressSnapshot };
    return apiPostKeepalive<TrainingSessionProgressVO>(`/training/sessions/${sessionId}/progress`, payload);
  },
  submitAnswer: (sessionId: number, payload: SubmitTrainingAnswerRequest) =>
    apiPost<TrainingAnswerSubmissionVO>(`/training/sessions/${sessionId}/answers`, payload),
  complete: (sessionId: number) => apiPost<TrainingSessionProgressVO>(`/training/sessions/${sessionId}/complete`),
  abandon: (sessionId: number) => apiPost<TrainingSessionProgressVO>(`/training/sessions/${sessionId}/abandon`),
  getSummary: (sessionId: number, options?: RequestOptions) => apiGet<TrainingSessionSummaryVO>(`/training/sessions/${sessionId}/summary`, options),
};

export const assessmentService = {
  listQuestionBankItems: (params: QuestionBankListParams, options?: RequestOptions) =>
    apiGet<PageResult<QuestionBankItemSummaryVO>>('/teacher/assessments/question-bank', { ...options, params }),
  downloadQuestionBankImportTemplate: (options?: RequestOptions) =>
    apiDownload('/teacher/assessments/question-bank/import-template.xlsx', options),
  preflightQuestionBankImport: (file: File, options?: RequestOptions) => {
    const formData = new FormData();
    formData.append('file', file);
    return apiUpload<QuestionBankImportPreflightVO>('/teacher/assessments/question-bank/imports/preflight', formData, options);
  },
  downloadQuestionBankImportJsonTemplate: (options?: RequestOptions) =>
    apiDownload('/teacher/assessments/question-bank/import-template.json', options),
  commitQuestionBankImport: (importId: number | string, payload: QuestionBankImportCommitRequest) =>
    apiPost<QuestionBankImportCommitVO>(`/teacher/assessments/question-bank/imports/${importId}/commit`, payload),
  getQuestionBankImportReadiness: (importId: number | string, options?: RequestOptions) =>
    apiGet<QuestionBankImportReviewVO>(`/teacher/assessments/question-bank/imports/${importId}/publish-readiness`, options),
  confirmQuestionBankImportIssues: (importId: number | string, issueIds: Array<number | string>, resolutionNote: string) =>
    apiPost<QuestionBankImportReviewVO>(`/teacher/assessments/question-bank/imports/${importId}/confirm`, {
      issueIds,
      resolutionNote,
    }),
  resolveQuestionBankImportIssue: (importId: number | string, issueId: number | string, payload: ContentReviewResolutionRequest) =>
    apiPost<void>(`/teacher/assessments/question-bank/imports/${importId}/review-issues/${issueId}`, payload),
  approveQuestionBankImport: (importId: number | string) =>
    apiPost<QuestionBankImportCommitVO>(`/teacher/assessments/question-bank/imports/${importId}/approve`),
  listTeacherPapers: (params?: { purpose?: AssessmentPaperPurpose }, options?: RequestOptions) =>
    apiGet<AssessmentPaperSummaryVO[]>('/teacher/assessments/papers', { ...options, params }),
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
  listPublicReleases: (options?: RequestOptions) =>
    apiGet<PublicAssessmentReleaseSummaryVO[]>('/teacher/assessments/public-releases', options),
  listParticipationCodes: (
    publishId: number,
    params: { pageNo?: number; pageSize?: number; status?: string; batchId?: string },
    options?: RequestOptions
  ) => apiGet<PageResult<ParticipationCodeItemVO>>(
    `/teacher/assessments/publishes/${publishId}/participation-codes`,
    { ...options, params }
  ),
  createParticipationCodeBatch: (publishId: number, count: number) =>
    apiPost<ParticipationCodeBatchCreatedVO>(
      `/teacher/assessments/publishes/${publishId}/participation-code-batches`,
      { count }
    ),
  revokeParticipationCode: (publishId: number, codeId: number) =>
    apiPost<ParticipationCodeRevokeResultVO>(
      `/teacher/assessments/publishes/${publishId}/participation-codes/${codeId}/revoke`
    ),
  revokeParticipationCodeBatch: (publishId: number, batchId: string) =>
    apiPost<ParticipationCodeRevokeResultVO>(
      `/teacher/assessments/publishes/${publishId}/participation-code-batches/${encodeURIComponent(batchId)}/revoke-unused`
    ),
  updatePublicRelease: (publishId: number, qrEntryEnabled: boolean) =>
    apiPatch<PublicAssessmentReleaseSummaryVO>(
      `/teacher/assessments/publishes/${publishId}/public-release`,
      { qrEntryEnabled }
    ),
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
  getStudentAttemptHeartbeat: (attemptId: number, options?: RequestOptions) =>
    apiGet<AssessmentAttemptHeartbeatVO>(`/student/assessments/attempts/${attemptId}/heartbeat`, options),
  saveStudentResponses: (attemptId: number, payload: SaveAssessmentResponsesRequest) =>
    apiPost<AssessmentAttemptProgressVO>(`/student/assessments/attempts/${attemptId}/responses`, payload),
  saveStudentResponsesKeepalive: (attemptId: number, payload: SaveAssessmentResponsesRequest) =>
    apiPostKeepalive<AssessmentAttemptProgressVO>(`/student/assessments/attempts/${attemptId}/responses`, payload),
  submitStudentAttempt: (attemptId: number, payload: SubmitAssessmentAttemptRequest) =>
    apiPost<AssessmentAttemptSubmitVO>(`/student/assessments/attempts/${attemptId}/submit`, payload),
  getStudentAttemptResult: (attemptId: number, options?: RequestOptions) =>
    apiGet<AssessmentAttemptResultVO>(`/student/assessments/attempts/${attemptId}/result`, options),
};

const researchFilterParams = (filters?: ResearchAnalyticsFilter) => ({
  status: filters?.status || undefined,
  entryType: filters?.entryType || undefined,
  qualityFlag: filters?.qualityFlag || undefined,
  aiStatus: filters?.aiStatus || undefined,
  submittedFrom: filters?.submittedFrom || undefined,
  submittedTo: filters?.submittedTo || undefined,
  keyword: filters?.keyword || undefined,
});

export const researchAnalyticsService = {
  listReleases: (options?: RequestOptions) =>
    apiGet<ResearchReleaseListItemVO[]>('/teacher/research/releases', options),
  getOverview: (publishId: number, filters?: ResearchAnalyticsFilter, options?: RequestOptions) =>
    apiGet<ResearchPublishOverviewVO>(`/teacher/research/publishes/${publishId}/overview`, {
      ...options,
      params: researchFilterParams(filters),
    }),
  listAttempts: (
    publishId: number,
    params: ResearchAnalyticsFilter & { pageNo?: number; pageSize?: number; sort?: string },
    options?: RequestOptions
  ) => apiGet<PageResult<ResearchAttemptSummaryVO>>(`/teacher/research/publishes/${publishId}/attempts`, {
    ...options,
    params: {
      ...researchFilterParams(params),
      pageNo: params.pageNo,
      pageSize: params.pageSize,
      sort: params.sort,
    },
  }),
  getAttemptDetail: (attemptId: number, options?: RequestOptions) =>
    apiGet<TeacherResearchAttemptDetailVO>(`/teacher/research/attempts/${attemptId}`, options),
  getQuestionStats: (publishId: number, filters?: ResearchAnalyticsFilter, options?: RequestOptions) =>
    apiGet<ResearchQuestionStatisticsVO>(`/teacher/research/publishes/${publishId}/statistics/questions`, {
      ...options,
      params: researchFilterParams(filters),
    }),
  getOptionStats: (publishId: number, filters?: ResearchAnalyticsFilter, options?: RequestOptions) =>
    apiGet<ResearchOptionStatisticsVO>(`/teacher/research/publishes/${publishId}/statistics/options`, {
      ...options,
      params: researchFilterParams(filters),
    }),
  getDimensionStats: (publishId: number, filters?: ResearchAnalyticsFilter, options?: RequestOptions) =>
    apiGet<ResearchDimensionStatisticsVO>(`/teacher/research/publishes/${publishId}/statistics/dimensions`, {
      ...options,
      params: researchFilterParams(filters),
    }),
  getReactionStats: (publishId: number, filters?: ResearchAnalyticsFilter, options?: RequestOptions) =>
    apiGet<ResearchReactionTimeStatisticsVO>(`/teacher/research/publishes/${publishId}/statistics/reaction-times`, {
      ...options,
      params: researchFilterParams(filters),
    }),
  getQualityStats: (publishId: number, filters?: ResearchAnalyticsFilter, options?: RequestOptions) =>
    apiGet<ResearchQualityStatisticVO>(`/teacher/research/publishes/${publishId}/statistics/quality`, {
      ...options,
      params: researchFilterParams(filters),
    }),
  getTextThemeStats: (publishId: number, filters?: ResearchAnalyticsFilter, options?: RequestOptions) =>
    apiGet<ResearchTextThemeStatisticsVO>(`/teacher/research/publishes/${publishId}/statistics/text-themes`, {
      ...options,
      params: researchFilterParams(filters),
    }),
  createExport: (
    publishId: number,
    payload: ResearchAnalyticsFilter & {
      format: 'CSV' | 'XLSX';
      scope?: string;
      includeSensitiveFields?: boolean;
      includeAttachmentManifest?: boolean;
    }
  ) => apiPost<ResearchExportJobVO>(`/teacher/research/publishes/${publishId}/exports`, payload),
  getExport: (jobId: number, options?: RequestOptions) =>
    apiGet<ResearchExportJobVO>(`/teacher/research/exports/${jobId}`, options),
  downloadExport: (jobId: number, options?: RequestOptions) =>
    apiDownload(`/teacher/research/exports/${jobId}/download`, options),
  getFileMetadata: (fileId: number, options?: RequestOptions) =>
    apiGet<ResearchAttachmentVO>(`/teacher/research/files/${fileId}/metadata`, options),
  downloadFile: (fileId: number, options?: RequestOptions) =>
    apiDownload(`/teacher/research/files/${fileId}/download`, options),
  createAiReport: (publishId: number, filters?: ResearchAnalyticsFilter) =>
    apiPost<ResearchAiReportVO>(`/teacher/research/publishes/${publishId}/ai-reports`, {}, {
      params: researchFilterParams(filters),
    }),
  getLatestAiReport: (publishId: number, options?: RequestOptions) =>
    apiGet<ResearchAiReportVO | null>(`/teacher/research/publishes/${publishId}/ai-reports/latest`, options),
  getAiReport: (reportId: number, options?: RequestOptions) =>
    apiGet<ResearchAiReportVO>(`/teacher/research/ai-reports/${reportId}`, options),
  retryAiReport: (reportId: number) =>
    apiPost<ResearchAiReportVO>(`/teacher/research/ai-reports/${reportId}/retry`),
};

const publicAssessmentPath = (releaseCode: string) =>
  `/public/assessments/${encodeURIComponent(releaseCode.trim())}`;
const publicAssessmentOptions = (options?: RequestOptions): AxiosRequestConfig => ({
  ...options,
  withCredentials: true,
});

export const publicAssessmentService = {
  getMetadata: (releaseCode: string, options?: RequestOptions) =>
    apiGet<PublicAssessmentMetadataVO>(publicAssessmentPath(releaseCode), publicAssessmentOptions(options)),
  verifyCode: (releaseCode: string, payload: PublicAssessmentVerifyRequest) =>
    apiPost<PublicAssessmentSessionVO>(`${publicAssessmentPath(releaseCode)}/verify`, payload, publicAssessmentOptions()),
  enterByQr: (releaseCode: string, payload: PublicAssessmentQrEntryRequest) =>
    apiPost<PublicAssessmentSessionVO>(`${publicAssessmentPath(releaseCode)}/qr-entry`, payload, publicAssessmentOptions()),
  getAttempt: (releaseCode: string, options?: RequestOptions) =>
    apiGet<PublicAssessmentAttemptVO>(`${publicAssessmentPath(releaseCode)}/attempt`, publicAssessmentOptions(options)),
  saveResponses: (releaseCode: string, payload: PublicAssessmentSaveRequest) =>
    apiPost<PublicAssessmentProgressVO>(`${publicAssessmentPath(releaseCode)}/responses`, payload, publicAssessmentOptions()),
  recordTiming: (releaseCode: string, payload: PublicAssessmentTimingRequest) =>
    apiPost<void>(`${publicAssessmentPath(releaseCode)}/timing`, payload, publicAssessmentOptions()),
  attemptSpelling: (releaseCode: string, payload: SpellingAttemptRequest) =>
    apiPost<SpellingAttemptVO>(`${publicAssessmentPath(releaseCode)}/spelling-attempt`, payload, publicAssessmentOptions()),
  submit: (releaseCode: string, payload: PublicAssessmentSubmitRequest) =>
    apiPost<PublicAssessmentSubmitVO>(`${publicAssessmentPath(releaseCode)}/submit`, payload, publicAssessmentOptions()),
  getResult: (releaseCode: string, options?: RequestOptions) =>
    apiGet<PublicAssessmentResultVO>(`${publicAssessmentPath(releaseCode)}/result`, publicAssessmentOptions(options)),
  initiateFile: (releaseCode: string, payload: { questionOrder: number; fileName: string; contentType?: string; sizeBytes: number }) =>
    apiPost<ResearchFileInitiateVO>(`${publicAssessmentPath(releaseCode)}/files/initiate`, payload, publicAssessmentOptions()),
  uploadFileContent: (releaseCode: string, uploadToken: string, file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    return apiUpload<ResearchAttachmentVO>(
      `${publicAssessmentPath(releaseCode)}/files/${encodeURIComponent(uploadToken)}/content`,
      formData,
      publicAssessmentOptions()
    );
  },
  deleteFile: (releaseCode: string, uploadToken: string) =>
    apiDelete<void>(`${publicAssessmentPath(releaseCode)}/files/${encodeURIComponent(uploadToken)}`, publicAssessmentOptions()),
};

export const practiceService = {
  listBanks: (options?: RequestOptions) => apiGet<PracticeBankVO[]>('/student/practice/banks', options),
  startSession: (payload: { bankCode: string; sectionCode?: string | null; targetWords?: string[] }) =>
    apiPost<PracticeSessionCreatedVO>('/student/practice/sessions', payload),
  getSession: (sessionId: number, options?: RequestOptions) =>
    apiGet<PracticeSessionDetailVO>(`/student/practice/sessions/${sessionId}`, options),
  saveDraft: (sessionId: number, payload: { answers: { questionOrder: number; response: string[] }[] }) =>
    apiPost<PracticeProgressVO>(`/student/practice/sessions/${sessionId}/draft`, payload),
  checkSpelling: (sessionId: number, payload: PracticeSpellingCheckRequest) =>
    apiPost<PracticeSpellingCheckVO>(`/student/practice/sessions/${sessionId}/answers/spelling-check`, payload),
  complete: (sessionId: number, payload: { answers: { questionOrder: number; response: string[] }[] }) =>
    apiPost<PracticeProgressVO>(`/student/practice/sessions/${sessionId}/complete`, payload),
  abandon: (sessionId: number) => apiPost<PracticeProgressVO>(`/student/practice/sessions/${sessionId}/abandon`),
  getResult: (sessionId: number, options?: RequestOptions) =>
    apiGet<PracticeResultVO>(`/student/practice/sessions/${sessionId}/result`, options),
  listHistory: (params: PracticeSessionPageQuery, options?: RequestOptions) =>
    apiGet<PageResult<PracticeHistoryVO>>('/student/practice/history', { ...options, params }),
};

async function pollAiJob<T>(
  jobId: string,
  options?: RequestOptions,
  intervalMs = 2000,
  maxWaitMs = 180_000
): Promise<T> {
  const started = Date.now();
  while (Date.now() - started < maxWaitMs) {
    if (options?.signal?.aborted) {
      throw new DOMException('The operation was aborted.', 'AbortError');
    }
    const job = await apiGet<AiAsyncJobVO>(`/ai/jobs/${jobId}`, options);
    if (job.status === 'SUCCEEDED') {
      return job.result as T;
    }
    if (job.status === 'FAILED') {
      throw new Error(job.errorMessage || 'AI async job failed');
    }
    await new Promise<void>((resolve) => {
      setTimeout(resolve, intervalMs);
    });
  }
  throw new Error('AI async job timed out');
}

export const aiService = {
  explainDiagnosis: (diagnosisSummaryId?: number | null, options?: RequestOptions) => {
    const payload: ExplainDiagnosisRequest | undefined = diagnosisSummaryId ? { diagnosisSummaryId } : undefined;
    return apiPost<AiGuidanceResponseVO>('/ai/explain-diagnosis', payload, options);
  },
  recommendTraining: (diagnosisSummaryId?: number | null, options?: RequestOptions) => {
    const payload: RecommendTrainingRequest | undefined = diagnosisSummaryId ? { diagnosisSummaryId } : undefined;
    return apiPost<AiGuidanceResponseVO>('/ai/recommend-training', payload, options);
  },
  explainDiagnosisAsync: async (diagnosisSummaryId?: number | null, options?: RequestOptions) => {
    const payload: ExplainDiagnosisRequest | undefined = diagnosisSummaryId ? { diagnosisSummaryId } : undefined;
    const submitted = await apiPost<AiAsyncJobSubmitVO>('/ai/explain-diagnosis/async', payload, options);
    return pollAiJob<AiGuidanceResponseVO>(submitted.jobId, options);
  },
  recommendTrainingAsync: async (diagnosisSummaryId?: number | null, options?: RequestOptions) => {
    const payload: RecommendTrainingRequest | undefined = diagnosisSummaryId ? { diagnosisSummaryId } : undefined;
    const submitted = await apiPost<AiAsyncJobSubmitVO>('/ai/recommend-training/async', payload, options);
    return pollAiJob<AiGuidanceResponseVO>(submitted.jobId, options);
  },
  getAiJob: (jobId: string, options?: RequestOptions) => apiGet<AiAsyncJobVO>(`/ai/jobs/${jobId}`, options),
  queryLexicalRag: (payload: LexicalRagQueryRequest) => apiPost<LexicalRagAnswerVO>('/ai/lexical-rag/query', payload),
  queryLexicalRagAsync: async (payload: LexicalRagQueryRequest, options?: RequestOptions) => {
    const submitted = await apiPost<AiAsyncJobSubmitVO>('/ai/lexical-rag/query/async', payload, options);
    return pollAiJob<LexicalRagAnswerVO>(submitted.jobId, options);
  },
  practiceTutoringAsync: async (practiceSessionId: number, options?: RequestOptions) => {
    const payload: PracticeTutoringRequest = { practiceSessionId };
    const submitted = await apiPost<AiAsyncJobSubmitVO>('/ai/practice-tutoring/async', payload, options);
    return pollAiJob<AiGuidanceResponseVO>(submitted.jobId, options);
  },
  explainPracticeQuestion: (payload: PracticeQuestionTutorRequest, options?: RequestOptions) =>
    apiPost<PracticeQuestionTutorVO>('/ai/practice-question-tutor', payload, options),
  listLexicalRagConversations: (params?: { pageNo?: number; pageSize?: number }, options?: RequestOptions) =>
    apiGet<PageResult<LexicalRagConversationSummaryVO>>('/ai/lexical-rag/conversations', { ...options, params }),
  getLexicalRagConversation: (conversationId: string, options?: RequestOptions) =>
    apiGet<LexicalRagConversationDetailVO>(`/ai/lexical-rag/conversations/${conversationId}`, options),
  suggestTeacherIntervention: (payload: TeacherInterventionSuggestRequest) =>
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
  generateInviteCode: () =>
    apiPost<TeacherClassInviteCodeVO>('/teacher/classes/invite-code'),
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
  suggest: (params: { keyword: string; limit?: number; active?: boolean }, options?: RequestOptions) =>
    apiGet<LexicalPairSuggestionVO[]>('/lexical-pairs/suggestions', { ...options, params }),
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
  reindexImportBatch: (batchId: number) =>
    apiPost<RagReindexResponse>(`/lexical-pairs/import-batches/${batchId}/reindex`),
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
  getDashboard: (options?: RequestOptions) => apiGet<AdminDashboardVO>('/admin/dashboard', options),
  listUsers: (
    params: { pageNo?: number; pageSize?: number; keyword?: string; role?: string; enabled?: boolean; invitationStatus?: string; profileLinkStatus?: string },
    options?: RequestOptions
  ) => apiGet<PageResult<UserSummaryVO>>('/admin/users', { ...options, params }),
  listAuditLogs: (
    params: { pageNo?: number; pageSize?: number; startAt?: string; endAt?: string; actionType?: string; userKeyword?: string },
    options?: RequestOptions
  ) => apiGet<PageResult<AdminAuditLogItemVO>>('/admin/audit-logs', { ...options, params }),
  createUser: (payload: AdminUserCreateRequest) => apiPost<AdminUserProvisionResultVO>('/admin/users', payload),
  batchUsers: (payload: AdminUserBatchRequest) => apiPost<AdminUserBatchResultVO>('/admin/users/batch', payload),
  updateUserAccess: (userId: number, payload: AdminUserAccessUpdateRequest) =>
    apiPut<UserSummaryVO>(`/admin/users/${userId}/access`, payload),
  createInviteLink: (userId: number) => apiPost<AccountActionLinkVO>(`/admin/users/${userId}/invite-link`),
  createPasswordResetLink: (userId: number) => apiPost<AccountActionLinkVO>(`/admin/users/${userId}/password-reset-link`),
  getAiConfig: (options?: RequestOptions) => apiGet<AdminAiConfigViewVO>('/admin/ai-config', options),
  validateAiConfig: (payload: AdminAiConfigSaveRequest) => apiPost<AiOpsConfigValidationResponse>('/admin/ai-config/validate', payload),
  saveAiConfig: (payload: AdminAiConfigSaveRequest) => apiPut<AdminAiConfigViewVO>('/admin/ai-config', payload),
  syncAiRuntime: (payload: AdminAiRuntimeSyncRequest) => apiPost<AdminAiConfigViewVO>('/admin/ai-config/runtime/sync', payload),
  getAiDrift: (options?: RequestOptions) => apiGet<AdminAiConfigDriftVO>('/admin/ai-config/drift', options),
  getOutboxRecords: (status?: string, limit?: number, options?: RequestOptions) =>
    apiGet<AdminOutboxRecordVO[]>('/admin/ai-config/outbox', { ...options, params: { status, limit } }),
  replayOutboxRecord: (id: number) => apiPost<AdminOutboxRecordVO>(`/admin/ai-config/outbox/${id}/replay`),
  getAiHealth: (options?: RequestOptions) => apiGet<AiGatewayHealthResponse>('/admin/ai-config/health', options),
  probeAiEmbedding: (payload: AdminAiConfigSaveRequest) => apiPost<AdminAiEmbeddingProbeVO>('/admin/ai-config/probes/embedding', payload),
  probeAiRerank: (payload: AdminAiConfigSaveRequest) => apiPost<AdminAiRerankProbeVO>('/admin/ai-config/probes/rerank', payload),
  triggerRagReindex: (payload: RagReindexRequest) => apiPost<RagReindexResponse>('/admin/ai-config/reindex', payload),
  getRagReindexJob: (jobId: string, options?: RequestOptions) => apiGet<RagReindexJobResponse>(`/admin/ai-config/reindex-jobs/${jobId}`, options),
};

export { teacherWorkspaceService } from './services/teacherWorkspace';
