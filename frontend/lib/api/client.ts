// Server-side code (Server Components, route handlers) runs inside the
// frontend's own container/process, so "localhost" does NOT reach the backend
// container over the Docker network — it must use the Docker Compose service
// name (or, on Railway, the private-network host) via the server-only
// INTERNAL_API_BASE_URL.
//
// Client-side (browser) code deliberately uses **its own origin** and relies on
// the `/api/*` rewrite in next.config.ts to reach the backend. Pointing the
// browser straight at the backend would hardcode a URL that is only correct on
// the machine running the stack: once deployed, "localhost:8080" means the
// visitor's own computer, so every page would still render (Server Components
// fetch server-side) while every mutation silently failed. Same-origin also
// keeps these requests out of CORS entirely and lets the backend stay off the
// public internet — which is load-bearing while there is no login.
//
// NEXT_PUBLIC_API_BASE_URL remains an escape hatch for pointing a local
// frontend at some other backend; note Next inlines it at build time.
const API_BASE_URL =
  typeof window === "undefined"
    ? (process.env.INTERNAL_API_BASE_URL ??
      process.env.NEXT_PUBLIC_API_BASE_URL ??
      "http://localhost:8080")
    : (process.env.NEXT_PUBLIC_API_BASE_URL || window.location.origin);

export class ApiError extends Error {
  status: number;
  path: string;

  constructor(message: string, status: number, path: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.path = path;
  }
}

interface RequestOptions extends RequestInit {
  /** Query params appended to the request path. */
  params?: Record<string, string | number | boolean | undefined>;
}

function buildUrl(path: string, params?: RequestOptions["params"]) {
  const url = new URL(path, API_BASE_URL);
  if (params) {
    for (const [key, value] of Object.entries(params)) {
      if (value !== undefined) url.searchParams.set(key, String(value));
    }
  }
  return url.toString();
}

/**
 * Thin fetch wrapper for the AI Export Agent backend REST API.
 *
 * - Uses `cache: "no-store"` by default so dashboard data is always fresh
 *   (Sprint 1 has no auth; tenant scoping is injected server-side).
 * - Throws a typed `ApiError` on non-2xx responses.
 */
export async function apiFetch<T>(
  path: string,
  options: RequestOptions = {},
): Promise<T> {
  const { params, headers, ...rest } = options;
  const url = buildUrl(path, params);

  let response: Response;
  try {
    response = await fetch(url, {
      cache: "no-store",
      headers: {
        "Content-Type": "application/json",
        ...headers,
      },
      ...rest,
    });
  } catch {
    throw new ApiError(
      `Failed to reach backend at ${url}. Is the API running?`,
      0,
      path,
    );
  }

  if (!response.ok) {
    let message = `Request to ${path} failed with status ${response.status}`;
    try {
      const body = await response.json();
      if (body?.message) message = body.message;
    } catch {
      // response had no JSON body; keep default message
    }
    throw new ApiError(message, response.status, path);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}
