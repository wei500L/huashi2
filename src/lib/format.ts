import type { AnalyticsHeatmapVO, AnalyticsRadarMetricVO, AnalyticsScatterVO, AnalyticsTrendVO, Capability, CurrentUserVO } from './contracts';
import type { AppChartOption } from './echarts';

const EMPTY_LABEL = '--';

const LEXICAL_PAIR_TYPE_LABELS: Record<string, string> = {
  COGNATE: '同源词',
  FALSE_FRIEND: '同形异义词',
  PARTIAL_COGNATE: '部分同源词',
  ORTHOGRAPHIC_SIMILAR: '形近词',
};

const CONTEXT_LEVEL_LABELS: Record<string, string> = {
  LOW: '低语境',
  MEDIUM: '中语境',
  HIGH: '高语境',
};

const RISK_LEVEL_LABELS: Record<string, string> = {
  LOW: '低风险',
  MEDIUM: '中风险',
  HIGH: '高风险',
  CRITICAL: '极高风险',
};

const TRAINING_MODE_LABELS: Record<string, string> = {
  COGNATE_BOOST: '强化：同源词迁移',
  FALSE_FRIEND_DISCRIM: '纠偏：同形异义词辨析',
  CONTEXT_FIX: '修复：语境纠偏',
  SPEED_CHALLENGE: '提速：快速识别',
};

const ERROR_TYPE_LABELS: Record<string, string> = {
  FALSE_FRIEND_CONFUSION: '假朋友混淆',
  CONTEXT_IGNORED: '忽略语境',
  OVER_TRANSFER: '过度迁移',
  UNDER_TRANSFER: '迁移不足',
  ORTHOGRAPHIC_INTERFERENCE: '形近干扰',
  SEMANTIC_MISFIRE: '语义误判',
};

const ROLE_LABELS: Record<string, string> = {
  ADMIN: '管理员',
  TEACHER: '教师',
  STUDENT: '学生',
};

const WORKSPACE_LABELS: Record<Capability, string> = {
  ADMIN_CONSOLE: '管理后台',
  TEACHING_WORKSPACE: '教师工作台',
  STUDENT_WORKSPACE: '学生工作台',
};

const DIAGNOSIS_TEMPLATE_SYNC_STATE_LABELS: Record<string, string> = {
  DIRTY: '待同步',
  SYNCED: '已同步',
};

const INTERVENTION_PRIORITY_LABELS: Record<string, string> = {
  LOW: '低优先级',
  NORMAL: '常规',
  URGENT: '紧急',
};

const INTERVENTION_STATUS_LABELS: Record<string, string> = {
  PENDING: '待处理',
  IN_PROGRESS: '处理中',
  COMPLETED: '已完成',
  OVERDUE: '已逾期',
};

const ASSESSMENT_ATTEMPT_STATUS_LABELS: Record<string, string> = {
  NOT_STARTED: '待开始',
  IN_PROGRESS: '进行中',
  SUBMITTED: '已提交',
};

const INVITATION_STATUS_LABELS: Record<string, string> = {
  PENDING: '待激活',
  CONSUMED: '已使用',
  EXPIRED: '已过期',
  NONE: '无邀请链接',
};

const PROFILE_LINK_STATUS_LABELS: Record<string, string> = {
  UNLINKED: '未关联档案',
  LINKED: '已关联档案',
  STUDENT_ONLY: '仅关联学生档案',
  TEACHER_ONLY: '仅关联教师档案',
  BOTH: '已关联学生/教师档案',
};

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
    return EMPTY_LABEL;
  }
  return labels[normalized] ?? unknownLabel;
}

export function formatPercent(value: number, digits = 0): string {
  return `${(value * 100).toFixed(digits)}%`;
}

export function formatMaybePercent(value?: number | null, digits = 0): string {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return '--';
  }
  return formatPercent(value, digits);
}

export function formatMs(value?: number | null): string {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return '--';
  }
  return `${Math.round(value)}ms`;
}

export function formatDateTime(value?: string | null): string {
  if (!value) {
    return '--';
  }
  return new Date(value).toLocaleString('zh-CN', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export function formatDate(value?: string | null): string {
  if (!value) {
    return '--';
  }
  return new Date(value).toLocaleDateString('zh-CN', {
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
  return mapDisplayValue(type, LEXICAL_PAIR_TYPE_LABELS, '未定义词对类型');
}

export function contextLevelLabel(level?: string | null): string {
  return mapDisplayValue(level, CONTEXT_LEVEL_LABELS, '未定义语境等级');
}

export function riskLevelLabel(level?: string | null): string {
  return mapDisplayValue(level, RISK_LEVEL_LABELS, '未知风险等级');
}

export function trainingModeLabel(mode?: string | null): string {
  return mapDisplayValue(mode, TRAINING_MODE_LABELS, '未定义训练模式');
}

export function errorTypeLabel(errorType?: string | null): string {
  if (!errorType) {
    return '未标注错误';
  }
  return mapDisplayValue(errorType, ERROR_TYPE_LABELS, '未知错误类型');
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
  return mapDisplayValue(role, ROLE_LABELS, '未知角色');
}

export function workspaceCapabilityLabel(capability?: Capability | null): string {
  return mapDisplayValue(capability, WORKSPACE_LABELS, '未知工作台');
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

export function diagnosisTemplateSyncStateLabel(syncState?: string | null): string {
  return mapDisplayValue(syncState, DIAGNOSIS_TEMPLATE_SYNC_STATE_LABELS, '未知同步状态');
}

export function interventionPriorityLabel(priority?: string | null): string {
  return mapDisplayValue(priority, INTERVENTION_PRIORITY_LABELS, '未知优先级');
}

export function interventionStatusLabel(status?: string | null): string {
  return mapDisplayValue(status, INTERVENTION_STATUS_LABELS, '未知干预状态');
}

export function assessmentAttemptStatusLabel(status?: string | null): string {
  return mapDisplayValue(status, ASSESSMENT_ATTEMPT_STATUS_LABELS, '未知作答状态');
}

export function invitationStatusLabel(status?: string | null): string {
  return mapDisplayValue(status, INVITATION_STATUS_LABELS, '未知邀请状态');
}

export function profileLinkStatusLabel(status?: string | null): string {
  return mapDisplayValue(status, PROFILE_LINK_STATUS_LABELS, '未知关联状态');
}

export function sessionActivityLabel(hasActiveSession?: boolean | null): string {
  if (hasActiveSession === undefined || hasActiveSession === null) {
    return EMPTY_LABEL;
  }
  return hasActiveSession ? '活跃中' : '无活动会话';
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
  return {
    backgroundColor: 'transparent',
    tooltip: {
      formatter: (params) => {
        const dataCarrier = (Array.isArray(params) ? params[0] : params) as { data?: unknown };
        const data = Array.isArray(dataCarrier.data)
          ? (dataCarrier.data as [number, number, number, number, number])
          : [0, 0, 0, 0, 0];
        const [xIndex, yIndex, value, accuracy, avgRt] = data;
        return `${heatmap.xAxis[xIndex]} / ${heatmap.yAxis[yIndex]}<br/>样本数: ${value}<br/>正确率: ${formatPercent(Number(accuracy), 0)}<br/>平均反应时: ${formatMs(Number(avgRt))}`;
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
  return {
    backgroundColor: 'transparent',
    tooltip: {
      formatter: (params) => {
        const dataCarrier = (Array.isArray(params) ? params[0] : params) as { data?: unknown };
        const data = Array.isArray(dataCarrier.data)
          ? (dataCarrier.data as [number, number, number, number, string])
          : [0, 0, 0, 0, ''];
        const [x, y, attempts, risk, label] = data;
        return `${label}<br/>${scatter.x}: ${formatMs(Number(x))}<br/>${scatter.y}: ${formatPercent(Number(y), 0)}<br/>尝试次数: ${Number(attempts)}<br/>风险: ${formatPercent(Number(risk), 0)}`;
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
