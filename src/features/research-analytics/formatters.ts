import type { ResearchRateVO } from '@/lib/contracts';

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
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  if (minutes === 0) return `${seconds}s`;
  return `${minutes}m ${seconds}s`;
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
