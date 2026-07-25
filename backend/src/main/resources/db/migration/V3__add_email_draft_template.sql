-- Design decision: outreach emails are AI-CUSTOMIZED from a client-authored
-- base draft template, not freely AI-generated from scratch (see CLAUDE.md).
-- Mirrors the existing buyer_criteria -> buyer_criteria_snapshot pattern:
-- tenant_settings holds the tenant's standing default template, and each
-- tenant_campaigns row can snapshot/override it for that campaign's specific
-- product line or region.

ALTER TABLE tenant_settings
    ADD COLUMN email_draft_template JSONB NOT NULL DEFAULT '{}'::jsonb;

ALTER TABLE tenant_campaigns
    ADD COLUMN email_draft_template_snapshot JSONB NOT NULL DEFAULT '{}'::jsonb;

-- =========================================================
-- Populate example values on the existing Sprint 1 seed data
-- (V2__seed_mock_data.sql) so it stays representative.
-- =========================================================

UPDATE tenant_settings
SET email_draft_template = '{
    "subject": "Turkish home textile manufacturer for {{companyName}}",
    "body": "Hi {{contactFirstName}},\n\nWe are Anatolia Textiles Export, a Turkey-based manufacturer of bedding, towels, and home textiles, supplying importers across North America and Europe.\n\nI wanted to reach out directly to {{companyName}} given your position in the {{sector}} space.\n\nWould you be open to a quick call this week?\n\nBest,\n{{senderName}}",
    "notes": "AI may personalize the opening paragraph with the recipient real products/recent news and match their regional tone, but must not alter pricing claims or the core value proposition."
}'::jsonb
WHERE id = '00000000-0000-0000-0000-0000000000c1';

UPDATE tenant_settings
SET email_draft_template = '{
    "subject": "Ceramics manufacturer partnership with {{companyName}}",
    "body": "Hi {{contactFirstName}},\n\nWe are Demo Ceramics Co, manufacturing ceramics and tableware for export partners across Europe and the GCC.\n\n{{companyName}} caught our attention as a strong fit for a direct-from-factory partnership.\n\nOpen to a short call?\n\nBest,\n{{senderName}}",
    "notes": "AI may personalize the opening paragraph with the recipient real products/recent news, but must not alter pricing claims or the core value proposition."
}'::jsonb
WHERE id = '00000000-0000-0000-0000-0000000000c2';

UPDATE tenant_campaigns
SET email_draft_template_snapshot = '{
    "subject": "Turkish home textile manufacturer for {{companyName}}",
    "body": "Hi {{contactFirstName}},\n\nWe are Anatolia Textiles Export, supplying home textile importers across the US and Canada with direct-from-factory bedding and towels.\n\n{{companyName}} stood out to us in the North American market.\n\nWould you be open to a quick call this week?\n\nBest,\n{{senderName}}",
    "notes": "North America campaign: emphasize US/Canada logistics and lead times."
}'::jsonb
WHERE id = '00000000-0000-0000-0000-000000003001';

UPDATE tenant_campaigns
SET email_draft_template_snapshot = '{
    "subject": "Direct-from-factory home textiles for {{companyName}}",
    "body": "Hi {{contactFirstName}},\n\nWe are Anatolia Textiles Export, a Turkey-based manufacturer supplying home textile importers across Western Europe.\n\n{{companyName}} stood out to us given your presence in the region.\n\nOpen to a short call?\n\nBest,\n{{senderName}}",
    "notes": "Western Europe campaign: emphasize EU import compliance and shorter shipping times from Turkey."
}'::jsonb
WHERE id = '00000000-0000-0000-0000-000000003002';

UPDATE tenant_campaigns
SET email_draft_template_snapshot = '{
    "subject": "Ceramics manufacturer partnership with {{companyName}}",
    "body": "Hi {{contactFirstName}},\n\nWe are Demo Ceramics Co, manufacturing ceramics and tableware for export partners across the EU.\n\n{{companyName}} caught our attention as a strong fit for a direct-from-factory partnership.\n\nOpen to a short call?\n\nBest,\n{{senderName}}",
    "notes": "EU campaign: emphasize CE compliance and EU-based sample shipping."
}'::jsonb
WHERE id = '00000000-0000-0000-0000-000000003003';

UPDATE tenant_campaigns
SET email_draft_template_snapshot = '{
    "subject": "Ceramics and furniture sourcing for {{companyName}}",
    "body": "Hi {{contactFirstName}},\n\nWe are Demo Ceramics Co, manufacturing ceramics, tableware, and furniture for export partners across the GCC.\n\n{{companyName}} stood out to us given your presence in the region.\n\nOpen to a short call?\n\nBest,\n{{senderName}}",
    "notes": "GCC campaign: emphasize Gulf shipping routes and bulk order pricing."
}'::jsonb
WHERE id = '00000000-0000-0000-0000-000000003004';
