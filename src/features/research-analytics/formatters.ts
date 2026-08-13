import type { ResearchRateVO } from '@/lib/contracts';

const QUALITY_FLAG_LABELS: Record<string, string> = {
  FAST_ITEM: '过快作答',
  SHORT_TOTAL_DURATION: '总时长过短',
  TIMING_GAP: '计时缺失',
};

const AI_STATUS_LABELS: Record<string, string> = {
  PENDING: '排队中',
  PROCESSING: '生成中',
  COMPLETED: '已完成',
  FAILED: '失败',
  FALLBACK: '规则摘要',
};

const SCAN_STATUS_LABELS: Record<string, string> = {
  PENDING: '类型校验中',
  CLEAN: '类型校验通过',
  INFECTED: '类型不匹配',
  FAILED: '类型校验失败',
};

const PARTICIPANT_TYPE_LABELS: Record<string, string> = {
  PUBLIC_CODE: '参与码进入',
  PUBLIC_QR: '二维码进入',
};

const SUBMIT_REASON_LABELS: Record<string, string> = {
  MANUAL: '主动提交',
  TIMEOUT: '超时提交',
  SCHEDULER: '系统提交',
};

export function formatRate(rate?: ResearchRateVO | null): string {
  if (!rate || rate.value == null || rate.denominator === 0) return '—';
  return `${Math.round(rate.value * 1000) / 10}%`;
}

export function formatRateHint(rate?: ResearchRateVO | null, unit = '份'): string {
  if (!rate) return '口径待返回';
  return `${rate.numerator} / ${rate.denominator} ${unit}`;
}

export function formatDuration(ms?: number | null): string {
  if (ms == null) return '—';
  const totalSeconds = Math.max(0, Math.round(ms / 1000));
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;
  if (hours > 0) return `${hours}小时${minutes}分`;
  if (minutes === 0) return `${seconds}秒`;
  if (seconds === 0) return `${minutes}分钟`;
  return `${minutes}分${seconds}秒`;
}

export function formatScore(value?: number | null): string {
  if (value == null) return '—';
  return `${Math.round(value * 10) / 10}`;
}

export function formatFileSize(bytes?: number | null): string {
  if (bytes == null) return '—';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 102.4) / 10} KB`;
  return `${Math.round(bytes / 1024 / 102.4) / 10} MB`;
}

export function qualityFlagLabel(flag?: string | null): string {
  if (!flag) return '—';
  return QUALITY_FLAG_LABELS[flag] || flag;
}

export function qualityFlagLabels(flags?: string[] | null): string[] {
  return (flags || []).map((flag) => qualityFlagLabel(flag));
}

export function aiAnalysisStatusLabel(status?: string | null): string {
  if (!status) return '未生成';
  return AI_STATUS_LABELS[status] || status;
}

export function scanStatusLabel(status?: string | null): string {
  if (!status) return '—';
  return SCAN_STATUS_LABELS[status] || status;
}

export function participantTypeLabel(type?: string | null): string {
  if (!type) return '公开进入';
  return PARTICIPANT_TYPE_LABELS[type] || type;
}

export function submitReasonLabel(reason?: string | null): string {
  if (!reason) return '—';
  return SUBMIT_REASON_LABELS[reason] || reason;
}
