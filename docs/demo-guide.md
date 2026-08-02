# TargetOut AI — Demo Guide

A 15-minute walkthrough for showing the product to someone in person.

Everything below runs on **seeded mock data**, a **mock AI provider** and a
**mock email sender**. Nothing costs money and no email leaves the machine, but
every click is real: real Postgres, real API calls, real scheduled jobs.

> **Companion docs:** [`phases.md`](phases.md) — what's coming next ·
> [`product-log.md`](product-log.md) — what shipped and why ·
> [`deployment.md`](deployment.md) — putting this on a URL you can send someone.

**Demoing remotely?** Everything below works unchanged against a deployed URL —
substitute it for `http://localhost:3000` and skip the pre-flight `docker`
commands. See [`deployment.md`](deployment.md).

---

## Before they arrive (10 minutes)

### 1. Reset to a clean demo state

```bash
docker compose down -v
docker compose up --build
```

**The `-v` matters.** It drops the database volume so Flyway re-seeds from
scratch. Without it, an earlier run's approvals leave the Pending Approval
queue empty — and Act 3, the best part of the demo, has nothing to approve.

Wait for the backend log line about Flyway finishing migrations, then open
**http://localhost:3000**. First page load compiles on demand and can take a
few seconds — do this before your friend is watching.

### 2. Confirm the seed is clean

Check these three things. If any is wrong, run the reset again.

| Where | Expect |
|---|---|
| Overview | **Failed Sends = 1** |
| Leads | **Provence Maison** → Pending Approval |
| Leads | **Maple & Co Home Goods** → Approved |

### 3. Know that nothing will fire on its own

Two background jobs run every 60 seconds, so it's fair to wonder whether the
demo will drift while you talk. It won't: on a fresh database there is nothing
for them to pick up. The one approved lead already has a failed email attached
(the system treats that lead as handled), and the other tenant's approved lead
sits under a paused campaign. **The pipeline stays still until you act.**

### 4. Set the scene in one sentence

> "This is the dashboard for a Turkish home-textiles factory that wants to sell
> to importers in Europe and North America. The software finds those buyers,
> checks whether they're worth contacting, writes the first email, sends it, and
> tells them who replied."

---

## Act 1 — The dashboard (1 min)

**Go to:** Overview

Five numbers, left to right. Don't explain the product yet — let the numbers do it.

- **Total Leads** — buyers found and qualified for this factory
- **Warm Replies** — buyers who replied wanting to talk. *This is the number the
  customer actually pays for.*
- **Active Campaigns** — outreach pushes currently allowed to send
- **Scraping Jobs** — where the companies came from
- **Failed Sends: 1** — ← **point at this and say "we'll come back to that"**

Planting the failure now makes Act 5 feel like a payoff instead of a bug demo.

---

## Act 2 — The pipeline (3 min)

**Go to:** Leads

One row per buyer, with a **Score** the AI assigned and a **Status** showing how
far along the conversation is. Scroll the status column and narrate the
lifecycle out loud:

> Pending Approval → Approved → Email Sent → Interested / Not Interested /
> No Response / Bounced → Converted

Point out the **Campaign** column — buyers can be grouped so they get different
pitches. That's Act 4.

**Now click into `Nordic Linen House`** (status: Interested).

This single page is the whole product:

- **Qualification Score 91** with the AI's reasoning under *Qualification Notes*
- **Outreach Emails** — the actual email that was sent, subject and body
- **Responses** — the buyer's reply, *"This looks interesting, please send a full
  catalog and MOQs"*, tagged **Interested** by the AI

> "Nobody at the factory wrote that email, chose that company, or read that
> reply first. They just saw a green Interested badge."

---

## Act 3 — Approve → automatic send (5 min)

**This is the money shot.** Measured on a clean stack, approve → sent took
**75 seconds**; worst case is about two minutes if you just miss both 60-second
ticks. The script below fills that time with the most interesting screens in the
app, so you're never watching a static table.

### 3a. Approve the lead (30 sec)

**Go to:** Leads → set the **Status** filter to **Pending Approval**

**Provence Maison** appears — a French home-decor chain the AI scored 69.5 and
flagged for human review.

1. Tick the checkbox on its row
2. A toolbar appears at the top — click **Approve Selected**
3. The status flips to **Approved**

> ⚠️ **Do this from the table, not the detail page.** Approve/reject lives in
> the bulk toolbar. There is no approve button inside a lead's detail page yet.

Say what happens next, then walk away from the screen:

> "That's the only human decision in the whole flow. From here the system drafts
> the email and sends it on its own — takes about a minute or two. Let me show
> you what it's using to write it."

### 3b. Fill the wait — the Settings tour (~2 min)

**Go to:** Settings

**Buyer Criteria** — the rules the AI qualifies companies against.

Click **Test criteria** first, unchanged. Five real companies come back scoring
**80–100**, each with the AI's reasoning, all marked *Would await review*.

Now show it discriminating. **Replace the Keywords with something off-target** —
type `ceramics, porcelain` — and click **Test criteria** again:

> Every score collapses from ~95 to **20**, and every verdict flips to
> **Would reject**.

> "Same companies, different rules. That's the filter doing its job — it won't
> waste a send on a buyer in the wrong sector."

**Don't save.** Reload the page to restore the real criteria.

> **Two things to expect, so they don't surprise you live:**
> - With default settings every result says *Would await review*, never
>   *Would auto-approve*. That label only appears once **Auto-Approve Threshold**
>   is switched on further down the page — worth demoing if there's interest, but
>   it changes saved settings.
> - The sample is always the same five home-textiles companies. It's capped at
>   five for speed, not randomised.

**Email Template** — the pitch, written by the client, not the AI.

- Click **Show preview** to see `{{companyName}}` and `{{contactFirstName}}`
  filled in with a real company
- The **Instructions for the AI** field is the guardrail — *"may personalise the
  opening paragraph, but must not change pricing claims"*

> "This is the important design decision. The AI doesn't write a cold email from
> nothing — the client writes the pitch once, and the AI's only job is
> personalising the opening for each company. The client keeps control of what's
> actually being promised."

**Sending Identity** — greyed out, and that's deliberate.

> "The from-name and address are set up by us during onboarding, together with
> the domain's spam authentication. If a customer could change it themselves,
> their mail would start landing in spam folders."

**Auto-Approve Threshold** — for customers who trust the scoring, leads above a
chosen score skip the review queue entirely.

### 3c. The payoff (1 min)

**Go to:** Outreach

A new row: **camille.fabre@provencemaison.fr**, subject personalised to Provence
Maison, status **Sent**.

**Go back to:** Leads → Provence Maison is now **Email Sent**.

> *If it hasn't landed yet:* the row will be **Queued** — the send job runs on a
> 60-second cycle, deliberately slow so a new sending domain doesn't get flagged
> as a spammer. Refresh in a moment. Meanwhile, go to Act 4 and come back.

---

## Act 4 — Campaigns and the safety switch (3 min)

**Go to:** Campaigns → **Q3 2026 Home Textiles Outreach - North America**

A campaign groups buyers who should get the same pitch — a different product
line, or a different region — with its own email template overriding the default.
Its assigned leads are listed on the page.

Before you touch anything, the page already warns you:

> *"1 approved leads will stop being emailed if you pause or archive this
> campaign."*

**Now hit the brakes.** Click **Move to Paused** (three plain buttons — *Move to
Paused*, *Move to Completed*, *Move to Archived* — not a dropdown).

The warning changes to tell you what you just did:

> *"1 approved leads are waiting — they won't be emailed until this campaign is
> active."*

> "One click stops outreach for this whole group. Nothing is deleted, nothing is
> lost — the leads just wait."

**Worth one extra click:** go to **Leads**. The paused campaign's rows now carry
*"Campaign not active — not sending"* right in the table, so the block is visible
where someone would actually notice it, not just on the campaign page.

Worth saying out loud, because it's a real design decision:

> "Pausing stops *new* emails. It doesn't recall mail that's already in the send
> queue — trying to claw those back would jam the queue and stop email for every
> customer on the system, not just this one."

**Click Move to Active** before you move on, so you finish in a clean state.

> *Accuracy note, if someone is paying close attention:* Act 5's requeue would
> send **even with this campaign paused**, because the send job deliberately
> ignores campaign status — which is exactly the "pausing doesn't recall queued
> mail" point above. Don't volunteer it unless asked; it's a good answer, not a
> good tangent.

Optional, if there's interest: **New campaign** shows the create form, which
starts pre-filled with a copy of the default template so there's never a blank
page.

---

## Act 5 — When a send fails (3 min)

**Go to:** Outreach

Find the row marked **Failed** — Maple & Co Home Goods, with the reason shown
in plain text:

```
Mailgun send failed: 401 Unauthorized - invalid API key
```

> "This is the failure mode that kills outreach tools. An email doesn't go out,
> nobody notices, and a buyer worth six figures just never gets contacted.
> Here it's on the dashboard, on the lead, and on the front page — and it says
> why."

Click **Requeue**. The status becomes **Queued**, and within about a minute
(measured: 51 seconds) it's **Sent** — and the lead cascades to **Email Sent**.

If asked why it isn't retried automatically:

> "On purpose. Redrafting costs a real AI call. If the system retried by itself
> and the problem was a bad API key, it would burn a paid call every 60 seconds
> forever without ever succeeding. A person clicks the button once the cause is
> actually fixed."

---

## Act 6 — The closer (1 min)

**Go to:** Settings

- **Language → Türkçe** — the entire dashboard flips, including every status
  badge. Not a URL switch, not a half-translated menu.
- **Appearance → Dark / Light**

> "Built for Turkish exporters, so the product speaks Turkish."

Switch back to English if you're handing over the laptop.

---

## Appendix A — Under the hood

*Skip this unless your friend writes software.*

**Run the AI qualification manually.** There's no button for this yet — the
operator UI is on the roadmap — but the endpoint is live:

```bash
curl -X POST http://localhost:8080/api/leads/score
```

It scores every company in the shared pool this tenant doesn't already have a
lead for, and creates a lead for each: approved for review if it clears the
score threshold, rejected otherwise. Re-running it is safe — it never touches an
existing lead.

On the seeded database it returns exactly this:

```json
{"suppliersEvaluated":6,"matched":0,"autoApproved":0,"rejected":6,"failed":0}
```

⚠️ **Two caveats before you run it live:**

1. **It writes rows** — six new `Rejected` leads, taking the Leads page from 10
   to 16. Run it at the *end* of the demo, or reset afterwards.
2. **Everything gets rejected, and that's correct.** The tenant already has leads
   for all ten home-textiles companies, so the only candidates left are ceramics
   and furniture importers, which score 20–35 against home-textiles criteria.
   Good story ("the filter refuses to waste sends on the wrong sector") — just
   don't promise a screen full of new matches.

**Other things worth showing a developer:**

- `docker compose logs -f backend` — the two scheduled jobs ticking every 60s
- Postgres on `:5432` (`aiexportagent` / `aiexportagent_dev`) — the
  `global_suppliers` table has **no `tenant_id` column**, which is the whole
  architecture in one observation

---

## Appendix B — What's real and what's mock

Be upfront about this. It's a stronger position than letting someone discover it.

| Piece | State |
|---|---|
| Database, API, dashboard, scheduling | **Real.** Postgres, Spring Boot, Next.js |
| Multi-tenant isolation | **Real** and enforced on every query |
| AI scoring & drafting | **Real code path**, running a deterministic mock provider. Switching to OpenAI or Anthropic is one environment variable — no code change |
| Email sending | **Real pipeline**, mock sender. Mailgun is wired and one env var away |
| Reply classification | **Seeded**, not live. The inbound webhook is the next major piece |
| Scraping | **Not built.** The Scraping Jobs page shows seeded history; file upload is the next phase |
| Login | **Not built.** One fixed tenant for development |

### The question you will get: "why can't I see the other company's data?"

There are two tenants in the database. You only ever see one.

> "Company records are *shared* across every customer — one pool of buyers that
> gets richer as more customers use the product. That pool is the asset. But
> which buyers *you're* pursuing, what you said to them, and what they replied is
> yours alone and never visible to anyone else. Same company, separate
> conversations."

---

## Appendix C — Quick answers

**"Does it only send one email? Nobody replies to one cold email."**
Correct, and that's the single biggest gap right now. Follow-up sequences are
planned — most replies to cold outreach come from the second and third touch.

**"What if the email address is wrong?"**
It comes back as Bounced and the lead is marked. Live bounce handling arrives
with the inbound webhook.

**"What's the difference between the template in Settings and in a Campaign?"**
Settings holds the default pitch. A campaign can override it — a different
product line or region gets its own angle without disturbing everyone else.

**"When could this send real email?"**
Technically today — it's one environment variable. Deliberately not done yet:
there's no kill switch, no daily cap and no recipient allowlist, and those land
before anything reaches a real buyer.

**"What about GDPR / unsubscribe?"**
Required before the first real email to an EU buyer, and tracked as a hard gate.
The targets are France, Germany, the UK and the Netherlands, so this isn't
optional.

---

## If something goes wrong mid-demo

| Symptom | Fix |
|---|---|
| Cards show `—` and "Backend unavailable" | Backend still starting. Wait, then reload |
| Nothing in Pending Approval | Database wasn't reset. `docker compose down -v && docker compose up` |
| Approved lead won't send | Its campaign is paused — check the Campaign column on the leads table |
| Email stuck on Queued | Normal. The send job is a 60-second cycle |
| Port 3000 or 8080 in use | An old stack is running: `docker compose down` first |
