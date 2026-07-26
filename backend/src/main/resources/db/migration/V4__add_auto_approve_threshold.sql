-- Optional per-tenant auto-approve threshold: when set, AI-scored leads
-- meeting or exceeding this score skip the PENDING_APPROVAL review queue
-- and are created directly as APPROVED. Nullable with no default so
-- existing tenants are unaffected (NULL = feature off) -- no backfill
-- needed, unlike V3's NOT NULL JSONB columns.

ALTER TABLE tenant_settings
    ADD COLUMN auto_approve_threshold NUMERIC(5,2);
