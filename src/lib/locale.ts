export const LOCALE_STORAGE_KEY = 'ef-transfer-locale';

export const SUPPORTED_LOCALES = ['zh-CN', 'en-US'] as const;

export type SupportedLocale = (typeof SUPPORTED_LOCALES)[number];

export const DEFAULT_LOCALE: SupportedLocale = 'zh-CN';

export function isSupportedLocale(value: string | null | undefined): value is SupportedLocale {
  return SUPPORTED_LOCALES.includes(value as SupportedLocale);
}

export function readStoredLocale(): SupportedLocale {
  if (typeof window === 'undefined') {
    return DEFAULT_LOCALE;
  }
  const raw = window.localStorage.getItem(LOCALE_STORAGE_KEY);
  return isSupportedLocale(raw) ? raw : DEFAULT_LOCALE;
}

export function writeStoredLocale(locale: SupportedLocale): void {
  if (typeof window === 'undefined') {
    return;
  }
  window.localStorage.setItem(LOCALE_STORAGE_KEY, locale);
}
