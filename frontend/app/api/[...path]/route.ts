/**
 * Same-origin proxy to the backend API.
 *
 * The browser calls this app's own origin (`/api/...`) and this handler
 * forwards to the backend. Two reasons it exists:
 *
 * 1. **Deployability.** Pointing browser code straight at the backend means
 *    baking a URL that is only correct on the machine running the stack —
 *    `localhost:8080` becomes the *visitor's* computer once deployed. Server
 *    Components would still render (they fetch server-side), so every page
 *    would look fine while every mutation silently failed.
 * 2. **The backend needs no public URL.** Same-origin requests aren't CORS
 *    requests, and the API can stay on a private network. That is load-bearing
 *    while the app still has no login.
 *
 * **Why a route handler and not `rewrites()` in next.config.ts:** Next bakes
 * rewrite destinations into `.next/routes-manifest.json` at *build* time. In a
 * container the build runs before any deployment environment variables exist,
 * so the destination froze as `http://localhost:8080` and every proxied call
 * returned 500 — with the correct value sitting unused in the environment. A
 * route handler reads `process.env` per request, so one image works in local
 * dev, Docker Compose, and Railway with only the variable changing.
 */
import { NextRequest, NextResponse } from "next/server";

// Never cache or statically evaluate: this must run per request.
export const dynamic = "force-dynamic";

function backendOrigin(): string {
  return process.env.INTERNAL_API_BASE_URL ?? "http://localhost:8080";
}

/**
 * Hop-by-hop and host-specific headers must not be forwarded — `host` would
 * point at the frontend, and the compression/length headers describe a body
 * that fetch has already decoded.
 */
const STRIPPED_REQUEST_HEADERS = new Set([
  "host",
  "connection",
  "content-length",
  "accept-encoding",
  "transfer-encoding",
]);

const STRIPPED_RESPONSE_HEADERS = new Set([
  "content-encoding",
  "content-length",
  "transfer-encoding",
  "connection",
]);

async function proxy(request: NextRequest): Promise<Response> {
  // request.nextUrl keeps the /api prefix and the query string, both of which
  // the backend expects verbatim.
  const target = `${backendOrigin()}${request.nextUrl.pathname}${request.nextUrl.search}`;

  const headers = new Headers();
  request.headers.forEach((value, key) => {
    if (!STRIPPED_REQUEST_HEADERS.has(key.toLowerCase())) headers.set(key, value);
  });

  const hasBody = request.method !== "GET" && request.method !== "HEAD";

  let upstream: Response;
  try {
    upstream = await fetch(target, {
      method: request.method,
      headers,
      body: hasBody ? await request.arrayBuffer() : undefined,
      cache: "no-store",
      redirect: "manual",
    });
  } catch (error) {
    // The backend being unreachable is an infrastructure fault, not a client
    // error — 502 so it is distinguishable from a real API rejection.
    return NextResponse.json(
      {
        message: `Failed to reach backend at ${target}. Is the API running?`,
        detail: error instanceof Error ? error.message : String(error),
      },
      { status: 502 },
    );
  }

  const responseHeaders = new Headers();
  upstream.headers.forEach((value, key) => {
    if (!STRIPPED_RESPONSE_HEADERS.has(key.toLowerCase())) responseHeaders.set(key, value);
  });

  // 204/304 must not carry a body, and constructing a Response with one throws.
  if (upstream.status === 204 || upstream.status === 304) {
    return new Response(null, { status: upstream.status, headers: responseHeaders });
  }

  return new Response(await upstream.arrayBuffer(), {
    status: upstream.status,
    statusText: upstream.statusText,
    headers: responseHeaders,
  });
}

export const GET = proxy;
export const POST = proxy;
export const PUT = proxy;
export const PATCH = proxy;
export const DELETE = proxy;
export const HEAD = proxy;
export const OPTIONS = proxy;
