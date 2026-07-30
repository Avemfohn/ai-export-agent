# TargetOut AI — Delivery Phases

The forward-looking roadmap: what each phase delivers, what "done" means, and
where we are right now.

> **Companion doc:** [`product-log.md`](product-log.md) is the *backward*-looking
> record — what shipped and the decisions behind it. This file is the *forward*
> plan. When a phase completes, it moves from here into an entry there.

**Status:** ✅ Done · 🔵 In progress · ⚪ Not started · 🔒 Blocked/gated

---

## Progress

| # | Phase | Status | Gate |
|---|---|---|---|
| 0 | Failed-send recovery & visibility | ✅ Done | — |
| 1 | Configuration UI | ⚪ Not started | — |
| 1b | Campaign create/edit | ⚪ Not started | after 1 |
| 2 | Trade-fair upload ingestion | ⚪ Not started | — |
| 3 | Operator controls + notifications | ⚪ Not started | after 2 |
| 4 | Send safety | ⚪ Not started | **must precede 5** |
| 5 | Reply tracking + bounces | 🔒 Not started | needs 4 |
| 6 | WhatsApp notifications | ⚪ Not started | after 5 |
| 7 | Lifecycle completion + follow-ups | ⚪ Not started | after 5 |
| 8 | Tests & hardening | ⚪ Not started | — |

**Overall shape:** phases 1–3 build the product, phase 4 is the safety gate,
phases 5–7 are what happens after a real email goes out. Nothing before phase 4
can reach a real buyer — that's what lets us build quickly on mock data.

---

## ✅ Phase 0 — Failed-send recovery & visibility

**Delivered 2026-07-28** · commit `8b55b29` · see [product-log](product-log.md#2026-07-28--failed-send-recovery--visibility)

A failed send was invisible and unrecoverable — a lead (Provence Maison) was
silently lost and needed a hand-edited database row to recover. Shipped a manual
requeue action plus failure visibility on the outreach table, lead detail page,
and dashboard.

---

## Phase 1 — Configuration UI

**Goal:** let the customer configure the product at all.

**Why it matters:** `buyer_criteria` drives every scoring decision and
`email_draft_template` is the entire "AI personalises *your* pitch" premise —
neither has an editing UI or an update endpoint. The core value proposition is
currently unreachable by its own user. This is the biggest single blocker to a
real pilot.

**Scope**
- Expose the settings fields the API currently withholds (it returns only the
  auto-approve threshold today)
- `PATCH` endpoint for buyer criteria, email template, sender identity fields
- One reusable JSON editor card, used for both criteria and template

**Key decision:** keep the JSON **opaque** — validate it parses, don't impose a
schema. The AI interprets criteria holistically, so a rigid frontend schema would
fight the architecture and break every time the shape shifts.

**Done when:** the customer can edit buyer criteria in the UI, re-run scoring, and
observe different scores and decisions.

---

## Phase 1b — Campaign create/edit

**Goal:** manage campaigns, not just view them.

**Why now:** campaigns carry their own criteria *and* template snapshots, so the
editor is Phase 1's component plus name/description/status. Deferring this means
building the same thing twice.

**Scope:** create/update endpoints (campaigns are read-only today), create modal,
typed form validation.

**Done when:** a campaign can be created and edited end-to-end, with its own
criteria/template overrides applying to its leads.

---

## Phase 2 — Trade-fair upload ingestion

**Goal:** get *real* companies into the system.

**Why it matters:** every lead today is seeded mock data. Nothing has ever
created a real supplier record. File upload is the fastest real ingest path — no
external API, no cost, no webhooks — and the pilot factory already has these
lists.

**Scope:** Excel/CSV upload → parse → shared supplier pool + contacts, recorded as
a job with a proper state machine so async scraping drops into the same shape
later.

**Key risk — the shared pool.** Supplier records are shared across *all*
customers. An upload must only ever *add*, never *overwrite*: one tenant's sloppy
spreadsheet must not corrupt the company data another tenant's scoring depends on.

**Also note:** uploading 500 rows may create only 40 suppliers (the rest are
already pooled), and scoring will later surface companies the customer never
uploaded. That's the shared-pool thesis working correctly — but it must be
explained in the UI or it reads as a bug.

**Deferred within this phase:** PDF parsing. Exhibitor PDFs are an *extraction*
problem, not a parsing one, and need their own AI operation.

**Done when:** a real exhibitor file produces real suppliers; re-uploading the
same file creates nothing new; existing pool records are provably untouched.

---

## Phase 3 — Operator controls + notifications

**Goal:** close the loop on the data that now exists.

**Scope**
- "Score leads" button (the endpoint exists; there is no UI for it at all)
- Per-lead approve/reject on the detail page (endpoint exists, unwired)
- Notification writer — nothing currently writes to the notifications table

**Done when:** a full pass runs from the UI with no curl or SQL: upload → score →
approve → automated send.

---

## Phase 4 — Send safety 🔒

**Goal:** make it impossible to accidentally blast real buyers.

**Why it gates Phase 5:** turning on Mailgun for *inbound* mail simultaneously
arms the *outbound* scheduler, which runs unconditionally on every boot and drains
the whole queue at ~1/min with no cap and no kill switch. Today the only thing
protecting us is that the email provider defaults to mock.

**Scope**
- Kill switch, recipient allowlist, daily cap
- Suppression list — hard bounces and unsubscribes blocked permanently
  (deliverability, not the compliance work that's deferred)
- "Sent today / cap" counter so the operator can answer *"did we send anything
  weird overnight?"* without a database query

**Done when:** with sending disabled nothing goes out; with an allowlist only
allowed recipients go out; the cap halts sending and the counter reflects it.

---

## Phase 5 — Reply tracking + bounces

**Goal:** the product differentiator — know who replied and what they meant.

**Scope**
- Inbound webhook → store reply → AI classifies intent → warm replies surface
- **Bounce/delivery webhook** — the only path to bounced status, and what protects
  sender reputation on a cold domain
- Lead status cascades to Interested / Not Interested / Bounced

**Key risk — this is the hardest tenant-isolation problem in the project.** The
webhook is a public, unauthenticated endpoint with a fully attacker-controlled
payload. Two rules: the tenant must be resolved *only* from a server-generated
token we put on the outgoing email, and replies must **never** be matched by
sender address — the same buyer legitimately appears across multiple customers,
so "closest match" is a cross-tenant data leak that looks fine in review.

**Done when:** a signed test payload resolves to the right tenant and cascades the
lead; a tampered signature and a replayed payload are both rejected.

---

## Phase 6 — WhatsApp notifications

**Goal:** alert the customer to a warm reply without them watching a dashboard.

The number field and notification channel already exist in the schema and are
never used. Follows Phase 5 because warm replies are the only meaningful trigger.

**Done when:** a warm reply produces a WhatsApp message to the configured number.

---

## Phase 7 — Lifecycle completion + follow-ups

**Goal:** finish the lead lifecycle and stop leaving money on the table.

Three lead statuses are still unreachable:
- **No response** — needs a "sent N days ago, still silent" job
- **Converted** — a manual operator action; the only way the dashboard can ever
  show ROI, which is what renews the pilot contract
- **Bounced** — arrives via Phase 5

**The real prize: follow-up sequences.** Today the product sends *one email per
lead, ever*. For cold outreach that's a major functional gap — most reply volume
comes from follow-ups.

**Done when:** a lead with no reply is followed up automatically, and conversions
can be recorded.

---

## Phase 8 — Tests & hardening

**Goal:** make it safe to change.

Test coverage is effectively zero — one empty smoke test that needs a live
database.

**Scope:** real-Postgres integration tests (not H2 — this app's correctness is
JSONB + constraint + migration shaped, and H2 lies about all three), one
end-to-end happy path, health endpoint, and replacing the two endpoints that leak
database entities directly.

**Done when:** the end-to-end path runs in CI and a broken migration or constraint
fails the build.

---

## Deferred

| Item | Un-defer when |
|---|---|
| Customer login / accounts | A second real client exists. Isolation is already built; only the request filter changes. |
| Google Maps / Apify scraping | Upload proves the ingest path and the pilot wants volume beyond their own lists |
| PDF parsing | Excel/CSV covers most exhibitor lists |
| **Unsubscribe link + GDPR lawful basis** | **Before the first real email to an EU buyer.** Targets are FR/DE/UK/NL — a hard legal gate, not a nice-to-have. |
| Per-customer sender identity | Same gate. Today everything would send from one shared address, which hurts deliverability and looks wrong to recipients. |

---

## Maintaining this file

**On starting a phase:** flip its status to 🔵 in the progress table.

**On completing a phase:**
1. Flip status to ✅ in the progress table, and add the delivery date + commit.
2. Collapse the phase's section to a short summary linking to its
   [`product-log.md`](product-log.md) entry (Phase 0 above is the template).
3. Add the full entry to `product-log.md` — problem, what shipped, **decisions**,
   what was verified, what was deferred.
4. If the phase changed a product rule that must not be accidentally reverted,
   also record it in `CLAUDE.md` (as the no-auto-retry rule was).
5. Re-check the remaining phases — completing one often reveals the next needs
   reordering. Update this file if so, and say why.
