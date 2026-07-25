# AI Export Agent

A B2B SaaS that automates finding, qualifying, and emailing international
buyers for exporters/manufacturers — scraping target-sector companies, AI
filtering them against a client's buyer criteria, drafting and sending
personalized cold emails, and classifying replies so warm leads surface on a
dashboard in real time.

## Quick Start

```bash
docker compose up --build
```

- Frontend: [http://localhost:3000](http://localhost:3000)
- Backend API: [http://localhost:8080](http://localhost:8080)
- Postgres: `localhost:5432`

## Status

Sprint 1: full flow and UI running on mock data seeded into a real Postgres
schema. No real AI/scraping/email provider calls yet.

See [`CLAUDE.md`](CLAUDE.md) for the full architecture, the multi-tenant
"Master Pool" data model, tech stack, and current sprint details.
