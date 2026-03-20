export type Role = 'STUDENT' | 'TEACHER' | 'ADMIN';

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
  primaryRole: Role;
  roles: Role[];
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
  itemCount: number;
  estimatedDurationMinutes: number;
  scoringVersion: string;
  ownerUserId: number;
  updatedAt: string;
}

export interface DiagnosisTemplateStimulus {
  instruction?: string | null;
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
  idealReactionTimeMs?: number | null;
  reactionTimeWeight?: number | null;
  hesitationWeight?: number | null;
  accuracyWeight?: number | null;
  negativeTransferPenalty?: number | null;
  contextBonus?: number | null;
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
  estimatedDurationMinutes: number;
  scoringVersion: string;
  itemCount: number;
  ownerUserId: number;
  createdAt: string;
  updatedAt: string;
  items: DiagnosisTemplateItemVO[];
}

export interface DiagnosisHistorySummaryVO {
  sessionId: number;
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
  key: string;
  label: string;
  value: number;
  max: number;
}

export interface DiagnosisContextPerformance {
  contextSupportLevel: string;
  accuracy: number;
  avgReactionTimeMs: number;
  attemptCount: number;
}

export interface DiagnosisLexicalTypePerformance {
  lexicalPairType: string;
  accuracy: number;
  avgReactionTimeMs: number;
  attemptCount: number;
}

export interface DiagnosisResponseTimelinePoint {
  order: number;
  reactionTimeMs: number;
  hesitationTimeMs: number;
  correct: boolean;
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

export interface TrainingSessionSummaryVO {
  sessionId: number;
  mode: string;
  accuracy: number;
  averageReactionTime: number;
  improvementHint: string;
  nextRecommendedMode: string;
  riskWordsToReview: TrainingRiskWordVO[];
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
  label: string;
  completionRate: number;
  completedCount: number;
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
  patternDetected: string;
  suggestedAction: string;
}

export interface UserSummaryVO {
  id: number;
  username: string;
  email: string;
  displayName: string;
  enabled: boolean;
  roles: Role[];
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
  items: LexicalListItemVO[];
}

export interface CsvImportTemplateFieldVO {
  key: string;
  label: string;
  required: boolean;
  description?: string | null;
}

export interface CsvImportTemplateVO {
  fields: CsvImportTemplateFieldVO[];
  headerLine: string;
  exampleLine: string;
}

export interface CsvImportFailureVO {
  lineNo: number;
  message: string;
}

export interface CsvImportResultVO {
  successCount: number;
  failedCount: number;
  failures: CsvImportFailureVO[];
}

export interface AddLexicalListItemsResultVO {
  lexicalListId: number;
  addedCount: number;
  skippedCount: number;
  duplicatedPairIds: number[];
}

export interface DiagnosisTemplateUpsertRequest {
  templateName: string;
  description?: string;
  status: string;
  estimatedDurationMinutes: number;
  scoringVersion: string;
  items: DiagnosisTemplateItemVO[];
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
