# TargetOut AI — Product Change Log

A running record of what shipped, why it shipped, and what we decided along the
way. Written for product review, not code review — each entry leads with the
user problem, not the diff.

**Conventions**
- Newest first. One entry per meaningful product change (not per commit).
- "Decisions" captures choices we'd otherwise forget and re-litigate later.
- Status: 🟢 Shipped · 🟡 Built, not enabled · ⚪ Planned

**Companion doc:** [`phases.md`](phases.md) holds the forward roadmap. Every
completed phase gets an entry here; see the maintenance checklist at the bottom
of that file.

---

## At a glance

| Date | Change | Area | Status |
|---|---|---|---|
| 2026-07-31 | Campaigns: create/edit, lead assignment, status gating | Campaigns | 🟢 Shipped |
| 2026-07-30 | Configuration UI (criteria + email template) | Config | 🟢 Shipped |
| 2026-07-28 | Failed-send recovery & visibility | Outreach | 🟢 Shipped |
| 2026-07-26 | Automated approve → draft → send pipeline | Outreach | 🟢 Shipped (mock sender) |
| 2026-07-26 | Auto-approve threshold + bulk lead actions | Leads | 🟢 Shipped |
| 2026-07-25 | AI outreach email drafting | AI | 🟢 Shipped (mock provider) |
| 2026-07-25 | AI lead scoring | AI | 🟢 Shipped (mock provider) |
| 2026-07-25 | Email draft template (tenant + campaign) | Schema | 🟡 Schema only |
| 2026-07-25 | Dark/light mode + EN/TR translation | Platform | 🟢 Shipped |
| 2026-07-25 | Rename to TargetOut AI | Brand | 🟢 Shipped |
| 2026-07-25 | Multi-tenant scaffold (backend + dashboard) | Platform | 🟢 Shipped |

---

## 2026-07-31 — Campaigns that do something

**Status:** 🟢 Shipped · **Area:** Campaigns

### The problem
Campaigns were read-only — but the real problem was that they were **inert**.
Nothing in the product could put a lead into a campaign, so every lead the AI
had ever scored belonged to no campaign at all; only a handful of hand-seeded
demo leads did. And campaign status was a decorative label that no code read, so
a "Pause" button would have silently lied — emails would have kept going out.

Shipping "create and edit campaigns" on top of that would have produced
campaigns you could name but never route anything through.

### What shipped
- **Create and edit campaigns**, with their own email template.
- **Assign leads in bulk** from the leads table, including removing them from a
  campaign.
- **Status now controls sending.** Only Active campaigns send; leads in a
  Draft/Paused/Completed/Archived campaign are held back and counted.
- **Campaign column on the leads table**, showing a clear "Campaign not active —
  not sending" warning so held-back leads are never a mystery.
- **Campaign detail lists its leads**, so assignment has a visible result.

### Decisions
- **A new campaign starts as a copy of your default email template.** That's what
  a "snapshot" means, and it means a new campaign is immediately usable rather
  than opening on an empty form that would silently block its leads.
- **"Activate immediately" is on by default when creating.** Defaulting to Draft
  would silently park every lead assigned to a brand-new campaign — the exact
  confusion this phase set out to remove.
- **Status transitions are restricted**, not free-form: a Completed campaign
  can't quietly resume sending. Archive is reversible (back to Draft), because a
  mis-archive shouldn't need a database edit — the same lesson as failed sends.
- **Only pending/approved leads can be reassigned.** Once a lead has been
  emailed, its campaign no longer changes anything, so offering the action would
  be theatre. The toolbar now shows per-action eligibility ("Approve (0/1)")
  rather than silently doing nothing.
- **Pausing a campaign does not recall mail already queued.** This is deliberate
  and load-bearing: filtering the send queue by campaign would make the oldest
  blocked email permanently jam the queue for *every* tenant. The code carries an
  explicit warning, because it looks like a one-line improvement.
- **Per-campaign buyer criteria were left out.** They're stored but read by
  nothing, and an editor for a field that does nothing is worse than no editor.

### Verified
Create returns the tenant's template pre-filled; blank names and illegal starting
statuses rejected; every status transition rule enforced (including
Archived→Active rejected, Archived→Draft allowed, and same-status idempotent);
assigning to **another tenant's campaign returns 404 with nothing written**;
already-emailed leads reported as skipped rather than silently ignored; a paused
campaign holds its leads while a lead with no campaign still sends *in the same
run*; reactivating releases them; queued mail still drains under a paused
campaign. Turkish throughout. Tenant-isolation review passed with zero findings.

### Fixed along the way
A shared UI button couldn't be used by any server-rendered page — it pulled in a
library that isn't available there, which would have broken the campaigns pages
outright. Also fixed the page title on nested routes, which resolved to a raw id.

---

## 2026-07-30 — Configuration UI

**Status:** 🟢 Shipped · **Area:** Config

### The problem
**The customer could not configure the product at all.** Buyer criteria drive
every scoring decision, and the email template is the entire "AI personalises
*your* pitch" premise — neither had an editing screen or an API to change them.
The Settings page showed a static placeholder listing five fields
(*Target Countries*, *Minimum Company Size*, *Preferred Languages*…) that
matched no real data and could never be filled in. The core value proposition
was unreachable by the person paying for it.

### What shipped
- **Buyer criteria editor** — friendly fields for keywords, minimum revenue,
  target sectors and regions, plus an **Advanced (raw JSON)** mode for anything
  the form doesn't cover.
- **Email template editor** — subject, body, and separate "Instructions for the
  AI", with a **preview** showing the email filled in for a sample company.
- **"Test criteria"** — scores a handful of real companies against your criteria
  and shows what would happen, without saving anything or creating leads.
- **Sending identity** shown read-only.
- Placeholder warnings for typos and unsupported tokens.

### Decisions
- **Criteria stay schema-free.** We validate *shape* (it must be a JSON object),
  never contents. The AI reads criteria holistically, so locking them to a fixed
  set of fields would make any criterion we didn't anticipate uneditable.
- **The guided form never destroys what it doesn't understand.** Editing keywords
  preserves every other key, including ones the form has no input for. This is
  the property most likely to be broken by a future change — it's covered by an
  explicit test.
- **The raw-JSON editor is a mode, not a live mirror.** You apply your changes
  explicitly, and saving is blocked until you do — otherwise the guided fields
  would silently change under you.
- **Sender name and address are visible but not editable.** They're tied to
  SPF/DKIM and sending-domain reputation, so TargetOut configures them during
  onboarding. The update endpoint physically cannot write them.
- **Placeholder problems warn rather than block.** A real AI provider may resolve
  a token the mock can't, so blocking would bake our provider choice into
  validation.

### Also fixed
- **Blank templates no longer send blank emails.** An unconfigured template
  produced an empty subject *and body*, which the pipeline sent anyway. Those
  leads are now skipped and reported, before any AI call is made.
- **`{{sector}}` now works.** It appeared in the shipped default template but was
  never substituted, so it went out to recipients as literal text.

### Verified
Unknown criteria keys survive a guided-form save; raw-JSON mode blocks saving
until applied and refuses to coerce values it can't represent; validation rejects
bad shapes with readable messages; "Test criteria" creates nothing (lead count
unchanged) and scores swing 20→100 purely from criteria; a blank template is
skipped instead of sent; sender fields ignore an attempted overwrite; Turkish
throughout. Tenant-isolation review passed with zero findings.

### Deferred
Campaign editing (next phase, reuses these editors). Editing a template still
doesn't affect emails already queued — the count is surfaced as a warning, and a
real cancel belongs with the Phase 4 kill switch.

---

## 2026-07-28 — Failed-send recovery & visibility

**Status:** 🟢 Shipped · **Area:** Outreach · **Commit:** `8b55b29`

### The problem
A lead called **Provence Maison** was silently lost. Its outreach email failed to
send, and from that moment the lead was invisible and unrecoverable:

- The failure reason was never exposed by the API, so the UI showed a red
  "Failed" badge with no explanation and no next step.
- The lead detail page — where you'd naturally investigate — said *"No emails
  sent to this lead yet"*, which was actively misleading.
- There was no way to retry. Unsticking it required hand-editing the database.

Every future send failure would have died the same way, with no one noticing.

### What shipped
- **Requeue action** — a failed email can be put back on the send queue from the
  UI. It then sends normally and the lead advances to *Email Sent*.
- **The reason is now visible** — the provider's actual error text is shown
  wherever the email appears.
- **Three surfaces now report failures:** the outreach table, the lead detail
  page (which now lists that lead's real emails instead of a placeholder), and a
  new **"Failed Sends"** counter on the dashboard.

### Decisions
- **Failures are never retried automatically.** Recovery is a deliberate human
  action. Auto-retry would let a misconfigured integration hammer itself, and —
  because the drafting step treats *any* existing email as "this lead is handled"
  — it would have re-drafted the same lead every 60 seconds forever, each attempt
  costing a billed AI call. The blocking behaviour and manual-only recovery are
  two halves of one design; this is now written into `CLAUDE.md` so it doesn't get
  "fixed" by accident.
- **Requeue is a recovery action, not a status editor.** It only accepts an email
  that actually failed.
- **Retries jump the queue.** A requeued email keeps its original timestamp, so
  recovered work goes out ahead of newer sends.

### Verified
Full recovery cascade end-to-end (including clicking the button in a real
browser); no auto-retry and no duplicate rows across four scheduler cycles;
permission and error-handling guards; Turkish translations; automated
tenant-isolation review passed with zero findings.

### Deferred
Bulk requeue, campaign detail page email list, and alerting on failure (today you
still have to open the dashboard to notice).

---

## 2026-07-26 — Automated approve → draft → send pipeline

**Status:** 🟢 Shipped, running on the mock sender · **Area:** Outreach
**Commit:** `74e5928`

### The problem
Approving a lead did nothing. Drafting was a separate manual trigger, and there
was no send step at all — approved leads simply sat there.

### What shipped
Approval now starts a hands-off pipeline: an approved lead is automatically
AI-drafted and sent, with no further clicks. Emails go out on a steady drip
rather than in a burst.

### Decisions
- **Conservative pacing: ~1 email per minute, globally.** Protects a sending
  domain with no reputation yet. Deliberately not per-tenant — that's premature
  at one pilot client.
- **"Draft" is invisible.** The automated path never saves a draft you have to
  approve a second time. The manual draft endpoint stays as a testing hatch.
- **No auto-retry on failure** (see 2026-07-28 for the consequences we then had
  to fix).
- **Mock sender by default** — nothing real leaves the machine until Mailgun
  credentials are deliberately configured.

### Follow-up fixed same day
Trial-status tenants were being skipped by the automation entirely. A trial
prospect needs the full experience to see the product's value, so only suspended
and cancelled accounts are now excluded.

---

## 2026-07-26 — Auto-approve threshold + bulk lead actions

**Status:** 🟢 Shipped · **Area:** Leads · **Commit:** `ee33a90`

### The problem
Every AI-scored lead needed individual manual approval, including obvious
high-confidence matches. Reviewing at volume was tedious.

### What shipped
- An optional **score threshold** — leads scoring above it skip manual review.
- **Bulk approve/reject** with multi-select on the leads table.

### Decisions
- The threshold is **off by default**; a human reviews everything until the
  customer explicitly opts into automation.
- **Only pending leads are selectable.** Initially every row was selectable, which
  made it look like you could re-approve already-processed leads. Selection is now
  restricted so the UI can't imply an action that won't happen.

---

## 2026-07-25 — AI outreach email drafting

**Status:** 🟢 Shipped, mock AI provider by default · **Area:** AI
**Commit:** `14f4f13`

### What shipped
The AI turns the client's own base email template into a personalised email per
buyer, grounded in that company's sector and description.

### Decisions
- **The AI does not write from scratch.** The client authors the pitch; the AI
  only personalises the opening and matches tone. This keeps the customer in
  control of their own messaging.
- A campaign can override the tenant's default template — same pattern already
  used for buyer criteria.
- **Known limit:** personalisation is only as good as the company description we
  hold. There's no live "recent news" enrichment, so don't promise real-time
  references yet.

---

## 2026-07-25 — AI lead scoring

**Status:** 🟢 Shipped, mock AI provider by default · **Area:** AI
**Commits:** `b2c11cf`, `d5785a1`

### What shipped
Scores every company in the shared pool against the tenant's buyer criteria and
creates a lead per candidate — pending review if it clears the bar, rejected
otherwise.

### Decisions
- **Provider-agnostic from day one** — mock / OpenAI / Anthropic swap by config,
  no code change. Mock is the default so no one is billed by surprise.
- **Create-only and safe to re-run** — never modifies an existing lead.
- Buyer criteria stay free-form for the AI to interpret holistically, rather than
  being forced into a rigid schema.
- **Reliability fix:** scoring was holding a database connection open across every
  AI call, which would exhaust the connection pool under load. Each lead now
  commits independently, and one bad lead can't abort the batch.

---

## 2026-07-25 — Email draft template schema

**Status:** 🟡 Schema only · **Area:** Schema · **Commit:** `3fcbbc4`

Added storage for a tenant's default email template and per-campaign overrides.
**No editing UI exists yet** — this is the single biggest gap blocking a real
pilot, and is the first item on the roadmap below.

---

## 2026-07-25 — Theming, translation, and rename

**Status:** 🟢 Shipped · **Area:** Platform / Brand · **Commit:** `3ebd28c`

Renamed **AI Export Agent → TargetOut AI**. Added dark/light mode and full
English/Turkish translation, switched from Settings.

**Decision:** language is a user preference, not a URL — no `/tr/` routes. All UI
text must live in both dictionaries; a string in only one language is treated as
a bug and fails the build.

---

## 2026-07-25 — Multi-tenant scaffold

**Status:** 🟢 Shipped · **Area:** Platform · **Commits:** `361dc8a`, `30afc0e`

Backend, dashboard, and database, containerised together.

**Decision — the master pool.** Companies and contacts live in a *shared* pool
across all customers, deduplicated by domain; each tenant gets its own link to a
company with its own independent status. This shared pool is the core long-term
asset of the product. The hard rule that follows: no tenant may ever see another
tenant's leads, campaigns, emails, or settings. Every backend change touching
tenant data goes through an automated isolation review before it ships.

---

## What's next

See [`phases.md`](phases.md) for the forward roadmap, per-phase scope, and
deferred items. That file is the plan; this one is the record. When a phase
completes it moves from there to a new entry at the top of this file.
