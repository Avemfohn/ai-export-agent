# Deploying TargetOut AI to Railway

How to put the app on a URL you can send someone. Seed data comes along
automatically — Flyway runs on first boot, so a fresh deploy is demo-ready with
no extra step.

> **Read first:** [`demo-guide.md`](demo-guide.md) — what to actually show once
> the link works.

---

## Architecture

Three services in one Railway project.

```
browser ──► frontend (public)  ──►  backend (private)  ──►  postgres (private)
             Next.js                 Spring Boot
             /api/* proxy
```

**Only the frontend gets a public domain.** The backend stays on Railway's
private network, reachable solely through the frontend's `/api/*` proxy. That is
a deliberate security choice, not an omission: there is still no login, so the
smaller the public surface the better.

---

## Why the `/api` proxy exists

[`frontend/app/api/[...path]/route.ts`](../frontend/app/api/%5B...path%5D/route.ts)
forwards browser calls to the backend, and it is what makes deployment possible
at all.

Without it, browser code needs a publicly reachable backend URL. That works on
your laptop, where the browser and the backend share `localhost` — and breaks
the moment anyone else opens the app, because `localhost:8080` then means *their*
computer. The failure is nasty: Server Components fetch server-side, so every
page still renders correctly while every button (approve, requeue, pause
campaign, save settings) silently does nothing.

Routing through the proxy also makes these same-origin requests, so CORS never
applies and [`WebConfig.java`](../backend/src/main/java/com/aiexportagent/config/WebConfig.java)
can keep its narrow `localhost:3000` allowlist instead of being widened to `*`.

> **Do not replace this with `rewrites()` in `next.config.ts`.** It looks
> equivalent and is not: Next bakes rewrite destinations into
> `.next/routes-manifest.json` at **build** time. In a container the build runs
> before any deployment environment exists, so the destination freezes as
> `http://localhost:8080` and every proxied call returns 500 — with the correct
> value sitting unused in the environment. This was tried first and failed
> exactly that way. A route handler reads `process.env` per request, so one
> image works in local dev, Docker Compose, and Railway.

---

## Setup

Create a project from `github.com/Avemfohn/ai-export-agent`, add a **Postgres**
plugin, then add two services from the same repo. Set each one's **Root
Directory** so Railway finds the right Dockerfile.

| Service | Root Directory | Public domain |
|---|---|---|
| `postgres` | — (Railway plugin) | no |
| `backend` | `backend` | **no** |
| `frontend` | `frontend` | **yes** — this is the link you share |

Both app services carry a `railway.json` pinning the Docker builder, so there's
nothing to configure beyond the root directory and the variables below.

### Backend variables

```
SPRING_PROFILES_ACTIVE     = docker
SPRING_DATASOURCE_URL      = jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}
SPRING_DATASOURCE_USERNAME = ${{Postgres.PGUSER}}
SPRING_DATASOURCE_PASSWORD = ${{Postgres.PGPASSWORD}}
DEV_TENANT_ID              = 00000000-0000-0000-0000-000000000001
SERVER_ADDRESS             = ::
AI_PROVIDER                = mock
EMAIL_PROVIDER             = mock
JAVA_TOOL_OPTIONS          = -XX:MaxRAMPercentage=75 -Djava.net.preferIPv6Addresses=true
```

**`SERVER_ADDRESS=::` is the one that will waste an afternoon if you skip it.**
Railway's private network is IPv6-only and Spring Boot binds IPv4 by default, so
without it the frontend's proxy calls are refused and the backend looks like it
failed to start when it's actually running fine.

**`AI_PROVIDER` and `EMAIL_PROVIDER` are already the defaults — set them
explicitly anyway.** On a public URL with no login they are the only thing
between a curious visitor and real billed AI calls or real email to real
addresses.

### Frontend variables

```
INTERNAL_API_BASE_URL = http://backend.railway.internal:8080
```

That's all. Deliberately **no** `NEXT_PUBLIC_API_BASE_URL` — setting it would
send browsers straight at a backend URL and reintroduce the exact bug the proxy
exists to prevent.

---

## After the first deploy

The backend Dockerfile runs a full `mvn package`, so expect several minutes on a
cold build. Then check the demo guide's pre-flight list against the public URL:

| Check | Expect |
|---|---|
| Overview → Failed Sends | **1** |
| Leads → Provence Maison | Pending Approval |
| Leads → Maple & Co Home Goods | Approved |

If those are right, Flyway seeded correctly. Approve one lead and confirm it
sends within ~90 seconds to prove the schedulers run in Railway too.

---

## Things to know before you share the link

**There is no login.** Anyone with the URL has full control of the demo tenant —
approving leads, editing the email template, changing campaign status. Fine for
showing one person; don't post it publicly.

**Demo state degrades as people click.** To reset, wipe the Railway Postgres
volume and redeploy; Flyway re-seeds from scratch, the same as
`docker compose down -v` locally.

**No mail is sent and no AI is billed** as long as both providers stay `mock`.
Nothing in the UI can change that — it's environment-only.
