export type Role = 'STUDENT' | 'TEACHER' | 'ADMIN';
export type Capability = 'STUDENT_WORKSPACE' | 'TEACHING_WORKSPACE' | 'ADMIN_CONSOLE';

export interface ApiResponse<T> {
  success: boolean;
  code: string;
  message: string;
  data: T;
  timestamp: string;
  traceId?: string | null;
}

export interface PageResult<T> {
  total: number;
  pageNo: number;
  pageSize: number;
  records: T[];
}

export interface StudentProfileVO {
  studentNo: string;
  gradeName: string;
  englishLevel: string;
  frenchLevel: string;
  courseStage: string;
  compositeScore: number;
  dailyTrainingTarget?: number | null;
  weeklyAccuracyTarget?: number | null;
}

export interface TeacherProfileVO {
  employeeNo: string;
  department: string;
  title: string;
}

export interface CurrentUserVO {
  id: number;
  username: string;
  email: string;
  displayName: string;
  lastLoginAt?: string | null;
  primaryRole: Role;
  roles: Role[];
  capabilities: Capability[];
  studentProfile?: StudentProfileVO | null;
  teacherProfile?: TeacherProfileVO | null;
}

export interface LoginResponse {
  accessToken: string;
  accessTokenExpiresAt: string;
  refreshToken: string;
  refreshTokenExpiresAt: string;
  userInfo: CurrentUserVO;
}

export interface RegisterStudentRequest {
  username: string;
  email: string;
  displayName: string;
  password: string;
  registrationToken: string;
  englishLevel: string;
  frenchLevel: string;
  courseStage: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface ResolveStudentRegistrationContextRequest {
  classCode: string;
}

export interface StudentRegistrationContextVO {
  className: string;
  gradeName: string;
  registrationToken: string;
  registrationTokenExpiresAt: string;
}

export interface NotificationItemVO {
  id: number;
  category: string;
  level: string;
  title: string;
  content: string;
  actionUrl?: string | null;
  actionLabel?: string | null;
  status: 'UNREAD' | 'READ' | string;
  payloadJson?: string | null;
  createdAt: string;
  readAt?: string | null;
}

export interface NotificationUnreadCountVO {
  unreadCount: number;
}

export interface NotificationSocketMessage {
  type: 'NOTIFICATION_CREATED' | string;
  notification?: NotificationItemVO | null;
  unreadCount: number;
  sentAt: string;
}

export interface AnalyticsCardVO {
  key: string;
  label: string;
  unit: string;
  value: number;
}

export interface AnalyticsRadarMetricVO {
  key: string;
  label: string;
  value: number;
  max: number;
}

export interface AnalyticsContextPerformanceVO {
  contextSupportLevel: string;
  accuracy: number;
  avgReactionTimeMs: number;
  attemptCount: number;
}

export interface StudentRiskPairPayload {
  lexicalPairId: number;
  englishWord: string;
  frenchWord: string;
  lexicalPairType: string;
  riskScore: number;
  attemptCount: number;
  incorrectCount: number;
}

export interface StudentErrorDistributionPayload {
  errorType: string;
  count: number;
  ratio: number;
}

export interface StudentAnalyticsSnapshotPayload {
  studentName: string;
  gradeName: string;
  englishLevel: string;
  frenchLevel: string;
  lastDiagnosisSummaryId?: number | null;
  lastTrainingSessionId?: number | null;
  primaryRiskLevel: string;
  recommendedTrainingMode: string;
  pendingReviewCount: number;
  highRiskPairCount: number;
  recentAccuracy: number;
  recentNegativeTransferRisk: number;
  recentAvgReactionTimeMs: number;
  lastActiveAt?: string | null;
  topRiskPairs: StudentRiskPairPayload[];
  errorDistribution: StudentErrorDistributionPayload[];
  focusTags: string[];
}

export interface StudentAchievementBadgeVO {
  code: string;
  unlocked: boolean;
  progressValue: number;
  targetValue: number;
  awardedAt?: string | null;
}

export interface StudentAchievementWallVO {
  unlockedCount: number;
  totalCount: number;
  badges: StudentAchievementBadgeVO[];
}

export interface StudentLearningGoalVO {
  dailyTrainingTarget?: number | null;
  dailyTrainingCompletedToday: number;
  dailyTrainingRemaining: number;
  weeklyAccuracyTarget?: number | null;
  weeklyAccuracyCurrent: number;
  weeklyAccuracyDelta: number;
  configured: boolean;
  updatedAt?: string | null;
}

export interface UpdateStudentLearningGoalRequest {
  dailyTrainingTarget?: number | null;
  weeklyAccuracyTarget?: number | null;
}

export interface StudentAnalyticsOverviewVO {
  studentUserId: number;
  studentName: string;
  gradeName: string;
  englishLevel: string;
  frenchLevel: string;
  primaryRiskLevel: string;
  recommendedTrainingMode: string;
  cards: AnalyticsCardVO[];
  radar: AnalyticsRadarMetricVO[];
  contextPerformance: AnalyticsContextPerformanceVO[];
  latestSnapshot: StudentAnalyticsSnapshotPayload;
  achievementWall: StudentAchievementWallVO;
  learningGoal: StudentLearningGoalVO;
}

export interface AnalyticsSeriesVO {
  key: string;
  label: string;
  values: number[];
}

export interface AnalyticsTrendVO {
  bucket: string;
  xAxis: string[];
  series: AnalyticsSeriesVO[];
}

export interface AnalyticsHeatmapCellVO {
  xKey: string;
  yKey: string;
  value: number;
  accuracy: number;
  avgReactionTimeMs: number;
}

export interface AnalyticsHeatmapMetaVO {
  range: string;
  bucket: string;
  filters?: Record<string, string>;
}

export interface AnalyticsHeatmapVO {
  xAxis: string[];
  yAxis: string[];
  cells: AnalyticsHeatmapCellVO[];
  meta: AnalyticsHeatmapMetaVO;
}

export interface AnalyticsScatterPointVO {
  lexicalPairId: number;
  label: string;
  lexicalPairType: string;
  accuracy: number;
  avgReactionTimeMs: number;
  attemptCount: number;
  riskScore: number;
}

export interface AnalyticsScatterVO {
  x: string;
  y: string;
  points: AnalyticsScatterPointVO[];
}

export interface AnalyticsRiskPairVO {
  lexicalPairId: number;
  englishWord: string;
  frenchWord: string;
  lexicalPairType: string;
  riskScore: number;
  attemptCount: number;
  incorrectCount: number;
}

export interface AnalyticsErrorDistributionVO {
  key: string;
  label: string;
  count: number;
  ratio: number;
}

export interface StudentAnalyticsDetailVO {
  overview: StudentAnalyticsOverviewVO;
  trend7d: AnalyticsTrendVO;
  trend30d: AnalyticsTrendVO;
  transferHeatmap: AnalyticsHeatmapVO;
  scatter: AnalyticsScatterVO;
  highRiskPairs: AnalyticsRiskPairVO[];
  errorDistribution: AnalyticsErrorDistributionVO[];
}

export interface DiagnosisTemplateSummaryVO {
  id: number;
  templateName: string;
  description?: string | null;
  status: string;
  targetClassId?: number | null;
  targetClassName?: string | null;
  itemCount: number;
  estimatedDurationMinutes: number;
  scoringVersion: string;
  ownerUserId: number;
  updatedAt: string;
}

export interface DiagnosisTemplateStimulus {
  instruction: string;
  contextSentence?: string | null;
  promptText?: string | null;
}

export interface DiagnosisTemplateOption {
  key: string;
  label: string;
  semanticMatch?: boolean | null;
  ignoreContextTrap?: boolean | null;
}

export interface DiagnosisTemplateScoringProfile {
  formulaKey?: string | null;
  pairWeight?: number | null;
  riskAmplifier?: number | null;
  maxReactionTimeMs?: number | null;
}

export interface DiagnosisTemplateStimulusRequest {
  instruction: string;
  contextSentence?: string | null;
  promptText?: string | null;
}

export interface DiagnosisTemplateOptionRequest {
  key: string;
  label: string;
  semanticMatch?: boolean | null;
  ignoreContextTrap?: boolean | null;
}

export interface DiagnosisTemplateScoringProfileRequest {
  formulaKey?: string | null;
  pairWeight?: number | null;
  riskAmplifier?: number | null;
  maxReactionTimeMs?: number | null;
}

export interface DiagnosisTemplateItemRequest {
  lexicalPairId: number;
  taskType: string;
  blockCode: string;
  sortOrder: number;
  contextSupportLevel: string;
  expectedSemanticMatch: boolean;
  stimulus: DiagnosisTemplateStimulusRequest;
  options: DiagnosisTemplateOptionRequest[];
  correctAnswerKey: string;
  scoringProfile?: DiagnosisTemplateScoringProfileRequest | null;
}

export interface DiagnosisTemplateItemVO {
  id?: number | null;
  lexicalPairId: number;
  englishWord?: string | null;
  frenchWord?: string | null;
  chineseGloss?: string | null;
  lexicalPairType?: string | null;
  taskType: string;
  blockCode: string;
  sortOrder: number;
  contextSupportLevel: string;
  expectedSemanticMatch: boolean;
  stimulus: DiagnosisTemplateStimulus;
  options: DiagnosisTemplateOption[];
  correctAnswerKey: string;
  scoringProfile?: DiagnosisTemplateScoringProfile | null;
}

export interface DiagnosisTemplateDetailVO {
  id: number;
  templateName: string;
  description?: string | null;
  status: string;
  targetClassId?: number | null;
  targetClassName?: string | null;
  estimatedDurationMinutes: number;
  scoringVersion: string;
  itemCount: number;
  ownerUserId: number;
  createdAt: string;
  updatedAt: string;
  items: DiagnosisTemplateItemVO[];
}

export interface DiagnosisTemplateDraftBasicVO {
  templateName: string;
  description?: string | null;
  publishTarget?: string | null;
  estimatedDurationMinutes: number;
  targetClassId?: number | null;
  scoringVersion: string;
}

export interface DiagnosisTemplateDraftItemVO {
  draftItemId: string;
  lexicalPairId?: number | null;
  englishWord?: string | null;
  frenchWord?: string | null;
  chineseGloss?: string | null;
  lexicalPairType?: string | null;
  taskType?: string | null;
  blockCode?: string | null;
  sortOrder?: number | null;
  contextSupportLevel?: string | null;
  expectedSemanticMatch?: boolean | null;
  stimulus: DiagnosisTemplateStimulus;
  options: DiagnosisTemplateOption[];
  correctAnswerKey?: string | null;
  scoringProfile?: DiagnosisTemplateScoringProfile | null;
}

export interface DiagnosisTemplateDraftSchemaVO {
  basic: DiagnosisTemplateDraftBasicVO;
  items: DiagnosisTemplateDraftItemVO[];
}

export interface DiagnosisTemplateDraftItemValidationVO {
  draftItemId?: string | null;
  itemIndex: number;
  fieldErrors: Record<string, string>;
}

export interface DiagnosisTemplateDraftValidationResponseVO {
  valid: boolean;
  fieldErrors: Record<string, string>;
  itemErrors: DiagnosisTemplateDraftItemValidationVO[];
  blockingSteps: string[];
}

export interface DiagnosisTemplateDraftSummaryVO {
  draftId: number;
  sourceTemplateId?: number | null;
  publishedTemplateId?: number | null;
  templateName: string;
  description?: string | null;
  syncState: string;
  version: number;
  updatedAt: string;
}

export interface DiagnosisTemplateDraftDetailVO {
  draftId: number;
  sourceTemplateId?: number | null;
  publishedTemplateId?: number | null;
  syncState: string;
  version: number;
  schema: DiagnosisTemplateDraftSchemaVO;
  createdAt: string;
  updatedAt: string;
}

export interface DiagnosisTemplateDraftBasicRequest {
  templateName: string;
  description?: string | null;
  publishTarget?: string | null;
  estimatedDurationMinutes: number;
  targetClassId?: number | null;
  scoringVersion: string;
}

export interface DiagnosisTemplateDraftItemRequest {
  draftItemId: string;
  lexicalPairId?: number | null;
  taskType?: string | null;
  blockCode?: string | null;
  sortOrder?: number | null;
  contextSupportLevel?: string | null;
  expectedSemanticMatch?: boolean | null;
  stimulus: DiagnosisTemplateStimulusRequest;
  options: DiagnosisTemplateOptionRequest[];
  correctAnswerKey?: string | null;
  scoringProfile?: DiagnosisTemplateScoringProfileRequest | null;
}

export interface DiagnosisTemplateDraftSchemaRequest {
  basic: DiagnosisTemplateDraftBasicRequest;
  items: DiagnosisTemplateDraftItemRequest[];
}

export interface DiagnosisTemplateDraftSaveRequest {
  version: number;
  schema: DiagnosisTemplateDraftSchemaRequest;
}

export interface DiagnosisTemplateDeleteResultVO {
  templateId: number;
  outcome: 'DELETED' | 'ARCHIVED';
  status?: string | null;
}

export interface DiagnosisHistorySummaryVO {
  sessionId: number;
  summaryId?: number | null;
  templateId: number;
  templateName: string;
  ownerUserId: number;
  status: string;
  startedAt: string;
  completedAt?: string | null;
  positiveTransferScore?: number | null;
  negativeTransferRisk?: number | null;
  overallAccuracy?: number | null;
}

export interface DiagnosisSessionCreatedVO {
  sessionId: number;
  templateId: number;
  templateName: string;
  status: string;
  totalItems: number;
  answeredItems: number;
  currentItemOrder: number;
  startedAt: string;
}

export interface DiagnosisOptionViewVO {
  key: string;
  label: string;
  semanticMatch?: boolean | null;
}

export interface DiagnosisQuestionItemVO {
  itemResultId: number;
  templateItemId: number;
  taskType: string;
  presentationOrder: number;
  lexicalPairId: number;
  englishWord: string;
  frenchWord: string;
  chineseGloss: string;
  lexicalPairType: string;
  contextSupportLevel: string;
  stimulus: DiagnosisTemplateStimulus;
  options: DiagnosisOptionViewVO[];
}

export interface DiagnosisNextItemVO {
  sessionId: number;
  sessionStatus: string;
  totalItems: number;
  answeredItems: number;
  currentItemOrder: number;
  hasNextItem: boolean;
  item?: DiagnosisQuestionItemVO | null;
}

export interface DiagnosisSessionProgressVO {
  sessionId: number;
  status: string;
  totalItems: number;
  answeredItems: number;
  currentItemOrder: number;
  completed: boolean;
}

export interface DiagnosisSummaryMetricsVO {
  positiveTransferScore: number;
  negativeTransferRisk: number;
  contextSensitivity: number;
  semanticDiscrimination: number;
  overallAccuracy: number;
  averageReactionTime: number;
}

export interface DiagnosisDistributionItem {
  code: string;
  label: string;
  count: number;
  ratio: number;
}

export interface DiagnosisHighRiskLexicalPair {
  lexicalPairId: number;
  englishWord: string;
  frenchWord: string;
  lexicalPairType: string;
  riskScore: number;
  errorCount: number;
  averageReactionTime: number;
  dominantErrorType: string;
}

export interface DiagnosisRadarMetric {
  code: string;
  label: string;
  value: number;
}

export interface DiagnosisContextPerformance {
  level: string;
  accuracy: number;
  avgReactionTime: number;
  totalCount: number;
}

export interface DiagnosisLexicalTypePerformance {
  lexicalPairType: string;
  accuracy: number;
  avgReactionTime: number;
  totalCount: number;
}

export interface DiagnosisResponseTimelinePoint {
  presentationOrder: number;
  itemResultId?: number | null;
  taskType: string;
  lexicalPairType: string;
  reactionTime: number;
  correct: boolean;
  errorType?: string | null;
}

export interface DiagnosisChartPayload {
  radarMetrics: DiagnosisRadarMetric[];
  errorTypeDistribution: DiagnosisDistributionItem[];
  contextPerformance: DiagnosisContextPerformance[];
  lexicalTypePerformance: DiagnosisLexicalTypePerformance[];
  topRiskPairs: DiagnosisHighRiskLexicalPair[];
  responseTimeline: DiagnosisResponseTimelinePoint[];
}

export interface DiagnosisOptionPayload {
  key: string;
  label: string;
  semanticMatch?: boolean | null;
  ignoreContextTrap?: boolean | null;
}

export interface DiagnosisItemResultDetailVO {
  itemResultId: number;
  templateItemId: number;
  presentationOrder: number;
  taskType: string;
  lexicalPairId: number;
  englishWord: string;
  frenchWord: string;
  chineseGloss: string;
  lexicalPairType: string;
  contextSupportLevel: string;
  expectedSemanticMatch: boolean;
  stimulus: DiagnosisTemplateStimulus;
  options: DiagnosisOptionPayload[];
  correctAnswerKey?: string | null;
  selectedAnswerKey?: string | null;
  reactionTimeMs: number;
  hesitationTimeMs: number;
  correct: boolean;
  semanticConsistent: boolean;
  detectedErrorType: string;
  transferRiskScore: number;
  itemScore: number;
}

export interface DiagnosisResultDetailVO {
  summaryId: number;
  sessionId: number;
  status: string;
  templateId: number;
  templateName: string;
  ownerUserId: number;
  totalItems: number;
  answeredItems: number;
  startedAt: string;
  completedAt?: string | null;
  metrics: DiagnosisSummaryMetricsVO;
  errorTypeDistribution: DiagnosisDistributionItem[];
  highRiskLexicalPairs: DiagnosisHighRiskLexicalPair[];
  chartPayload: DiagnosisChartPayload;
  items: DiagnosisItemResultDetailVO[];
}

export interface TrainingSuggestedSessionVO {
  mode: string;
  label: string;
  count: number;
}

export interface RecommendedTrainingPairVO {
  planItemId: number;
  lexicalPairId: number;
  englishWord: string;
  frenchWord: string;
  chineseGloss: string;
  lexicalPairType: string;
  recommendedMode: string;
  recommendedDifficulty: number;
  riskLevel: string;
  priorityScore: number;
  recommendedReason: string;
  dominantErrorType: string;
  expectedExposures: number;
  targetContextSupport: string;
}

export interface RecommendedTrainingPlanVO {
  planId: number;
  sourceDiagnosisSessionId: number;
  sourceDiagnosisSummaryId: number;
  status: string;
  priorityMode: string;
  recommendedDifficulty: number;
  riskLevel: string;
  estimatedTrainingVolume: number;
  recommendationReason: string;
  targetMetrics: string[];
  suggestedSessions: TrainingSuggestedSessionVO[];
  recommendedPairs: RecommendedTrainingPairVO[];
  generatedAt: string;
}

export interface WrongBookItemVO {
  wrongBookId: number;
  lexicalPairId: number;
  englishWord: string;
  frenchWord: string;
  chineseGloss: string;
  lexicalPairType: string;
  recommendedMode: string;
  wrongCount: number;
  lastErrorType: string;
  masteryStatus: string;
  firstWrongAt: string;
  lastWrongAt: string;
  nextReviewAt?: string | null;
}

export interface ReviewScheduleItemVO {
  reviewScheduleId: number;
  wrongBookId: number;
  lexicalPairId: number;
  englishWord: string;
  frenchWord: string;
  chineseGloss: string;
  lexicalPairType: string;
  scheduleStage: number;
  intervalDays: number;
  dueAt: string;
  status: string;
  reviewMode: string;
  triggerReason: string;
}

export interface TrainingWordPairVO {
  en: string;
  fr: string;
  zh: string;
  type: string;
}

export interface TrainingExerciseContentVO {
  question: string;
  options: string[];
  explanation: string;
  contextLevel?: string | null;
  sentence?: string | null;
}

export interface TrainingOptionViewVO {
  key: string;
  label: string;
}

export interface TrainingStimulusPayload {
  instruction?: string | null;
  questionText?: string | null;
  contextSentence?: string | null;
  explanation?: string | null;
  contextSupportLevel?: string | null;
}

export interface TrainingQuestionItemVO {
  itemResultId: number;
  planItemId: number;
  mode: string;
  itemType: string;
  presentationOrder: number;
  lexicalPairId: number;
  englishWord: string;
  frenchWord: string;
  chineseGloss: string;
  lexicalPairType: string;
  wordPair: TrainingWordPairVO;
  difficultyLevel: number;
  cognitiveTag: string;
  content: TrainingExerciseContentVO;
  stimulus: TrainingStimulusPayload;
  options: TrainingOptionViewVO[];
}

export interface TrainingSessionCreatedVO {
  sessionId: number;
  planId: number;
  status: string;
  mode: string;
  totalItems: number;
  answeredItems: number;
  currentItemOrder: number;
}

export interface TrainingNextItemVO {
  sessionId: number;
  sessionStatus: string;
  mode: string;
  totalItems: number;
  answeredItems: number;
  currentItemOrder: number;
  hasNextItem: boolean;
  item?: TrainingQuestionItemVO | null;
}

export interface TrainingSessionProgressVO {
  sessionId: number;
  status: string;
  totalItems: number;
  answeredItems: number;
  currentItemOrder: number;
  completed: boolean;
}

export interface TrainingHistorySummaryVO {
  sessionId: number;
  planId: number;
  ownerUserId: number;
  status: string;
  mode: string;
  totalItems: number;
  answeredItems: number;
  currentItemOrder: number;
  startedAt: string;
  lastSavedAt?: string | null;
  completedAt?: string | null;
}

export interface TrainingRiskWordVO {
  lexicalPairId: number;
  englishWord: string;
  frenchWord: string;
  chineseGloss: string;
  lexicalPairType: string;
  reason: string;
  riskLevel: string;
  dominantErrorType: string;
}

export interface TrainingItemResultDetailVO {
  itemResultId: number;
  planItemId?: number | null;
  presentationOrder: number;
  mode: string;
  itemType: string;
  lexicalPairId: number;
  englishWord?: string | null;
  frenchWord?: string | null;
  chineseGloss?: string | null;
  lexicalPairType?: string | null;
  wordPair?: TrainingWordPairVO | null;
  difficultyLevel?: number | null;
  cognitiveTag: string;
  content: TrainingExerciseContentVO;
  stimulus: TrainingStimulusPayload;
  options: TrainingOptionViewVO[];
  correctAnswerKey?: string | null;
  selectedAnswerKey?: string | null;
  submittedAt?: string | null;
  reactionTimeMs?: number | null;
  hesitationTimeMs?: number | null;
  correct?: boolean | null;
  detectedErrorType?: string | null;
  reviewRequired?: boolean | null;
  adaptationAction?: string | null;
}

export interface TrainingSessionSummaryVO {
  sessionId: number;
  mode: string;
  accuracy: number;
  averageReactionTime: number;
  improvementHint: string;
  nextRecommendedMode: string;
  riskWordsToReview: TrainingRiskWordVO[];
  items: TrainingItemResultDetailVO[];
}

export interface AiRecommendationPathItemVO {
  title: string;
  reason: string;
  priority: string;
}

export interface AiFocusLexicalPairVO {
  lexicalPairId: number;
  englishWord: string;
  frenchWord: string;
  chineseGloss: string;
  lexicalPairType: string;
  riskScore: number;
  dominantErrorType: string;
  focusReason: string;
}

export interface AiRecommendedTrainingModeVO {
  mode: string;
  label: string;
  reason: string;
}

export interface AiGuidanceResponseVO {
  requestId: string;
  generationSource: string;
  promptVersion: string;
  model?: string | null;
  latencyMs: number;
  recommendationPath: AiRecommendationPathItemVO[];
  focusLexicalPairs: AiFocusLexicalPairVO[];
  recommendedTrainingModes: AiRecommendedTrainingModeVO[];
  explanation: string;
  teacherNote: string;
  confidence: number;
  fallbackReason?: string | null;
}

export interface RagCitation {
  citationId: string;
  sourceType: string;
  sourceId: string;
  title: string;
  snippet: string;
  score?: number | null;
}

export interface RagContextChunk {
  citationId: string;
  sourceType: string;
  sourceId: string;
  title: string;
  content: string;
  snippet: string;
  score?: number | null;
  metadata?: Record<string, unknown> | null;
}

export interface LexicalRagAnswerVO {
  requestId: string;
  generationSource: string;
  model?: string | null;
  latencyMs: number;
  grounded: boolean;
  answer: string;
  explanation: string;
  recommendedActions: string[];
  confidence: number;
  citationIds?: string[];
  citations?: RagCitation[];
  contextChunks?: RagContextChunk[];
  fallbackReason?: string | null;
}

export interface TeachingClassSummaryVO {
  classId: number;
  classCode: string;
  className: string;
  gradeName: string;
  studentCount: number;
}

export interface TeacherClassStudentVO {
  studentUserId: number;
  studentName: string;
  username?: string | null;
  studentNo?: string | null;
  gradeName?: string | null;
  joinedAt?: string | null;
}

export interface TeacherClassDetailVO {
  classId: number;
  classCode: string;
  className: string;
  gradeName: string;
  teacherUserId: number;
  active: boolean;
  studentCount: number;
  createdAt?: string | null;
  updatedAt?: string | null;
  students: TeacherClassStudentVO[];
}

export interface TeacherClassStudentCandidateVO {
  studentUserId: number;
  studentName: string;
  username?: string | null;
  studentNo?: string | null;
  gradeName?: string | null;
  assigned: boolean;
  activeClassCount: number;
}

export interface TeacherClassUpsertRequest {
  classCode: string;
  className: string;
  gradeName: string;
}

export interface TeacherClassInviteCodeVO {
  classCode: string;
}

export interface TeacherClassStudentBatchRequest {
  studentUserIds: number[];
}

export interface RiskBucketPayload {
  riskLevel: string;
  studentCount: number;
}

export interface ModeFocusPayload {
  mode: string;
  studentCount: number;
}

export interface ClassRiskPairPayload {
  lexicalPairId: number;
  englishWord: string;
  frenchWord: string;
  lexicalPairType: string;
  riskScore: number;
  attemptCount: number;
  incorrectCount: number;
}

export interface ClassErrorDistributionPayload {
  errorType: string;
  count: number;
  ratio: number;
}

export interface ClassAnalyticsSnapshotPayload {
  classCode: string;
  className: string;
  gradeName: string;
  studentCount: number;
  activeStudentCount: number;
  highRiskStudentCount: number;
  recentAccuracy: number;
  recentNegativeTransferRisk: number;
  recentAvgReactionTimeMs: number;
  primaryRiskLevel: string;
  lastActiveAt?: string | null;
  riskDistribution: RiskBucketPayload[];
  recommendedFocusModes: ModeFocusPayload[];
  topRiskPairs: ClassRiskPairPayload[];
  errorDistribution: ClassErrorDistributionPayload[];
}

export interface ClassAnalyticsOverviewVO {
  classId: number;
  classCode: string;
  className: string;
  studentCount: number;
  activeStudentCount: number;
  highRiskStudentCount: number;
  primaryRiskLevel: string;
  cards: AnalyticsCardVO[];
  radar: AnalyticsRadarMetricVO[];
  latestSnapshot: ClassAnalyticsSnapshotPayload;
}

export interface AnalyticsRiskBucketVO {
  bucketStart: number;
  bucketEnd: number;
  studentCount: number;
}

export interface StudentProfileSummaryVO {
  studentUserId: number;
  studentName: string;
  gradeName: string;
  primaryRiskLevel: string;
  recentAccuracy: number;
  recentNegativeTransferRisk: number;
  recentAvgReactionTimeMs: number;
  pendingReviewCount: number;
  recommendedTrainingMode: string;
  lastActiveAt?: string | null;
}

export interface ClassCompletionByModeVO {
  mode: string;
  completionRate: number;
  completedStudentCount: number;
  studentCount: number;
}

export interface ClassCompletionRateVO {
  overallRate: number;
  studentCount: number;
  completedStudentCount: number;
  trend: AnalyticsTrendVO;
  byMode: ClassCompletionByModeVO[];
}

export interface TeacherStudentDetailVO {
  studentUserId: number;
  studentName: string;
  classRank: number;
  classPercentile: number;
  analysis: StudentAnalyticsDetailVO;
}

export interface TeacherInterventionSummaryVO {
  id: number;
  studentUserId: number;
  studentName: string;
  classId: number;
  className: string;
  priority: string;
  status: string;
  plannedAt?: string | null;
  completedAt?: string | null;
  teacherNote?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
  patternDetected: string;
  suggestedAction: string;
  effectTracking?: TeacherInterventionEffectVO | null;
}

export interface TeacherInterventionEffectSnapshotVO {
  snapshotId: number;
  snapshotAt?: string | null;
  primaryRiskLevel?: string | null;
  recommendedTrainingMode?: string | null;
  pendingReviewCount?: number | null;
  highRiskPairCount?: number | null;
  recentAccuracy?: number | null;
  recentNegativeTransferRisk?: number | null;
  recentAvgReactionTimeMs?: number | null;
}

export interface TeacherInterventionEffectDiffVO {
  recentAccuracyDelta?: number | null;
  recentNegativeTransferRiskDelta?: number | null;
  recentAvgReactionTimeMsDelta?: number | null;
  pendingReviewCountDelta?: number | null;
  highRiskPairCountDelta?: number | null;
}

export interface TeacherInterventionEffectVO {
  baselineSnapshotId?: number | null;
  completionSnapshotId?: number | null;
  baselineSnapshot?: TeacherInterventionEffectSnapshotVO | null;
  completionSnapshot?: TeacherInterventionEffectSnapshotVO | null;
  metricDiff?: TeacherInterventionEffectDiffVO | null;
}

export interface TeacherInterventionUpdateRequest {
  priority?: string | null;
  status?: string | null;
  plannedAt?: string | null;
  teacherNote?: string | null;
}

export interface UserSummaryVO {
  id: number;
  username: string;
  email: string;
  displayName: string;
  enabled: boolean;
  roles: Role[];
  lastLoginAt?: string | null;
  studentProfileLinked?: boolean;
  teacherProfileLinked?: boolean;
  profileLinkStatus?: string | null;
  invitationStatus?: string | null;
  hasActiveSession?: boolean;
}

export interface AdminAuditLogItemVO {
  id: number;
  actorUserId?: number | null;
  actorUsername?: string | null;
  actorDisplayName?: string | null;
  actionType: string;
  targetType: string;
  targetId?: string | null;
  requestPath: string;
  requestMethod: string;
  traceId: string;
  requestPayload?: string | null;
  responseCode: string;
  createdAt: string;
}

export interface AdminUserCreateRequest {
  username: string;
  email: string;
  displayName: string;
  initialPassword?: string;
  credentialMode?: 'INVITE_LINK' | 'MANUAL_PASSWORD';
  enabled: boolean;
  roles: Role[];
}

export interface AdminUserAccessUpdateRequest {
  enabled: boolean;
  roles: Role[];
}

export interface AdminUserProvisionResultVO {
  user: UserSummaryVO;
  accountAction?: AccountActionLinkVO | null;
}

export interface AdminDashboardOverviewVO {
  totalUsers: number;
  enabledUsers: number;
  registrationsLast30Days: number;
  dailyActiveUsers: number;
  weeklyActiveUsers: number;
  diagnosisCompletedLast30Days: number;
  trainingCompletedLast30Days: number;
  assessmentCompletedLast30Days: number;
  aiCallsLast30Days: number;
  aiFallbackCountLast30Days: number;
  aiFallbackRateLast30Days: number;
  generatedAt: string;
}

export interface AdminDashboardRegistrationTrendPointVO {
  date: string;
  registrations: number;
}

export interface AdminDashboardCompletionTrendPointVO {
  date: string;
  diagnosisCompleted: number;
  trainingCompleted: number;
  assessmentCompleted: number;
}

export interface AdminDashboardAiTrendPointVO {
  date: string;
  totalCalls: number;
  fallbackCalls: number;
  fallbackRate: number;
}

export interface AdminDashboardSceneDistributionVO {
  scene: string;
  count: number;
  ratio: number;
}

export interface AdminDashboardVO {
  overview: AdminDashboardOverviewVO;
  registrationTrend: AdminDashboardRegistrationTrendPointVO[];
  completionTrend: AdminDashboardCompletionTrendPointVO[];
  aiTrend: AdminDashboardAiTrendPointVO[];
  aiSceneDistribution: AdminDashboardSceneDistributionVO[];
}

export interface AccountActionLinkVO {
  purpose: string;
  linkUrl: string;
  expiresAt: string;
  status: string;
}

export interface AccountActionPreviewVO {
  purpose: string;
  username: string;
  email: string;
  displayName: string;
  enabled: boolean;
  expiresAt: string;
}

export interface CompleteAccountActionRequest {
  password: string;
}

export interface SessionOverviewVO {
  lastLoginAt?: string | null;
  refreshSessionIssuedAt?: string | null;
  refreshSessionExpiresAt?: string | null;
  accessTokenExpiresAt?: string | null;
  userAgentFingerprint?: string | null;
  issuedIpAddress?: string | null;
  hasActiveSession: boolean;
}

export interface LexicalPairSummaryVO {
  id: number;
  englishWord: string;
  frenchWord: string;
  chineseGloss: string;
  lexicalPairType: string;
  semanticOverlapScore: number;
  falseFriendRisk: number;
  riskLevel: string;
  defaultContextSupport: string;
  difficultyLevel: number;
  source?: string | null;
  active: boolean;
  knowledgeStatus?: string | null;
  embeddingStatus?: string | null;
  lastEmbeddedAt?: string | null;
  tags: string[];
}

export interface LexicalPairOverviewVO {
  totalCount: number;
  activeCount: number;
  pendingEmbeddingCount: number;
  embeddedCount: number;
  failedEmbeddingCount: number;
  latestCreatedAt?: string | null;
  latestUpdatedAt?: string | null;
  latestEmbeddedAt?: string | null;
}

export interface LexicalPairExampleVO {
  id?: number | null;
  sortOrder: number;
  englishExample: string;
  frenchExample: string;
  chineseTranslation: string;
  contextSupportLevel: string;
  source?: string | null;
}

export interface LexicalPairSenseVO {
  id?: number | null;
  sortOrder: number;
  englishDefinition: string;
  frenchDefinition: string;
  chineseDefinition: string;
  examples: LexicalPairExampleVO[];
}

export interface LexicalPairDetailVO {
  id: number;
  englishWord: string;
  frenchWord: string;
  chineseGloss: string;
  lexicalPairType: string;
  semanticOverlapScore: number;
  falseFriendRisk: number;
  riskLevel: string;
  defaultContextSupport: string;
  difficultyLevel: number;
  notes?: string | null;
  source?: string | null;
  active: boolean;
  searchableText?: string | null;
  knowledgeStatus?: string | null;
  embeddingStatus?: string | null;
  lastEmbeddedAt?: string | null;
  tags: string[];
  senses: LexicalPairSenseVO[];
}

export interface LexicalListSummaryVO {
  id: number;
  listName: string;
  description?: string | null;
  ownerUserId: number;
  ownerDisplayName?: string | null;
  active: boolean;
  itemCount: number;
  createdAt: string;
  updatedAt?: string | null;
}

export interface LexicalListItemVO {
  itemId: number;
  lexicalPairId: number;
  sortOrder: number;
  notes?: string | null;
  englishWord: string;
  frenchWord: string;
  chineseGloss: string;
  lexicalPairType: string;
  defaultContextSupport: string;
  difficultyLevel: number;
  riskLevel: string;
}

export interface LexicalListDetailVO {
  id: number;
  listName: string;
  description?: string | null;
  ownerUserId: number;
  ownerDisplayName?: string | null;
  active: boolean;
  itemCount: number;
  createdAt: string;
  updatedAt?: string | null;
  items: LexicalListItemVO[];
}

export interface UpdateLexicalListRequest {
  listName: string;
  description?: string | null;
  active: boolean;
}

export interface ReorderLexicalListItemsRequest {
  orderedItemIds: number[];
}

export interface CsvImportTemplateFieldVO {
  fieldName: string;
  required: boolean;
  description?: string | null;
  example?: string | null;
}

export interface CsvImportTemplateVO {
  fields: CsvImportTemplateFieldVO[];
  headerLine: string;
  exampleLine: string;
}

export interface CsvImportFailureVO {
  rowNumber: number;
  englishWord?: string | null;
  frenchWord?: string | null;
  reason?: string | null;
}

export interface CsvImportResultVO {
  successCount: number;
  failedCount: number;
  failures: CsvImportFailureVO[];
}

export type LexicalImportBatchStatus = 'PARSING' | 'DRAFT' | 'IMPORTING' | 'COMPLETED' | 'FAILED';
export type LexicalImportRowStatus = 'READY' | 'INVALID' | 'SKIPPED' | 'IMPORTED';

export interface LexicalImportRowDraft {
  englishWord?: string | null;
  frenchWord?: string | null;
  chineseGloss?: string | null;
  lexicalPairType?: string | null;
  semanticOverlapScore?: string | null;
  falseFriendRisk?: string | null;
  defaultContextSupport?: string | null;
  difficultyLevel?: string | null;
  notes?: string | null;
  source?: string | null;
  active?: string | null;
  tags?: string | null;
  knowledgeStatus?: string | null;
  embeddingStatus?: string | null;
  senseEnglishDefinition?: string | null;
  senseFrenchDefinition?: string | null;
  senseChineseDefinition?: string | null;
  exampleEnglish?: string | null;
  exampleFrench?: string | null;
  exampleChinese?: string | null;
  exampleContextSupport?: string | null;
}

export interface LexicalImportRowUpdateRequest extends LexicalImportRowDraft {
  skipped?: boolean;
}

export interface LexicalImportBatchCreatedVO {
  batchId: number;
  status: LexicalImportBatchStatus;
}

export interface LexicalImportBatchSummaryVO {
  id: number;
  status: LexicalImportBatchStatus;
  sourceFormat: string;
  originalFilename: string;
  contentType?: string | null;
  fileSizeBytes: number;
  totalRows: number;
  readyRows: number;
  invalidRows: number;
  skippedRows: number;
  importedRows: number;
  errorMessage?: string | null;
  ownerUserId: number;
  ownerDisplayName?: string | null;
  createdAt: string;
  updatedAt: string;
  parserJobFinishedAt?: string | null;
  importJobFinishedAt?: string | null;
}

export interface LexicalImportBatchDetailVO {
  id: number;
  status: LexicalImportBatchStatus;
  sourceFormat: string;
  originalFilename: string;
  contentType?: string | null;
  fileSizeBytes: number;
  totalRows: number;
  readyRows: number;
  invalidRows: number;
  skippedRows: number;
  importedRows: number;
  errorMessage?: string | null;
  ownerUserId: number;
  ownerDisplayName?: string | null;
  fileSha256?: string | null;
  parserJobStartedAt?: string | null;
  parserJobFinishedAt?: string | null;
  importJobStartedAt?: string | null;
  importJobFinishedAt?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface LexicalImportRowVO {
  id: number;
  rowNumber: number;
  status: LexicalImportRowStatus;
  draft: LexicalImportRowDraft;
  validationErrors: string[];
  importedLexicalPairId?: number | null;
  importMessage?: string | null;
}

export interface AddLexicalListItemsResultVO {
  addedCount: number;
  skippedPairIds: number[];
}

export interface DiagnosisTemplateUpsertRequest {
  templateName: string;
  description?: string;
  status: string;
  estimatedDurationMinutes: number;
  targetClassId?: number | null;
  scoringVersion: string;
  items: DiagnosisTemplateItemRequest[];
}

export interface LexicalPairUpsertRequest {
  englishWord: string;
  frenchWord: string;
  chineseGloss: string;
  lexicalPairType: string;
  semanticOverlapScore: number;
  falseFriendRisk: number;
  defaultContextSupport: string;
  difficultyLevel: number;
  notes?: string;
  source?: string;
  active?: boolean;
  knowledgeStatus?: string;
  embeddingStatus?: string;
  tags?: string[];
  senses: LexicalPairSenseVO[];
}

export interface CreateLexicalListRequest {
  listName: string;
  description?: string;
  active?: boolean;
}

export interface AddLexicalListItemsRequest {
  lexicalPairIds: number[];
}

export interface AiOpsChatConfig {
  baseUrl: string;
  apiKey?: string | null;
  model: string;
  timeout: string;
  temperature: number;
  maxTokens: number;
}

export interface AiOpsEmbeddingConfig {
  baseUrl: string;
  apiKey?: string | null;
  model: string;
  timeout: string;
  dimension: number;
}

export interface AiOpsRerankConfig {
  baseUrl: string;
  apiKey?: string | null;
  model: string;
  timeout: string;
}

export interface AiOpsProviderDefinition {
  chat: AiOpsChatConfig;
  embedding: AiOpsEmbeddingConfig;
  rerank: AiOpsRerankConfig;
}

export interface AiOpsProviderConfig {
  activeProvider: string;
  fallbackProvider: string;
  providers: Record<string, AiOpsProviderDefinition>;
}

export interface AiOpsResilienceConfig {
  maxAttempts: number;
  waitDuration: string;
  failureRateThreshold: number;
  slidingWindowSize: number;
  openStateDuration: string;
}

export interface AiOpsRagAppServerConfig {
  baseUrl: string;
  internalToken?: string | null;
  connectTimeout: string;
  readTimeout: string;
}

export interface AiOpsRagIngestionConfig {
  exportPageSize: number;
  embeddingBatchSize: number;
}

export interface AiOpsRagRetrievalConfig {
  recallTopK: number;
  recallThreshold: number;
  rerankTopN: number;
  rerankThreshold: number;
  finalTopK: number;
}

export interface AiOpsRagConfig {
  appServer: AiOpsRagAppServerConfig;
  ingestion: AiOpsRagIngestionConfig;
  retrieval: AiOpsRagRetrievalConfig;
}

export interface AiOpsConfigPayload {
  provider: AiOpsProviderConfig;
  resilience: AiOpsResilienceConfig;
  rag: AiOpsRagConfig;
}

export interface AiOpsConfigIssue {
  field: string;
  message: string;
}

export interface AiOpsConfigValidationResponse {
  valid: boolean;
  issues: AiOpsConfigIssue[];
  notices: string[];
}

export interface AdminAiSecretFieldVO {
  configured: boolean;
  maskedValue: string;
}

export interface AdminAiProviderSecretFieldsVO {
  chatApiKey: AdminAiSecretFieldVO;
  embeddingApiKey: AdminAiSecretFieldVO;
  rerankApiKey: AdminAiSecretFieldVO;
}

export interface AdminAiSecretFieldsVO {
  providers: Record<string, AdminAiProviderSecretFieldsVO>;
  appServerInternalToken: AdminAiSecretFieldVO;
}

export interface AdminAiRuntimeStateVO {
  available: boolean;
  source?: string | null;
  version?: number | null;
  appliedAt?: string | null;
  inSync: boolean;
}

export interface AdminAiStoredStateVO {
  present: boolean;
  version?: number | null;
  updatedAt?: string | null;
}

export interface AdminAiConfigViewVO {
  config: AiOpsConfigPayload;
  secrets: AdminAiSecretFieldsVO;
  source: string;
  version?: number | null;
  updatedAt?: string | null;
  notices: string[];
  runtime: AdminAiRuntimeStateVO;
  stored: AdminAiStoredStateVO;
}

export interface AdminAiSecretValueUpdate {
  retainExisting?: boolean;
  value?: string | null;
}

export interface AdminAiProviderSecretUpdateGroup {
  chatApiKey?: AdminAiSecretValueUpdate;
  embeddingApiKey?: AdminAiSecretValueUpdate;
  rerankApiKey?: AdminAiSecretValueUpdate;
}

export interface AdminAiSecretUpdateGroup {
  providers?: Record<string, AdminAiProviderSecretUpdateGroup>;
  appServerInternalToken?: AdminAiSecretValueUpdate;
}

export interface AdminAiConfigSaveRequest {
  config: AiOpsConfigPayload;
  expectedVersion?: number | null;
  providerOrigins?: Record<string, string>;
  secrets: AdminAiSecretUpdateGroup;
}

export interface AdminOutboxRecordVO {
  id: number;
  eventId: string;
  eventType: string;
  routingKey: string;
  status: string;
  attemptCount: number;
  traceId?: string | null;
  lastError?: string | null;
  nextAttemptAt?: string | null;
  processingStartedAt?: string | null;
  publishedAt?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface AiGatewayHealthResponse {
  service: string;
  status: string;
  provider: string;
  fallbackProvider: string;
  chatModel: string;
  embeddingModel: string;
  rerankModel: string;
  databaseReady: boolean;
  vectorStoreReady: boolean;
  providerReady: boolean;
  rerankReady: boolean;
  appServerReady: boolean;
  vectorExtensionVersion: string;
  activeProfiles: string[];
  timestamp: string;
  appServerError?: string | null;
}

export interface RagReindexRequest {
  mode?: string;
  sourceTypes?: string[];
  sourceIds?: string[];
  forceReembed?: boolean;
}

export interface RagReindexResponse {
  jobId: number;
  status: string;
}

export interface RagReindexJobResponse {
  jobId: number;
  jobType: string;
  mode: string;
  status: string;
  sourceTypes: string[];
  sourceIds: string[];
  lastCursor?: string | null;
  lastSourceUpdatedAt?: string | null;
  finishedAt?: string | null;
  stats: Record<string, unknown>;
  errorMessage?: string | null;
}

export type AssessmentQuestionType = 'SINGLE_CHOICE' | 'MULTIPLE_CHOICE' | 'FILL_BLANK';
export type AssessmentPaperStatus = 'DRAFT' | 'PUBLISHED';
export type AssessmentAttemptStatus = 'IN_PROGRESS' | 'SUBMITTED';

export interface AssessmentOptionRequest {
  key: string;
  label: string;
}

export interface AssessmentQuestionRequest {
  questionType: AssessmentQuestionType;
  stemText: string;
  promptText?: string | null;
  options?: AssessmentOptionRequest[];
  correctAnswers: string[];
  explanationText?: string | null;
  score: number;
}

export interface AssessmentPaperSaveRequest {
  title: string;
  description?: string | null;
  durationMinutes: number;
  questions: AssessmentQuestionRequest[];
}

export interface AssessmentPublishRequest {
  teachingClassId: number;
  startsAt?: string | null;
  dueAt?: string | null;
  instructionsText?: string | null;
}

export interface AssessmentAttemptResponseRequest {
  questionOrder: number;
  responses?: string[];
}

export interface SaveAssessmentResponsesRequest {
  responses: AssessmentAttemptResponseRequest[];
}

export interface AssessmentOptionVO {
  key: string;
  label: string;
}

export interface AssessmentPaperQuestionVO {
  questionId: number;
  questionType: AssessmentQuestionType;
  sortOrder: number;
  stemText: string;
  promptText?: string | null;
  options: AssessmentOptionVO[];
  correctAnswers: string[];
  explanationText?: string | null;
  score: number;
}

export interface AssessmentPublishSummaryVO {
  publishId: number;
  teachingClassId: number;
  className: string;
  status: string;
  durationMinutes: number;
  questionCount: number;
  totalScore: number;
  instructionsText?: string | null;
  startsAt?: string | null;
  dueAt?: string | null;
  publishedAt: string;
  assignedCount: number;
  attemptCount: number;
  submittedCount: number;
  pendingCount: number;
}

export interface AssessmentPaperSummaryVO {
  paperId: number;
  paperCode: string;
  title: string;
  description?: string | null;
  status: AssessmentPaperStatus | string;
  durationMinutes: number;
  questionCount: number;
  totalScore: number;
  latestPublishAt?: string | null;
  updatedAt: string;
}

export interface AssessmentPaperDetailVO {
  paperId: number;
  paperCode: string;
  title: string;
  description?: string | null;
  status: AssessmentPaperStatus | string;
  durationMinutes: number;
  questionCount: number;
  totalScore: number;
  latestPublishAt?: string | null;
  questions: AssessmentPaperQuestionVO[];
  publishes: AssessmentPublishSummaryVO[];
}

export interface StudentAssessmentSummaryVO {
  publishId: number;
  paperId: number;
  title: string;
  description?: string | null;
  teachingClassId: number;
  className: string;
  instructionsText?: string | null;
  durationMinutes: number;
  questionCount: number;
  totalScore: number;
  startsAt?: string | null;
  dueAt?: string | null;
  publishedAt: string;
  attemptStatus?: AssessmentAttemptStatus | string | null;
  attemptId?: number | null;
  answeredCount?: number | null;
  startedAt?: string | null;
  expiresAt?: string | null;
  submittedAt?: string | null;
}

export interface StudentAssessmentHistorySummaryVO {
  attemptId: number;
  publishId: number;
  paperId: number;
  title: string;
  description?: string | null;
  className: string;
  status: AssessmentAttemptStatus | string;
  questionCount: number;
  answeredCount: number;
  objectiveScore: number;
  totalScore: number;
  startedAt: string;
  lastSavedAt?: string | null;
  expiresAt?: string | null;
  submittedAt?: string | null;
}

export interface AssessmentAttemptStartVO {
  attemptId: number;
  publishId: number;
  status: AssessmentAttemptStatus | string;
  resumed: boolean;
}

export interface AssessmentAttemptQuestionVO {
  answerId: number;
  questionId: number;
  questionOrder: number;
  questionType: AssessmentQuestionType;
  stemText: string;
  promptText?: string | null;
  options: AssessmentOptionVO[];
  score: number;
  responses: string[];
  answered: boolean;
}

export interface AssessmentAttemptDetailVO {
  attemptId: number;
  publishId: number;
  paperId: number;
  paperTitle: string;
  paperDescription?: string | null;
  className: string;
  status: AssessmentAttemptStatus | string;
  instructionsText?: string | null;
  durationMinutes: number;
  questionCount: number;
  answeredCount: number;
  totalScore: number;
  startedAt: string;
  expiresAt: string;
  submittedAt?: string | null;
  lastSavedAt?: string | null;
  serverTime: string;
  questions: AssessmentAttemptQuestionVO[];
}

export interface AssessmentAttemptProgressVO {
  attemptId: number;
  status: AssessmentAttemptStatus | string;
  answeredCount: number;
  lastSavedAt?: string | null;
}

export interface AssessmentAttemptSubmitVO {
  attemptId: number;
  status: AssessmentAttemptStatus | string;
  submittedAt?: string | null;
}

export interface AssessmentAttemptResultQuestionVO {
  answerId: number;
  questionId: number;
  questionOrder: number;
  questionType: AssessmentQuestionType;
  stemText: string;
  promptText?: string | null;
  options: AssessmentOptionVO[];
  score: number;
  responses: string[];
  correctAnswers: string[];
  correct?: boolean | null;
  scoreAwarded?: number | null;
  explanationText?: string | null;
}

export interface AssessmentAttemptResultVO {
  attemptId: number;
  publishId: number;
  paperId: number;
  paperTitle: string;
  paperDescription?: string | null;
  className: string;
  status: AssessmentAttemptStatus | string;
  instructionsText?: string | null;
  questionCount: number;
  answeredCount: number;
  correctCount: number;
  objectiveScore: number;
  totalScore: number;
  startedAt: string;
  expiresAt: string;
  submittedAt: string;
  questions: AssessmentAttemptResultQuestionVO[];
}

export interface AssessmentPublishRosterItemVO {
  studentUserId: number;
  studentName: string;
  attemptStatus: 'NOT_STARTED' | AssessmentAttemptStatus | string;
  attemptId?: number | null;
  answeredCount?: number | null;
  questionCount?: number | null;
  objectiveScore?: number | null;
  totalScore?: number | null;
  startedAt?: string | null;
  expiresAt?: string | null;
  submittedAt?: string | null;
  lastSavedAt?: string | null;
}

export interface AssessmentPublishDetailVO {
  publishId: number;
  paperId: number;
  paperTitle: string;
  paperDescription?: string | null;
  teachingClassId: number;
  className: string;
  status: string;
  durationMinutes: number;
  questionCount: number;
  totalScore: number;
  instructionsText?: string | null;
  startsAt?: string | null;
  dueAt?: string | null;
  publishedAt: string;
  assignedCount: number;
  notStartedCount: number;
  inProgressCount: number;
  submittedCount: number;
  averageScore?: number | null;
  roster: AssessmentPublishRosterItemVO[];
}

export interface TeacherAssessmentAttemptResultVO {
  attemptId: number;
  publishId: number;
  paperId: number;
  teachingClassId: number;
  studentUserId: number;
  studentName: string;
  paperTitle: string;
  paperDescription?: string | null;
  className: string;
  status: AssessmentAttemptStatus | string;
  instructionsText?: string | null;
  questionCount: number;
  answeredCount: number;
  correctCount: number;
  objectiveScore: number;
  totalScore: number;
  startedAt: string;
  expiresAt: string;
  submittedAt: string;
  questions: AssessmentAttemptResultQuestionVO[];
}

export type {
  TeacherWorkspaceAssessmentPublishVO,
  TeacherWorkspaceClassActivityVO,
  TeacherWorkspaceDraftTemplateVO,
  TeacherWorkspaceInterventionVO,
  TeacherWorkspaceLexicalListVO,
  TeacherWorkspaceOverviewVO,
  TeacherWorkspaceSummaryVO,
} from './contracts/teacherWorkspace';
