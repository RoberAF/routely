// Small dependency-free i18n dictionary for the web demo. `Messages` is the shared key
// type for both languages, so TypeScript errors if either `en` or `es` is missing a key
// (or has an extra one) rather than silently falling back to English at runtime.

export type Lang = 'en' | 'es';

export interface Messages {
  pageTitle: string;
  headerTitle: string;
  headerSubtitle: string;
  repLabel: string;
  allActiveLabel: string;
  customersLabel: string;
  computeButton: string;
  legendNaive: string;
  legendOptimized: string;
  statTitle: string;
  statNaiveLabel: string;
  statOptimizedLabel: string;
  statImprovementLabel: string;
  statPassesLabel: string;
  statViolationsLabel: string;
  popupNoTimeWindow: string;
  popupHomeBase: string;
  langToggleAriaLabel: string;
}

export const messages: Record<Lang, Messages> = {
  en: {
    pageTitle: 'Routely — route optimizer demo',
    headerTitle: 'Routely — route optimizer demo',
    headerSubtitle:
      'Nearest-neighbor + 2-opt over the seeded Galicia dataset, running entirely in your browser.',
    repLabel: 'Rep home base',
    allActiveLabel: 'All active customers',
    customersLabel: 'Customers (multi-select)',
    computeButton: 'Compute route',
    legendNaive: 'Naive (input order)',
    legendOptimized: 'Optimized (NN + 2-opt)',
    statTitle: 'Result',
    statNaiveLabel: 'Naive distance',
    statOptimizedLabel: 'Optimized distance',
    statImprovementLabel: 'Improvement',
    statPassesLabel: '2-opt passes',
    statViolationsLabel: 'Time window violations',
    popupNoTimeWindow: 'no time window',
    popupHomeBase: 'home base',
    langToggleAriaLabel: 'Language',
  },
  es: {
    pageTitle: 'Routely — demo del optimizador de rutas',
    headerTitle: 'Routely — demo del optimizador de rutas',
    headerSubtitle:
      'Vecino más cercano + 2-opt sobre el dataset sembrado de Galicia, ejecutándose enteramente en tu navegador.',
    repLabel: 'Base del comercial',
    allActiveLabel: 'Todos los clientes activos',
    customersLabel: 'Clientes (selección múltiple)',
    computeButton: 'Calcular ruta',
    legendNaive: 'Ingenua (orden de entrada)',
    legendOptimized: 'Optimizada (NN + 2-opt)',
    statTitle: 'Resultado',
    statNaiveLabel: 'Distancia ingenua',
    statOptimizedLabel: 'Distancia optimizada',
    statImprovementLabel: 'Mejora',
    statPassesLabel: 'Pasadas de 2-opt',
    statViolationsLabel: 'Infracciones de franja horaria',
    popupNoTimeWindow: 'sin franja horaria',
    popupHomeBase: 'base del comercial',
    langToggleAriaLabel: 'Idioma',
  },
};

const STORAGE_KEY = 'routely:lang';

function isLang(value: string | null): value is Lang {
  return value === 'en' || value === 'es';
}

/** navigator.language starting with "es" -> Spanish, everything else -> English. */
function detectBrowserLang(): Lang {
  return navigator.language.toLowerCase().startsWith('es') ? 'es' : 'en';
}

export function detectInitialLang(): Lang {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (isLang(stored)) {
      return stored;
    }
  } catch {
    // localStorage unavailable (privacy mode, etc.) — fall back to browser detection.
  }
  return detectBrowserLang();
}

export function persistLang(lang: Lang): void {
  try {
    localStorage.setItem(STORAGE_KEY, lang);
  } catch {
    // Persistence is a nice-to-have, not a requirement — ignore write failures.
  }
}
