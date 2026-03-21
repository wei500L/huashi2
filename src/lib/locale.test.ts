import { beforeEach, describe, expect, it } from 'vitest';
import { DEFAULT_LOCALE, LOCALE_STORAGE_KEY, readStoredLocale, writeStoredLocale } from './locale';

describe('locale storage', () => {
  beforeEach(() => {
    window.localStorage.clear();
  });

  it('falls back to the default locale when storage is empty', () => {
    expect(readStoredLocale()).toBe(DEFAULT_LOCALE);
  });

  it('persists the selected locale', () => {
    writeStoredLocale('en-US');

    expect(window.localStorage.getItem(LOCALE_STORAGE_KEY)).toBe('en-US');
    expect(readStoredLocale()).toBe('en-US');
  });
});
