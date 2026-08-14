import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import { resources } from './i18n/resources';
import { DEFAULT_LOCALE, readStoredLocale } from './locale';

if (!i18n.isInitialized) {
  void i18n.use(initReactI18next).init({
    resources,
    lng: readStoredLocale(),
    fallbackLng: DEFAULT_LOCALE,
    supportedLngs: ['zh-CN', 'en-US'],
    showSupportNotice: false,
    interpolation: {
      escapeValue: false,
    },
    returnNull: false,
  });
}

export default i18n;
