# TargetOut AI

## 1. Project Overview

TargetOut AI is a B2B SaaS that automates finding, qualifying, and emailing
international buyers for exporters/manufacturers. Business flow:

1. **Scraping** — gather target-sector companies from Google Maps, B2B
   directories, and user-uploaded trade-fair lists (Excel/PDF).
2. **AI filtering** — an LLM checks each scraped company's website against the
   tenant's buyer criteria; non-matches are rejected.
3. **Outreach** — for matches, find corporate emails, then AI drafts a
   personalized cold email and sends it. The AI does **not** write freely
   from scratch: it *customizes* a client-authored base draft template per
   supplier (personalizing the opening reference to the recipient's real
   products/news, matching tone), so the client controls the core pitch and
   the AI's job is narrowed to targeted personalization. The tenant has one
   standing default template (`tenant_settings.email_draft_template`); a
   campaign targeting a different product line or region can override it
   with its own snapshot (`tenant_campaigns.email_draft_template_snapshot`),
   the same pattern already used for `buyer_criteria` →
   `buyer_criteria_snapshot`. The AI customization logic itself is phase 2 —
   only the schema fields exist so far.
4. **Reply tracking** — AI classifies intent on incoming replies; warm replies
   surface on the dashboard and trigger a WhatsApp notification.

Building an MVP for one pilot factory client first, architected from day one
to scale into a multi-tenant vertical SaaS product.

## 2. Master Pool Architecture (READ THIS BEFORE TOUCHING THE DATA LAYER)

- `global_suppliers` / `global_supplier_contacts` = a shared pool of companies
  and contacts across **ALL** tenants. These tables have **no `tenant_id`
  column** — this is the core IP asset of the product, deduped by domain.
- `tenant_leads` = the bridge table: one row per `(tenant_id,
  global_supplier_id)` pair, carrying its own status lifecycle
  (`PENDING_APPROVAL`, `EMAIL_SENT`, `INTERESTED`, ...). The same
  `global_suppliers` row can be independently referenced by many tenants'
  `tenant_leads`, each with its own status.
- **Hard rule**: one tenant's data, leads, campaigns, emails, responses,
  notifications, or API keys must **never** be visible to another tenant.
  Every tenant-scoped query must filter by a `tenantId` sourced from the
  trusted request context (`TenantContext`) — never from client-supplied
  input (body/query/path/header).
- Global-pool tables are read cross-tenant by design (that's the point of the
  shared pool), but writes that create tenant-linked rows must always be
  scoped to the current tenant only, and must never attach a `tenant_id` to
  `global_suppliers` / `global_supplier_contacts` themselves.
- See [`.claude/agents/tenant-isolation-reviewer.md`](.claude/agents/tenant-isolation-reviewer.md)
  — a subagent invoked proactively on backend changes to catch cross-tenant
  isolation violations before they ship.

## 3. Tech Stack

- **Backend**: Java 21, Maven, Spring Boot — package base `com.aiexportagent`
- **Frontend**: Next.js (App Router), Shadcn UI, Tailwind CSS, pnpm
- **DB**: PostgreSQL, Flyway migrations, Dockerized
- **AI** (not wired yet): LangChain4j + OpenAI/Anthropic — see "AI Integration
  Notes" below
- **Scraping** (not wired yet): Apify/Bright Data actors + webhooks
- **Email**: Mailgun, with inbound webhook support for reply tracking — chosen
  over Resend specifically because inbound reply tracking / intent
  classification is a core product feature, not a nice-to-have, and Mailgun's
  inbound routing is the more mature of the two.

### AI Integration Notes (budget time for this during the AI sprint)

LangChain4j is a real, actively maintained library, but it is a smaller port
of the Python LangChain ecosystem and lags it in a few concrete ways:

- **Fewer provider integrations** and slower coverage of new
  OpenAI/Anthropic API features compared to the Python SDKs.
- **Structured-output parsing is less turnkey.** Mapping an LLM response to a
  POJO/record (e.g. the buyer-criteria match verdict, or the email
  intent-classification result) may need manual JSON-schema prompting plus
  your own validation/retry layer, rather than a built-in output parser doing
  it for you.
- **Thinner community coverage** — fewer worked examples and StackOverflow
  answers than the Python ecosystem, so expect more time reading source/docs
  directly when something doesn't behave as expected.

None of this blocks the Java/Spring Boot choice — it's the right call for the
relational, multi-tenant core of this system — just plan AI-sprint estimates
accordingly.

## 4. Folder Map

```
.
├── backend/            Spring Boot API (Java 21, Maven)
├── frontend/            Next.js dashboard (App Router, Shadcn, Tailwind)
├── .claude/agents/       Claude Code subagents (tenant-isolation-reviewer)
└── docker-compose.yml    Local dev: postgres + backend + frontend
```

See `backend/` and `frontend/` for their internal structure — backend is
package-by-feature under `com.aiexportagent`, split into `global` (shared
pool, no tenant concept), `tenant` (every repo method scoped by `tenantId`),
and `common` (cross-cutting concerns incl. `TenantContext`).

**Frontend theming & translation:** dark/light mode uses `next-themes`
(`frontend/components/providers/theme-provider.tsx`), switched from the
Settings page only (no topbar quick-toggle). The UI is fully translatable
between English and Turkish, selected from Settings — not URL-based (no
`/tr/...` routes), driven by a `locale` cookie:
`frontend/lib/i18n/dictionaries/{en,tr}.json` hold the translation keys,
`frontend/lib/i18n/get-locale.ts` (reads the cookie) +
`frontend/lib/i18n/dictionaries.ts` (`getDictionary()`) are used server-side
by Server Component pages, and
`frontend/components/providers/i18n-provider.tsx` (`useTranslations()` hook)
serves Client Components (Sidebar, Topbar, status badges, the Settings
switchers). See Conventions below — this is a hard rule, not just the
current state.

## 5. How to Run Locally

- Full stack: `docker compose up --build` (frontend on :3000, backend on
  :8080, postgres on :5432). Flyway runs migrations automatically on backend
  boot.
- Backend alone: `cd backend && ./mvnw spring-boot:run
  -Dspring-boot.run.profiles=local` (expects Postgres reachable at
  `localhost:5432`, e.g. via `docker compose up postgres`).
- Frontend alone: `cd frontend && pnpm install && pnpm dev`.

## 6. Current Sprint Status

**Sprint 1 (current)**: build the full flow and UI on **mock data only**.
Mock data is seeded into a real Postgres database via a Flyway migration
(`V2__seed_mock_data.sql`) — the backend reads and writes through real JPA
repositories against the real schema, it does not hardcode fake objects in
application code. This validates the schema and UX before any real
integrations are wired in.

- **No real OpenAI/Anthropic/Apify/Resend/Mailgun calls yet.** Those
  integration points exist as empty placeholder packages (`ai/`, `scraping/`,
  `email/`) to be filled in in a later sprint.
- **No real tenant login yet.** A hardcoded dev `tenant_id` (env var
  `DEV_TENANT_ID`) is injected into every request via `TenantContextFilter`,
  so tenant-scoping code paths are real and exercised even though there's no
  login screen.

## 7. Conventions

- Package-by-feature, not package-by-layer, within the `global` vs `tenant`
  split described above.
- `VARCHAR` + `CHECK` constraints instead of native Postgres enums (mirrored
  as Java enums at the application layer).
- UUID primary keys everywhere (`gen_random_uuid()`).
- Every table has `created_at` / `updated_at` timestamps.
- Commit messages: conventional-commit-ish, imperative mood.
- **All new frontend UI text must go through the translation dictionaries**
  (`frontend/lib/i18n/dictionaries/{en,tr}.json`) — no hardcoded string
  literals in JSX. Add the key to both `en.json` and `tr.json` in the same
  change; a key present in only one language is a bug. Server Component
  pages call `getDictionary(await getLocale())` directly; Client Components
  use the `useTranslations()` hook from
  `frontend/components/providers/i18n-provider.tsx`. Status/enum badge
  values are translated too (see `dict.leads.status`, `dict.campaigns.status`,
  etc. for the pattern) — never render a raw backend enum string directly.
