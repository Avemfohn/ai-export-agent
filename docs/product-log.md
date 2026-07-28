# TargetOut AI — Product Change Log

A running record of what shipped, why it shipped, and what we decided along the
way. Written for product review, not code review — each entry leads with the
user problem, not the diff.

**Conventions**
- Newest first. One entry per meaningful product change (not per commit).
- "Decisions" captures choices we'd otherwise forget and re-litigate later.
- Status: 🟢 Shipped · 🟡 Built, not enabled · ⚪ Planned

---

## At a glance

| Date | Change | Area | Status |
|---|---|---|---|
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

Agreed roadmap, phase by phase. Each phase gets its own plan and approval.

| # | Phase | Why it matters |
|---|---|---|
| 1 | **Configuration UI** — edit buyer criteria + email template | **The customer currently cannot configure the product at all.** Biggest blocker. |
| 1b | Campaign create/edit | Reuses the same editors |
| 2 | Trade-fair file upload (Excel/CSV) | First route for *real* companies into the system |
| 3 | Operator controls — run scoring from the UI, per-lead approve | Closes the loop on real data |
| 4 | Send safety — kill switch, allowlist, daily cap, suppression list | Must land before any real email is sent |
| 5 | Reply tracking + bounce handling | The product differentiator; activates warm-reply detection |
| 6 | WhatsApp notifications | Alerting on warm replies |
| 7 | Follow-up sequences | Today we send *one* email per lead, ever — most replies come from follow-ups |
| 8 | Automated tests & hardening | Test coverage is currently near zero |

### Deliberately deferred

| Item | Un-defer when |
|---|---|
| Customer login / accounts | A second real client exists. One pilot doesn't need it yet. |
| Google Maps / directory scraping | File upload proves the ingest path first |
| **Unsubscribe link & GDPR basis** | **Before the first real email to an EU buyer** — legal gate, not optional |
| Per-customer sender identity | Same gate — today everything would send from one shared address |
