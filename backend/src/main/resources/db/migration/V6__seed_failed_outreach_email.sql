-- Seeds one FAILED outreach email so the failed-send recovery UI (error text
-- + requeue action) is visible on a fresh database. V2 seeds 11 outreach
-- emails, all SENT except one BOUNCED, so without this the entire recovery
-- path renders zero times on a clean `docker compose up -v`.
--
-- Same rationale as V2's existing BOUNCED row: mock data exists to make every
-- status reachable in the UI. V2 itself is not edited — it's already applied,
-- and changing it would fail Flyway's checksum validation on existing DBs.
--
-- Attached to lead 4005 (Maple & Co Home Goods), which is APPROVED and had no
-- outreach_email of its own, so requeueing it exercises the whole cascade:
-- FAILED -> QUEUED -> SENT -> lead becomes EMAIL_SENT. Lead 4004 is left
-- PENDING_APPROVAL so the normal approve -> auto-draft -> auto-send path stays
-- demonstrable too.
INSERT INTO outreach_emails (id, tenant_id, tenant_lead_id, to_email, subject, body, status, provider_message_id, error_message, sent_at) VALUES
    ('00000000-0000-0000-0000-000000005012', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000004005', 'olivia.bennett@mapleandcohome.ca', 'Turkish home textile manufacturer for Maple & Co Home Goods', 'Hi Olivia, noticed Maple & Co Home Goods has been expanding its home textiles range...', 'FAILED', NULL, 'Mailgun send failed: 401 Unauthorized - invalid API key', NULL);
