import { cookies } from "next/headers";

import { DEFAULT_LOCALE, isLocale, type Locale } from "@/lib/i18n/dictionaries";
import { LOCALE_COOKIE } from "@/lib/i18n/locale-cookie";

/** Server-only: reads the current locale from the request cookie. */
export async function getLocale(): Promise<Locale> {
  const store = await cookies();
  const value = store.get(LOCALE_COOKIE)?.value;
  return isLocale(value) ? value : DEFAULT_LOCALE;
}
