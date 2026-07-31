-- Campaign status now gates outreach: a lead is only drafted/queued if it has
-- no campaign, or its campaign is ACTIVE.
--
-- Seed lead 4004 (Provence Maison) sits on campaign 3002, which is DRAFT. V6's
-- header explicitly keeps 4004 as PENDING_APPROVAL "so the normal approve ->
-- auto-draft -> auto-send path stays demonstrable" — under the new gate that
-- demo would silently stop working, which is exactly the invisible-failure mode
-- Phase 0 existed to remove.
--
-- Move it to campaign 3001 (ACTIVE) rather than flipping 3002 to ACTIVE: DRAFT
-- would otherwise have no seed representation, and V2/V6's philosophy is that
-- mock data should make every status reachable in the UI. The blocked state is
-- better demonstrated by pausing a campaign live.
--
-- V2 is not edited — it is already applied, and changing it would fail Flyway's
-- checksum validation on existing databases.
UPDATE tenant_leads
SET tenant_campaign_id = '00000000-0000-0000-0000-000000003001'
WHERE id = '00000000-0000-0000-0000-000000004004';
