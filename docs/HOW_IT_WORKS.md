# How AI Export Agent Works

## 1. What this app does

AI Export Agent helps exporters and manufacturers find, qualify, and email
international buyers — without a sales team manually trawling Google Maps,
B2B directories, and trade-fair spreadsheets. It's being built as an MVP for
one pilot factory client first, with the underlying data model already
designed to scale into a multi-tenant SaaS product serving many factories at
once.

## 2. The business flow

1. **Scraping** — target-sector companies are gathered from Google Maps,
   B2B directories, and trade-fair lists the client uploads (Excel/PDF).
2. **AI filtering** — an LLM checks each scraped company's website against
   the client's buyer criteria (sectors, regions, minimum size, etc.) and
   rejects non-matches before they ever become a lead.
3. **Outreach** — for matches, the system finds a corporate contact, an AI
   drafts a personalized cold email referencing the company's real products
   or recent news, and sends it.
4. **Reply tracking** — when a reply comes in, AI classifies the sender's
   intent (interested, not interested, needs more info, out of office,
   etc.). Warm ("interested") replies surface on the dashboard and trigger a
   WhatsApp notification so nothing gets missed.

**What's real right now vs. mocked:** the database schema, backend API, and
dashboard UI are fully built and wired together — every screen you see is
reading real rows from a real Postgres database, through a real REST API.
What's *not* wired in yet is the AI/scraping/email providers themselves
(OpenAI/Anthropic, Apify/Bright Data, Mailgun) — this sprint's data is
realistic seed data standing in for what those integrations will produce.

## 3. The "Master Pool" model, in plain terms

Every company the system ever scrapes — across *every* client — goes into
one shared pool. If two different factories are both targeting, say, home
textile importers in Germany, and both target companies happen to overlap,
the system doesn't scrape and store that company twice. It's discovered
once, stored once, and each client gets their own independent relationship
to it: their own status (still deciding, emailed, interested, converted...),
their own emails, their own replies.

**The guarantee:** one client's leads, emails, replies, and notifications are
never visible to another client. The shared pool of companies is the
product's core asset — it makes the system smarter over time — but each
client's *relationship* to those companies is always kept completely
separate.

## 4. Screen-by-screen UI guide

The dashboard's left sidebar has eight sections. Here's what each one shows.

### Overview
The landing page. Four stat cards summarize the account at a glance: **Total
Leads**, **Warm Replies** (replies classified as "interested"), **Active
Campaigns**, and **Scraping Jobs**. Numbers are fetched live on every page
load; a card shows a dash (—) if the backend couldn't be reached at that
moment, so a slow connection never looks like "zero leads."

### Leads
A table of every company the system has matched against your buyer
criteria, with columns for **Company**, **Domain**, **Country**, **Sector**,
**Status**, and **Score** (the AI's qualification confidence). Clicking a
row opens the lead's detail page, which shows the qualification notes (why
the AI matched or scored it the way it did), plus sections for outreach
emails sent to that lead and any replies received.

A lead's **Status** moves through a lifecycle as the outreach process
progresses: `PENDING_APPROVAL` (matched, awaiting your review) →
`APPROVED`/`REJECTED` → `EMAIL_SENT` → then one of `NO_RESPONSE`,
`INTERESTED`, `NOT_INTERESTED`, `BOUNCED`, or — the goal — `CONVERTED`.

### Campaigns
Campaigns group leads and scraping jobs together under a named outreach
push (e.g. "Home Textiles — North America"). Each card shows the campaign's
**name**, **status** (`DRAFT`, `ACTIVE`, `PAUSED`, `COMPLETED`, or
`ARCHIVED`), and **description**. Clicking a campaign opens its detail page,
listing the outreach emails sent as part of it.

### Outreach
Every cold email the system has sent (or drafted), with **To**, **Subject**,
**Status** (`DRAFT` → `QUEUED` → `SENT`, or `FAILED`/`BOUNCED`), and **Sent
At**. This is the audit trail of everything that's gone out to buyers.

### Responses
The "warm reply" inbox. Every reply received to an outreach email, with the
sender, a color-coded **classified intent** badge (green for `INTERESTED`,
red for `NOT_INTERESTED`/`SPAM`/`UNSUBSCRIBE`, amber for `NEEDS_INFO`, gray
for `OUT_OF_OFFICE`/`UNKNOWN`), the subject line, a preview of the message
body, and when it was received. This is the page that matters most day to
day — it's where genuinely interested buyers show up first.

### Notifications
System alerts: warm-reply alerts (also pushed to WhatsApp), new-lead alerts,
scraping-job completions, and bounce alerts. Each shows a **channel** badge
(Dashboard, WhatsApp, or Email), the message, whether it's been read, and
when it was sent. Unread notifications are visually highlighted.

### Scraping Jobs
A log of every scraping run: its **source** (Google Maps, a B2B directory, a
trade-fair upload, or manual entry), **status** (`PENDING`, `RUNNING`,
`COMPLETED`, `FAILED`, `CANCELLED`), how many companies it found, and — for
failed jobs — the error message.

### Settings
Where the client's **buyer criteria** live: target sectors, target
countries, minimum company size, excluded keywords, preferred languages.
This is what the AI filtering step checks every scraped company against.
**Currently read-only** — this sprint displays the expected fields but isn't
yet wired to a real save endpoint.

## 5. Current limitations (Sprint 1)

- All data on screen is realistic **seed data**, not the output of a live
  scrape or a live AI call.
- No real OpenAI/Anthropic, Apify/Bright Data, or Mailgun calls are made yet
  — those integrations are stubbed out for a later sprint.
- There's **no login yet** — the app runs as a single hardcoded demo tenant.
- The **Settings** page is read-only; buyer criteria can't be edited from
  the UI yet.

See [`CLAUDE.md`](../CLAUDE.md) for the full technical architecture,
database schema, and tech stack.
