import { describe, expect, it } from 'vitest';
import { collectActiveFilterLabels, embeddingStatusLabel } from './LexicalPairsWorkspace';

describe('LexicalPairsWorkspace helpers', () => {
  it('maps embedding statuses to visible labels', () => {
    expect(embeddingStatusLabel('PENDING')).toBe('待嵌入');
    expect(embeddingStatusLabel('FAILED')).toBe('嵌入失败');
    expect(embeddingStatusLabel('UNKNOWN')).toBe('UNKNOWN');
    expect(embeddingStatusLabel(undefined)).toBe('--');
  });

  it('collects active filters for teacher mode without admin-only embedding labels', () => {
    expect(
      collectActiveFilterLabels('teacher', {
        keyword: 'coin',
        lexicalPairType: 'FALSE_FRIEND',
        active: 'ACTIVE',
        embeddingStatus: 'FAILED',
      })
    ).toEqual(['关键词：coin', '词对类型：同形异义', '启用状态：仅启用']);
  });

  it('collects active filters for admin mode including embedding status', () => {
    expect(
      collectActiveFilterLabels('admin', {
        keyword: '',
        lexicalPairType: 'ALL',
        active: 'ALL',
        embeddingStatus: 'FAILED',
      })
    ).toEqual(['向量状态：嵌入失败']);
  });
});
