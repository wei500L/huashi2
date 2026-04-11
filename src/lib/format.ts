import type { AnalyticsHeatmapVO, AnalyticsRadarMetricVO, AnalyticsScatterVO, AnalyticsTrendVO, Capability, CurrentUserVO } from './contracts';
import type { AppChartOption } from './echarts';
import i18n from './i18n';
import { DEFAULT_LOCALE, type SupportedLocale } from './locale';

type StatusTone = 'neutral' | 'info' | 'success' | 'warning' | 'danger';

const COPY: Record<
  SupportedLocale,
  {
    emptyLabel: string;
    lexicalPairTypeLabels: Record<string, string>;
    contextLevelLabels: Record<string, string>;
    riskLevelLabels: Record<string, string>;
    trainingModeLabels: Record<string, string>;
    diagnosisTaskTypeLabels: Record<string, string>;
    errorTypeLabels: Record<string, string>;
    roleLabels: Record<string, string>;
    workspaceLabels: Record<Capability, string>;
    diagnosisSessionStatusLabels: Record<string, string>;
    trainingSessionStatusLabels: Record<string, string>;
    diagnosisTemplateSyncStateLabels: Record<string, string>;
    interventionPriorityLabels: Record<string, string>;
    interventionStatusLabels: Record<string, string>;
    assessmentAttemptStatusLabels: Record<string, string>;
    assessmentPaperStatusLabels: Record<string, string>;
    assessmentQuestionTypeLabels: Record<string, string>;
    lexicalImportBatchStatusLabels: Record<string, string>;
    lexicalImportRowStatusLabels: Record<string, string>;
    embeddingStatusLabels: Record<string, string>;
    invitationStatusLabels: Record<string, string>;
    profileLinkStatusLabels: Record<string, string>;
    unknown: Record<string, string>;
    sessionActivity: {
      active: string;
      inactive: string;
    };
    tooltip: {
      sampleCount: string;
      accuracy: string;
      averageReactionTime: string;
      attempts: string;
      risk: string;
    };
  }
> = {
  'zh-CN': {
    emptyLabel: '--',
    lexicalPairTypeLabels: {
      COGNATE: '同源词',
      FALSE_FRIEND: '同形异义词',
      PARTIAL_COGNATE: '部分同源词',
      ORTHOGRAPHIC_SIMILAR: '形近词',
    },
    contextLevelLabels: {
      LOW: '低语境',
      MEDIUM: '中语境',
      HIGH: '高语境',
    },
    riskLevelLabels: {
      LOW: '低风险',
      MEDIUM: '中风险',
      HIGH: '高风险',
      CRITICAL: '极高风险',
    },
    trainingModeLabels: {
      COGNATE_BOOST: '强化：同源词迁移',
      FALSE_FRIEND_DISCRIM: '纠偏：同形异义词辨析',
      CONTEXT_FIX: '修复：语境纠偏',
      SPEED_CHALLENGE: '提速：快速识别',
    },
    diagnosisTaskTypeLabels: {
      REACTION_TIME: '反应时判断',
      SEMANTIC_JUDGEMENT: '语义判断',
    },
    errorTypeLabels: {
      FALSE_FRIEND_CONFUSION: '假朋友混淆',
      CONTEXT_IGNORED: '忽略语境',
      OVER_TRANSFER: '过度迁移',
      UNDER_TRANSFER: '迁移不足',
      ORTHOGRAPHIC_INTERFERENCE: '形近干扰',
      SEMANTIC_MISFIRE: '语义误判',
    },
    roleLabels: {
      ADMIN: '管理员',
      TEACHER: '教师',
      STUDENT: '学生',
    },
    workspaceLabels: {
      ADMIN_CONSOLE: '管理后台',
      TEACHING_WORKSPACE: '教师工作台',
      STUDENT_WORKSPACE: '学生工作台',
    },
    diagnosisSessionStatusLabels: {
      IN_PROGRESS: '进行中',
      COMPLETED: '已完成',
    },
    trainingSessionStatusLabels: {
      IN_PROGRESS: '进行中',
      COMPLETED: '已完成',
    },
    diagnosisTemplateSyncStateLabels: {
      DIRTY: '待同步',
      SYNCED: '已同步',
    },
    interventionPriorityLabels: {
      LOW: '低优先级',
      NORMAL: '常规',
      URGENT: '紧急',
    },
    interventionStatusLabels: {
      PENDING: '待处理',
      IN_PROGRESS: '处理中',
      COMPLETED: '已完成',
      OVERDUE: '已逾期',
    },
    assessmentAttemptStatusLabels: {
      NOT_STARTED: '待开始',
      IN_PROGRESS: '进行中',
      SUBMITTED: '已提交',
    },
    assessmentPaperStatusLabels: {
      DRAFT: '草稿',
      PUBLISHED: '已发布',
    },
    assessmentQuestionTypeLabels: {
      SINGLE_CHOICE: '单选题',
      MULTIPLE_CHOICE: '多选题',
      FILL_BLANK: '填空题',
    },
    lexicalImportBatchStatusLabels: {
      PARSING: '解析中',
      DRAFT: '待确认',
      IMPORTING: '导入中',
      COMPLETED: '已完成',
      FAILED: '失败',
    },
    lexicalImportRowStatusLabels: {
      READY: '可导入',
      INVALID: '需修正',
      SKIPPED: '已跳过',
      IMPORTED: '已导入',
    },
    embeddingStatusLabels: {
      PENDING: '待嵌入',
      EMBEDDED: '已嵌入',
      FAILED: '嵌入失败',
    },
    invitationStatusLabels: {
      PENDING: '待激活',
      CONSUMED: '已使用',
      EXPIRED: '已过期',
      NONE: '无邀请链接',
    },
    profileLinkStatusLabels: {
      UNLINKED: '未关联档案',
      LINKED: '已关联档案',
      STUDENT_ONLY: '仅关联学生档案',
      TEACHER_ONLY: '仅关联教师档案',
      BOTH: '已关联学生/教师档案',
    },
    unknown: {
      lexicalPairType: '未定义词对类型',
      contextLevel: '未定义语境等级',
      riskLevel: '未知风险等级',
      trainingMode: '未定义训练模式',
      diagnosisTaskType: '未知诊断题型',
      errorType: '未知错误类型',
      unmarkedErrorType: '未标注错误',
      role: '未知角色',
      workspace: '未知工作台',
      diagnosisSessionStatus: '未知诊断状态',
      trainingSessionStatus: '未知训练状态',
      syncState: '未知同步状态',
      priority: '未知优先级',
      interventionStatus: '未知干预状态',
      assessmentAttemptStatus: '未知作答状态',
      assessmentPaperStatus: '未知试卷状态',
      questionType: '未知题型',
      lexicalImportBatchStatus: '未知导入批次状态',
      lexicalImportRowStatus: '未知导入行状态',
      embeddingStatus: '未知向量状态',
      invitationStatus: '未知邀请状态',
      profileLinkStatus: '未知关联状态',
    },
    sessionActivity: {
      active: '活跃中',
      inactive: '无活动会话',
    },
    tooltip: {
      sampleCount: '样本数',
      accuracy: '正确率',
      averageReactionTime: '平均反应时',
      attempts: '尝试次数',
      risk: '风险',
    },
  },
  'en-US': {
    emptyLabel: '--',
    lexicalPairTypeLabels: {
      COGNATE: 'Cognate',
      FALSE_FRIEND: 'False friend',
      PARTIAL_COGNATE: 'Partial cognate',
      ORTHOGRAPHIC_SIMILAR: 'Orthographic similar',
    },
    contextLevelLabels: {
      LOW: 'Low context',
      MEDIUM: 'Medium context',
      HIGH: 'High context',
    },
    riskLevelLabels: {
      LOW: 'Low risk',
      MEDIUM: 'Medium risk',
      HIGH: 'High risk',
      CRITICAL: 'Critical risk',
    },
    trainingModeLabels: {
      COGNATE_BOOST: 'Boost: cognate transfer',
      FALSE_FRIEND_DISCRIM: 'Correct: false-friend discrimination',
      CONTEXT_FIX: 'Repair: context correction',
      SPEED_CHALLENGE: 'Speed: rapid recognition',
    },
    diagnosisTaskTypeLabels: {
      REACTION_TIME: 'Reaction-time check',
      SEMANTIC_JUDGEMENT: 'Semantic judgement',
    },
    errorTypeLabels: {
      FALSE_FRIEND_CONFUSION: 'False-friend confusion',
      CONTEXT_IGNORED: 'Context ignored',
      OVER_TRANSFER: 'Over-transfer',
      UNDER_TRANSFER: 'Under-transfer',
      ORTHOGRAPHIC_INTERFERENCE: 'Orthographic interference',
      SEMANTIC_MISFIRE: 'Semantic misfire',
    },
    roleLabels: {
      ADMIN: 'Administrator',
      TEACHER: 'Teacher',
      STUDENT: 'Student',
    },
    workspaceLabels: {
      ADMIN_CONSOLE: 'Admin console',
      TEACHING_WORKSPACE: 'Teacher workspace',
      STUDENT_WORKSPACE: 'Student workspace',
    },
    diagnosisSessionStatusLabels: {
      IN_PROGRESS: 'In progress',
      COMPLETED: 'Completed',
    },
    trainingSessionStatusLabels: {
      IN_PROGRESS: 'In progress',
      COMPLETED: 'Completed',
    },
    diagnosisTemplateSyncStateLabels: {
      DIRTY: 'Needs sync',
      SYNCED: 'Synced',
    },
    interventionPriorityLabels: {
      LOW: 'Low priority',
      NORMAL: 'Standard',
      URGENT: 'Urgent',
    },
    interventionStatusLabels: {
      PENDING: 'Pending',
      IN_PROGRESS: 'In progress',
      COMPLETED: 'Completed',
      OVERDUE: 'Overdue',
    },
    assessmentAttemptStatusLabels: {
      NOT_STARTED: 'Not started',
      IN_PROGRESS: 'In progress',
      SUBMITTED: 'Submitted',
    },
    assessmentPaperStatusLabels: {
      DRAFT: 'Draft',
      PUBLISHED: 'Published',
    },
    assessmentQuestionTypeLabels: {
      SINGLE_CHOICE: 'Single choice',
      MULTIPLE_CHOICE: 'Multiple choice',
      FILL_BLANK: 'Fill in the blank',
    },
    lexicalImportBatchStatusLabels: {
      PARSING: 'Parsing',
      DRAFT: 'Needs review',
      IMPORTING: 'Importing',
      COMPLETED: 'Completed',
      FAILED: 'Failed',
    },
    lexicalImportRowStatusLabels: {
      READY: 'Ready to import',
      INVALID: 'Needs correction',
      SKIPPED: 'Skipped',
      IMPORTED: 'Imported',
    },
    embeddingStatusLabels: {
      PENDING: 'Pending embedding',
      EMBEDDED: 'Embedded',
      FAILED: 'Embedding failed',
    },
    invitationStatusLabels: {
      PENDING: 'Pending activation',
      CONSUMED: 'Used',
      EXPIRED: 'Expired',
      NONE: 'No invitation link',
    },
    profileLinkStatusLabels: {
      UNLINKED: 'No profile linked',
      LINKED: 'Profile linked',
      STUDENT_ONLY: 'Student profile only',
      TEACHER_ONLY: 'Teacher profile only',
      BOTH: 'Student and teacher profiles linked',
    },
    unknown: {
      lexicalPairType: 'Unknown pair type',
      contextLevel: 'Unknown context level',
      riskLevel: 'Unknown risk level',
      trainingMode: 'Unknown training mode',
      diagnosisTaskType: 'Unknown diagnosis task type',
      errorType: 'Unknown error type',
      unmarkedErrorType: 'Unlabeled error',
      role: 'Unknown role',
      workspace: 'Unknown workspace',
      diagnosisSessionStatus: 'Unknown diagnosis status',
      trainingSessionStatus: 'Unknown training status',
      syncState: 'Unknown sync state',
      priority: 'Unknown priority',
      interventionStatus: 'Unknown intervention status',
      assessmentAttemptStatus: 'Unknown attempt status',
      assessmentPaperStatus: 'Unknown paper status',
      questionType: 'Unknown question type',
      lexicalImportBatchStatus: 'Unknown import batch status',
      lexicalImportRowStatus: 'Unknown import row status',
      embeddingStatus: 'Unknown embedding status',
      invitationStatus: 'Unknown invitation status',
      profileLinkStatus: 'Unknown link status',
    },
    sessionActivity: {
      active: 'Active',
      inactive: 'No active session',
    },
    tooltip: {
      sampleCount: 'Samples',
      accuracy: 'Accuracy',
      averageReactionTime: 'Avg reaction time',
      attempts: 'Attempts',
      risk: 'Risk',
    },
  },
};

function getActiveLocale(): SupportedLocale {
  const locale = i18n.resolvedLanguage ?? i18n.language;
  return locale === 'en-US' ? 'en-US' : DEFAULT_LOCALE;
}

function localeCopy() {
  return COPY[getActiveLocale()];
}

function normalizeDisplayKey(value?: string | null): string | null {
  if (!value) {
    return null;
  }
  const normalized = value.trim().toUpperCase();
  return normalized || null;
}

function mapDisplayValue(value: string | null | undefined, labels: Record<string, string>, unknownLabel: string): string {
  const normalized = normalizeDisplayKey(value);
  if (!normalized) {
    return localeCopy().emptyLabel;
  }
  return labels[normalized] ?? unknownLabel;
}

export function formatPercent(value: number, digits = 0): string {
  return `${(value * 100).toFixed(digits)}%`;
}

export function formatMaybePercent(value?: number | null, digits = 0): string {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return localeCopy().emptyLabel;
  }
  return formatPercent(value, digits);
}

export function formatMs(value?: number | null): string {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return localeCopy().emptyLabel;
  }
  return `${Math.round(value)} ms`;
}

export function formatDateTime(value?: string | null): string {
  if (!value) {
    return localeCopy().emptyLabel;
  }
  return new Date(value).toLocaleString(getActiveLocale(), {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export function formatDate(value?: string | null): string {
  if (!value) {
    return localeCopy().emptyLabel;
  }
  return new Date(value).toLocaleDateString(getActiveLocale(), {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
}

export function hasCapability(capabilities: Capability[] | null | undefined, capability: Capability): boolean {
  return Array.isArray(capabilities) && capabilities.includes(capability);
}

export function userHasCapability(user: Pick<CurrentUserVO, 'capabilities'> | null | undefined, capability: Capability): boolean {
  return hasCapability(user?.capabilities, capability);
}

export function homePathForCapabilities(capabilities?: Capability[] | null): string {
  if (hasCapability(capabilities, 'ADMIN_CONSOLE')) {
    return '/admin/users';
  }
  if (hasCapability(capabilities, 'TEACHING_WORKSPACE')) {
    return '/teacher/workspace';
  }
  return '/dashboard';
}

export function lexicalPairTypeLabel(type?: string | null): string {
  const copy = localeCopy();
  return mapDisplayValue(type, copy.lexicalPairTypeLabels, copy.unknown.lexicalPairType);
}

export function contextLevelLabel(level?: string | null): string {
  const copy = localeCopy();
  return mapDisplayValue(level, copy.contextLevelLabels, copy.unknown.contextLevel);
}

export function riskLevelLabel(level?: string | null): string {
  const copy = localeCopy();
  return mapDisplayValue(level, copy.riskLevelLabels, copy.unknown.riskLevel);
}

export function trainingModeLabel(mode?: string | null): string {
  const copy = localeCopy();
  return mapDisplayValue(mode, copy.trainingModeLabels, copy.unknown.trainingMode);
}

export function diagnosisTaskTypeLabel(taskType?: string | null): string {
  const copy = localeCopy();
  return mapDisplayValue(taskType, copy.diagnosisTaskTypeLabels, copy.unknown.diagnosisTaskType);
}

export function errorTypeLabel(errorType?: string | null): string {
  const copy = localeCopy();
  if (!errorType) {
    return copy.unknown.unmarkedErrorType;
  }
  return mapDisplayValue(errorType, copy.errorTypeLabels, copy.unknown.errorType);
}

export function riskTone(level?: string | null): string {
  switch (level) {
    case 'CRITICAL':
    case 'HIGH':
    case 'URGENT':
      return 'text-rose-500';
    case 'MEDIUM':
    case 'NORMAL':
      return 'text-amber-500';
    default:
      return 'text-emerald-500';
  }
}

export function roleLabel(role?: string | null): string {
  const copy = localeCopy();
  return mapDisplayValue(role, copy.roleLabels, copy.unknown.role);
}

export function workspaceCapabilityLabel(capability?: Capability | null): string {
  const copy = localeCopy();
  return mapDisplayValue(capability, copy.workspaceLabels, copy.unknown.workspace);
}

export function workspaceLabels(capabilities?: Capability[] | null): string[] {
  const labels: string[] = [];
  if (hasCapability(capabilities, 'ADMIN_CONSOLE')) {
    labels.push(workspaceCapabilityLabel('ADMIN_CONSOLE'));
  }
  if (hasCapability(capabilities, 'TEACHING_WORKSPACE')) {
    labels.push(workspaceCapabilityLabel('TEACHING_WORKSPACE'));
  }
  if (hasCapability(capabilities, 'STUDENT_WORKSPACE')) {
    labels.push(workspaceCapabilityLabel('STUDENT_WORKSPACE'));
  }
  return labels;
}

export function diagnosisSessionStatusLabel(status?: string | null): string {
  const copy = localeCopy();
  return mapDisplayValue(status, copy.diagnosisSessionStatusLabels, copy.unknown.diagnosisSessionStatus);
}

export function trainingSessionStatusLabel(status?: string | null): string {
  const copy = localeCopy();
  return mapDisplayValue(status, copy.trainingSessionStatusLabels, copy.unknown.trainingSessionStatus);
}

export function diagnosisTemplateSyncStateLabel(syncState?: string | null): string {
  const copy = localeCopy();
  return mapDisplayValue(syncState, copy.diagnosisTemplateSyncStateLabels, copy.unknown.syncState);
}

export function interventionPriorityLabel(priority?: string | null): string {
  const copy = localeCopy();
  return mapDisplayValue(priority, copy.interventionPriorityLabels, copy.unknown.priority);
}

export function interventionStatusLabel(status?: string | null): string {
  const copy = localeCopy();
  return mapDisplayValue(status, copy.interventionStatusLabels, copy.unknown.interventionStatus);
}

export function assessmentAttemptStatusLabel(status?: string | null): string {
  const copy = localeCopy();
  return mapDisplayValue(status, copy.assessmentAttemptStatusLabels, copy.unknown.assessmentAttemptStatus);
}

export function assessmentPaperStatusLabel(status?: string | null): string {
  const copy = localeCopy();
  return mapDisplayValue(status, copy.assessmentPaperStatusLabels, copy.unknown.assessmentPaperStatus);
}

export function assessmentQuestionTypeLabel(type?: string | null): string {
  const copy = localeCopy();
  return mapDisplayValue(type, copy.assessmentQuestionTypeLabels, copy.unknown.questionType);
}

export function lexicalImportBatchStatusLabel(status?: string | null): string {
  const copy = localeCopy();
  return mapDisplayValue(status, copy.lexicalImportBatchStatusLabels, copy.unknown.lexicalImportBatchStatus);
}

export function lexicalImportRowStatusLabel(status?: string | null): string {
  const copy = localeCopy();
  return mapDisplayValue(status, copy.lexicalImportRowStatusLabels, copy.unknown.lexicalImportRowStatus);
}

export function embeddingStatusLabel(status?: string | null): string {
  const copy = localeCopy();
  return mapDisplayValue(status, copy.embeddingStatusLabels, copy.unknown.embeddingStatus);
}

export function invitationStatusLabel(status?: string | null): string {
  const copy = localeCopy();
  return mapDisplayValue(status, copy.invitationStatusLabels, copy.unknown.invitationStatus);
}

export function profileLinkStatusLabel(status?: string | null): string {
  const copy = localeCopy();
  return mapDisplayValue(status, copy.profileLinkStatusLabels, copy.unknown.profileLinkStatus);
}

export function sessionActivityLabel(hasActiveSession?: boolean | null): string {
  const copy = localeCopy();
  if (hasActiveSession === undefined || hasActiveSession === null) {
    return copy.emptyLabel;
  }
  return hasActiveSession ? copy.sessionActivity.active : copy.sessionActivity.inactive;
}

export function diagnosisTemplateSyncStateTone(syncState?: string | null): StatusTone {
  return normalizeDisplayKey(syncState) === 'SYNCED' ? 'success' : 'warning';
}

export function diagnosisSessionStatusTone(status?: string | null): StatusTone {
  return normalizeDisplayKey(status) === 'COMPLETED' ? 'success' : 'warning';
}

export function trainingSessionStatusTone(status?: string | null): StatusTone {
  return normalizeDisplayKey(status) === 'COMPLETED' ? 'success' : 'warning';
}

export function assessmentAttemptStatusTone(status?: string | null): StatusTone {
  switch (normalizeDisplayKey(status)) {
    case 'SUBMITTED':
      return 'success';
    case 'IN_PROGRESS':
      return 'warning';
    default:
      return 'neutral';
  }
}

export function assessmentPaperStatusTone(status?: string | null): StatusTone {
  return normalizeDisplayKey(status) === 'PUBLISHED' ? 'success' : 'warning';
}

export function interventionStatusTone(status?: string | null): StatusTone {
  switch (normalizeDisplayKey(status)) {
    case 'COMPLETED':
      return 'success';
    case 'IN_PROGRESS':
      return 'info';
    case 'OVERDUE':
      return 'danger';
    default:
      return 'warning';
  }
}

export function lexicalImportBatchStatusTone(status?: string | null): StatusTone {
  switch (normalizeDisplayKey(status)) {
    case 'COMPLETED':
      return 'success';
    case 'FAILED':
      return 'danger';
    case 'PARSING':
    case 'IMPORTING':
      return 'info';
    default:
      return 'warning';
  }
}

export function lexicalImportRowStatusTone(status?: string | null): StatusTone {
  switch (normalizeDisplayKey(status)) {
    case 'READY':
    case 'IMPORTED':
      return 'success';
    case 'INVALID':
      return 'danger';
    default:
      return 'neutral';
  }
}

export function embeddingStatusTone(status?: string | null): StatusTone {
  switch (normalizeDisplayKey(status)) {
    case 'EMBEDDED':
      return 'success';
    case 'FAILED':
      return 'danger';
    default:
      return 'warning';
  }
}

export function buildTrendOption(trend?: AnalyticsTrendVO | null): AppChartOption {
  if (!trend) {
    return {};
  }
  return {
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis' },
    legend: { textStyle: { color: '#94a3b8' }, bottom: 0 },
    grid: { left: '4%', right: '4%', top: '8%', bottom: '18%', containLabel: true },
    xAxis: {
      type: 'category',
      data: trend.xAxis,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#94a3b8' },
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: 'rgba(148,163,184,0.12)' } },
      axisLabel: { color: '#94a3b8' },
    },
    series: trend.series.map((series, index) => {
      if (index === 0) {
        return {
          name: series.label,
          type: 'line',
          smooth: true,
          symbol: 'none',
          data: series.values,
        };
      }
      return {
        name: series.label,
        type: 'bar',
        data: series.values,
      };
    }),
  };
}

export function buildRadarOption(radar?: AnalyticsRadarMetricVO[] | null): AppChartOption {
  if (!radar?.length) {
    return {};
  }
  return {
    backgroundColor: 'transparent',
    radar: {
      indicator: radar.map((item) => ({ name: item.label, max: item.max })),
      axisName: { color: '#94a3b8' },
      splitArea: { areaStyle: { color: ['rgba(255,255,255,0.03)', 'rgba(255,255,255,0.06)'] } },
      splitLine: { lineStyle: { color: 'rgba(148,163,184,0.15)' } },
    },
    series: [
      {
        type: 'radar',
        data: [
          {
            value: radar.map((item) => item.value),
            areaStyle: { color: 'rgba(59,130,246,0.18)' },
            lineStyle: { color: '#3b82f6', width: 2 },
            symbol: 'none',
          },
        ],
      },
    ],
  };
}

export function buildHeatmapOption(heatmap?: AnalyticsHeatmapVO | null): AppChartOption {
  if (!heatmap) {
    return {};
  }
  const tooltipCopy = localeCopy().tooltip;
  return {
    backgroundColor: 'transparent',
    tooltip: {
      formatter: (params) => {
        const dataCarrier = (Array.isArray(params) ? params[0] : params) as { data?: unknown };
        const data = Array.isArray(dataCarrier.data)
          ? (dataCarrier.data as [number, number, number, number, number])
          : [0, 0, 0, 0, 0];
        const [xIndex, yIndex, value, accuracy, avgRt] = data;
        return `${heatmap.xAxis[xIndex]} / ${heatmap.yAxis[yIndex]}<br/>${tooltipCopy.sampleCount}: ${value}<br/>${tooltipCopy.accuracy}: ${formatPercent(Number(accuracy), 0)}<br/>${tooltipCopy.averageReactionTime}: ${formatMs(Number(avgRt))}`;
      },
    },
    grid: { left: '6%', right: '6%', top: '8%', bottom: '12%', containLabel: true },
    xAxis: { type: 'category', data: heatmap.xAxis, axisLabel: { color: '#94a3b8' } },
    yAxis: { type: 'category', data: heatmap.yAxis, axisLabel: { color: '#94a3b8' } },
    visualMap: {
      min: 0,
      max: Math.max(1, ...heatmap.cells.map((item) => item.value)),
      calculable: true,
      orient: 'horizontal',
      left: 'center',
      bottom: 0,
      textStyle: { color: '#94a3b8' },
    },
    series: [
      {
        type: 'heatmap',
        data: heatmap.cells.map((item) => [
          heatmap.xAxis.indexOf(item.xKey),
          heatmap.yAxis.indexOf(item.yKey),
          item.value,
          item.accuracy,
          item.avgReactionTimeMs,
        ]),
        label: { show: true, color: '#fff' },
      },
    ],
  };
}

export function buildScatterOption(scatter?: AnalyticsScatterVO | null): AppChartOption {
  if (!scatter) {
    return {};
  }
  const tooltipCopy = localeCopy().tooltip;
  return {
    backgroundColor: 'transparent',
    tooltip: {
      formatter: (params) => {
        const dataCarrier = (Array.isArray(params) ? params[0] : params) as { data?: unknown };
        const data = Array.isArray(dataCarrier.data)
          ? (dataCarrier.data as [number, number, number, number, string])
          : [0, 0, 0, 0, ''];
        const [x, y, attempts, risk, label] = data;
        return `${label}<br/>${scatter.x}: ${formatMs(Number(x))}<br/>${scatter.y}: ${formatPercent(Number(y), 0)}<br/>${tooltipCopy.attempts}: ${Number(attempts)}<br/>${tooltipCopy.risk}: ${formatPercent(Number(risk), 0)}`;
      },
    },
    xAxis: { name: scatter.x, axisLabel: { color: '#94a3b8' } },
    yAxis: { name: scatter.y, axisLabel: { color: '#94a3b8' }, max: 1 },
    series: [
      {
        type: 'scatter',
        data: scatter.points.map((point) => [
          point.avgReactionTimeMs,
          point.accuracy,
          point.attemptCount,
          point.riskScore,
          point.label,
        ]),
        symbolSize: (data: [number, number, number]) => Math.max(10, Math.min(30, data[2] * 2)),
      },
    ],
  };
}
