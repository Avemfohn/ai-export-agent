"use client";

import { createContext, useCallback, useContext, useMemo, useState } from "react";
import { useRouter } from "next/navigation";

import {
  type Dictionary,
  type Locale,
  getDictionary,
} from "@/lib/i18n/dictionaries";
import { LOCALE_COOKIE } from "@/lib/i18n/locale-cookie";

interface I18nContextValue {
  locale: Locale;
  dict: Dictionary;
  setLocale: (locale: Locale) => void;
}

const I18nContext = createContext<I18nContextValue | null>(null);

export function I18nProvider({
  initialLocale,
  children,
}: {
  initialLocale: Locale;
  children: React.ReactNode;
}) {
  const router = useRouter();
  const [locale, setLocaleState] = useState<Locale>(initialLocale);

  const setLocale = useCallback(
    (next: Locale) => {
      document.cookie = `${LOCALE_COOKIE}=${next}; path=/; max-age=31536000`;
      setLocaleState(next);
      router.refresh();
    },
    [router],
  );

  const value = useMemo<I18nContextValue>(
    () => ({ locale, dict: getDictionary(locale), setLocale }),
    [locale, setLocale],
  );

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>;
}

export function useTranslations() {
  const ctx = useContext(I18nContext);
  if (!ctx) {
    throw new Error("useTranslations must be used within an I18nProvider");
  }
  return ctx;
}
