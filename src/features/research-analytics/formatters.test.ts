import { describe, expect, it } from 'vitest';
import {
  aiAnalysisStatusLabel,
  formatDuration,
  qualityFlagLabel,
  scanStatusLabel,
  submitReasonLabel,
} from './formatters';

describe('research analytics formatters', () => {
  it('formats duration in Chinese', () => {
    expect(formatDuration(8000)).toBe('8秒');
    expect(formatDuration(120000)).toBe('2分钟');
    expect(formatDuration(150000)).toBe('2分30秒');
  });

  it('maps research enums to Chinese labels', () => {
    expect(qualityFlagLabel('FAST_ITEM')).toBe('过快作答');
    expect(qualityFlagLabel('SHORT_TOTAL_DURATION')).toBe('总时长过短');
    expect(aiAnalysisStatusLabel('FALLBACK')).toBe('规则摘要');
    expect(aiAnalysisStatusLabel(null)).toBe('未生成');
    expect(scanStatusLabel('CLEAN')).toBe('扫描通过');
    expect(submitReasonLabel('TIMEOUT')).toBe('超时提交');
  });
});
