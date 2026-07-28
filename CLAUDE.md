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
- **AI**: lead scoring and cold-email drafting are both wired (see "AI Lead
  Scoring" / "AI Outreach Drafting" below) via Spring's built-in
  `RestClient`, not LangChain4j (see "AI Integration Notes"). Reply-intent
  classification is not built yet.
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

### AI Lead Scoring (`com.aiexportagent.ai`)

`POST /api/leads/score` scores every `global_suppliers` row the current
tenant doesn't already have a `tenant_leads` row for, against that tenant's
`buyer_criteria`, and creates a lead per candidate (`PENDING_APPROVAL` if the
score clears `app.ai.match-threshold` — default 60 — else `REJECTED`).
Create-only/idempotent: never touches an existing lead, safe to re-run.

- `ai/client/` — provider-agnostic `AiClient` interface. Exactly one
  implementation is active, chosen by `app.ai.provider`
  (`mock` | `openai` | `anthropic`, default `mock`):
  - `MockAiClient` — deterministic keyword-overlap heuristic, no HTTP call,
    no API key. Proves the plumbing end-to-end for free; not a real
    qualification algorithm.
  - `OpenAiClient` / `AnthropicClient` — real calls via Spring's built-in
    `RestClient` (no LangChain4j — see AI Integration Notes above on why).
    Activated by setting `AI_PROVIDER=openai`/`anthropic` +
    `OPENAI_API_KEY`/`ANTHROPIC_API_KEY` — no code changes needed.
  - `PromptBuilder` builds the system/user prompt text for both operations
    this client supports (scoring and drafting, see below);
    `ScoringResponseParser` / `EmailDraftResponseParser` leniently
    extract+parse each operation's expected JSON shape (via the shared
    `JsonExtraction` helper — models don't always emit JSON-only despite
    instructions, so this extracts the first `{...}` span rather than
    requiring an exact match).
- `ai/scoring/LeadScoringService` — the orchestrator; depends on
  `TenantSettingsService` and `TenantLeadService` (never a repository
  directly, per the encapsulation convention below).
- `buyer_criteria` is passed into the prompt as raw JSON text, not parsed
  into a Java schema — the LLM interprets it holistically (same "stays
  opaque JSON" convention as `email_draft_template`).

### AI Outreach Drafting (`com.aiexportagent.ai.outreach`)

`POST /api/outreach-emails/draft` customizes the tenant's (or campaign's)
base `email_draft_template` into a real subject/body for every `APPROVED`
tenant_lead that doesn't already have an `outreach_emails` row, and stores
it with `status = 'DRAFT'` — it does **not** send anything (no Mailgun
integration yet). Create-only/idempotent, same as lead scoring.

- Uses the same `AiClient.draftEmail(...)` operation (see above) — one
  provider choice covers both scoring and drafting.
- `PENDING_APPROVAL` → `APPROVED`/`REJECTED` transition is a separate,
  narrowly-scoped endpoint: `PATCH /api/leads/{id}/status`
  (`TenantLeadService.updateStatusForCurrentTenant`) — only accepts
  `APPROVED`/`REJECTED`, and only from a lead currently
  `PENDING_APPROVAL` (409 otherwise). It's a review gate, not a general
  lead editor.
- `OutreachDraftingService` picks the lead's `TenantCampaign`'s
  `email_draft_template_snapshot` if `tenant_campaign_id` is set, else the
  tenant's default `email_draft_template`; picks the supplier's primary
  `global_supplier_contacts` row (falling back to any contact, or skipping
  the lead entirely — counted, not an error — if none exist).
- Personalization is limited to what `global_suppliers.description`
  actually contains — there's no live "recent news" enrichment yet, so
  don't expect real news references, just sector/description-grounded
  customization of the template.
- Same transaction-boundary lesson as `LeadScoringService`, applied from
  the start here: the orchestrating method is deliberately NOT
  `@Transactional` (the loop makes a synchronous external AI call per
  lead), each `outreach_emails` write commits independently via
  `REQUIRES_NEW`, and per-lead failures are caught broadly so one bad lead
  doesn't abort the batch.

### Automated Outreach Pipeline (`com.aiexportagent.email`)

Once a lead is `APPROVED`, two `@Scheduled` jobs carry it the rest of the
way with no manual trigger — `OutreachQueueingScheduler` (AI-drafts every
undrafted `APPROVED` lead and inserts it straight as `QUEUED`) and
`OutreachSendingScheduler` (sends the globally-oldest `QUEUED` email, then
marks it `SENT` and cascades the lead to `EMAIL_SENT`).

- **`DRAFT` is invisible in the automated flow.** The automated path never
  persists a `DRAFT` row; only the manual `POST /api/outreach-emails/draft`
  escape hatch produces those.
- **Pacing is conservative and global**, not per-tenant: `app.email.send-interval-ms`
  × `app.email.send-batch-size` defaults to ~1 email/60s across all tenants.
- `EmailSender` mirrors the `AiClient` split — `MockEmailSender` is the
  default (`app.email.provider=mock`), `MailgunEmailSender` is opt-in.
- These schedulers are the only code that runs outside an HTTP request, so
  they must set `TenantContext` explicitly per iteration and clear it in a
  `finally` — `TenantContextFilter` isn't there to do it for them.

**A failed send is never retried automatically, and that is deliberate.**
`markFailed` records the error and stops; the lead stays `APPROVED`. Recovery
is an explicit operator action — `POST /api/outreach-emails/{id}/requeue`
(`FAILED` → `QUEUED` only, 409 otherwise), surfaced in the UI on the outreach
table, the lead detail page, and a dashboard "failed sends" stat.

Do **not** "fix" this by making failures self-healing. `OutreachDraftingService`
treats *any* existing `outreach_emails` row — `FAILED` included — as "this lead
is already handled". Excluding `FAILED` from that check would have the 60s
queueing scheduler re-draft (a **billed AI call**) and re-queue the same lead
every single tick, forever. The blocking behaviour and the manual-only recovery
are two halves of the same design.

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

- **AI lead scoring is real (Phase 2), but defaults to a mock provider** —
  no OpenAI/Anthropic key is configured anywhere by default, so no billed
  calls happen unless you explicitly set `AI_PROVIDER` + an API key. See "AI
  Lead Scoring" above.
- **No real Apify calls yet** — `scraping/` is still an empty placeholder
  package, so nothing ever creates a `scraping_jobs` row.
- **Outbound email is built but not live.** `email/` has a real
  `MailgunEmailSender` and the full automated send pipeline (see above), but
  `app.email.provider` defaults to `mock`, so no mail actually leaves the
  machine unless you set `EMAIL_PROVIDER=mailgun` + credentials. The
  **inbound** side — the reply webhook that would populate `email_responses`
  and classify reply intent — is not built at all.
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
- **A package's Repository is never injected into another package's Service.**
  Cross-feature access always goes through the owning package's Service
  (e.g. `LeadScoringService` depends on `TenantSettingsService` and
  `TenantLeadService`, not `TenantSettingsRepository`/`TenantLeadRepository`
  directly) — same reasoning as `GlobalSupplierService` fronting
  `GlobalSupplierRepository` for the `tenant` package.
- **All new frontend UI text must go through the translation dictionaries**
  (`frontend/lib/i18n/dictionaries/{en,tr}.json`) — no hardcoded string
  literals in JSX. Add the key to both `en.json` and `tr.json` in the same
  change; a key present in only one language is a bug. Server Component
  pages call `getDictionary(await getLocale())` directly; Client Components
  use the `useTranslations()` hook from
  `frontend/components/providers/i18n-provider.tsx`. Status/enum badge
  values are translated too (see `dict.leads.status`, `dict.campaigns.status`,
  etc. for the pattern) — never render a raw backend enum string directly.
