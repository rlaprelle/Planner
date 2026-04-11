import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import LanguageDetector from 'i18next-browser-languagedetector';

import common from './locales/en/common.json';
import auth from './locales/en/auth.json';
import dashboard from './locales/en/dashboard.json';
import ritual from './locales/en/ritual.json';
import tasks from './locales/en/tasks.json';
import deferred from './locales/en/deferred.json';
import timeBlocking from './locales/en/timeBlocking.json';
import admin from './locales/en/admin.json';

i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources: {
      en: {
        common,
        auth,
        dashboard,
        ritual,
        tasks,
        deferred,
        timeBlocking,
        admin,
      },
    },
    lng: 'en',
    fallbackLng: 'en',
    defaultNS: 'common',
    interpolation: {
      escapeValue: false,
    },
  });

export default i18n;
