import { describe, expect, it } from 'vitest';
import i18n from './i18n';

describe('i18n runtime configuration', () => {
  it('does not print the upstream support notice in the application console', () => {
    expect(i18n.options.showSupportNotice).toBe(false);
  });
});
