-- Phase 2: trade-fair upload ingestion.
--
-- Both changes are additive and forward-only. They run against live demo data
-- on promotion to `production` (see docs/deployment.md), so nothing here drops
-- or rewrites an existing row.

-- 1. Uploaded contacts need a truthful provenance value.
--
-- The existing CHECK allows only WEBSITE_SCRAPE / ENRICHMENT_API / MANUAL.
-- Labelling an uploaded contact MANUAL would be a lie baked into the core data
-- asset, and provenance is exactly the thing that is painful to reconstruct
-- later. Postgres has no ALTER for a CHECK, so this is DROP + ADD.
--
-- V1 declared the constraint inline and therefore unnamed, leaving Postgres to
-- auto-name it `global_supplier_contacts_source_check`. IF EXISTS guards the
-- case where that assumption is wrong, and the re-added constraint is named
-- explicitly so the next migration does not have to guess.
--
-- Widening a CHECK cannot fail against existing rows: V2 seeds only
-- WEBSITE_SCRAPE and ENRICHMENT_API, both still permitted.
ALTER TABLE global_supplier_contacts
    DROP CONSTRAINT IF EXISTS global_supplier_contacts_source_check;

ALTER TABLE global_supplier_contacts
    ADD CONSTRAINT global_supplier_contacts_source_check
        CHECK (source IN ('WEBSITE_SCRAPE', 'ENRICHMENT_API', 'MANUAL', 'TRADE_FAIR_UPLOAD'));

-- 2. Provenance for pool rows contributed by an upload.
--
-- global_suppliers is the shared pool and deliberately has NO tenant_id (see
-- CLAUDE.md "Master Pool Architecture"). Pointing at the already-tenant-scoped
-- scraping_jobs row gives full traceability -- which tenant contributed a
-- company, and in which import -- WITHOUT putting a tenant_id on a global
-- table. It also makes cleaning up a bad import surgical: one job id.
--
-- Nullable because every row seeded before this migration predates it, and
-- because of the delete rule below.
--
-- ON DELETE SET NULL is load-bearing, not a default. scraping_jobs.tenant_id is
-- ON DELETE CASCADE from tenants, so with the default NO ACTION a shared-pool
-- row would *veto deleting a tenant* -- a global record blocking a tenant
-- operation, which inverts the master-pool boundary and would block GDPR
-- erasure. Provenance is deliberately sacrificed when its tenant is deleted.
ALTER TABLE global_suppliers
    ADD COLUMN source_scraping_job_id UUID
        REFERENCES scraping_jobs(id) ON DELETE SET NULL;

CREATE INDEX idx_global_suppliers_source_scraping_job
    ON global_suppliers(source_scraping_job_id)
    WHERE source_scraping_job_id IS NOT NULL;
