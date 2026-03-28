import { describe, expect, it } from 'vitest';
import { formatAddLexicalListItemsFeedback } from './LexicalLists';

describe('LexicalLists helpers', () => {
  it('formats success feedback from backend skipped pair ids', () => {
    expect(
      formatAddLexicalListItemsFeedback({
        addedCount: 2,
        skippedPairIds: [7, 9],
      })
    ).toBe('已添加 2 个词对，跳过 2 个已存在词对。');
  });

  it('omits the skipped clause when backend returns no skipped ids', () => {
    expect(
      formatAddLexicalListItemsFeedback({
        addedCount: 3,
        skippedPairIds: [],
      })
    ).toBe('已添加 3 个词对。');
  });
});
