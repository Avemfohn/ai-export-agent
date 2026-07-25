-- Sprint 1 mock data. Fixed, readable UUIDs so rows are referenceable/greppable
-- across this script. Tenant A's id matches the DEV_TENANT_ID used by
-- TenantContextFilter in local/docker dev (see docker-compose.yml).
--
-- Tenant A = 00000000-0000-0000-0000-000000000001 (pilot client)
-- Tenant B = 00000000-0000-0000-0000-000000000002 (second tenant, exists
--            purely to prove tenant isolation / the shared master-pool model)

-- =========================================================
-- tenants
-- =========================================================
INSERT INTO tenants (id, name, slug, status) VALUES
    ('00000000-0000-0000-0000-000000000001', 'Anatolia Textiles Export', 'anatolia-textiles', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000002', 'Demo Ceramics Co', 'demo-ceramics', 'TRIAL');

-- =========================================================
-- tenant_users
-- =========================================================
INSERT INTO tenant_users (id, tenant_id, email, full_name, role, status) VALUES
    ('00000000-0000-0000-0000-0000000000a1', '00000000-0000-0000-0000-000000000001', 'owner@anatolia-textiles.example', 'Elif Yildiz', 'OWNER', 'ACTIVE'),
    ('00000000-0000-0000-0000-0000000000a2', '00000000-0000-0000-0000-000000000001', 'sales@anatolia-textiles.example', 'Mert Demir', 'MEMBER', 'ACTIVE'),
    ('00000000-0000-0000-0000-0000000000b1', '00000000-0000-0000-0000-000000000002', 'owner@demo-ceramics.example', 'Dana Kovacs', 'OWNER', 'ACTIVE');

-- =========================================================
-- tenant_settings
-- =========================================================
INSERT INTO tenant_settings (id, tenant_id, buyer_criteria, target_sectors, target_regions, email_sender_name, email_sender_address, whatsapp_notify_number, notification_prefs) VALUES
    ('00000000-0000-0000-0000-0000000000c1', '00000000-0000-0000-0000-000000000001',
     '{"minAnnualRevenueUsd": 500000, "importsFromTurkey": true, "keywords": ["home textiles", "bedding", "towels"]}'::jsonb,
     '["home textiles"]'::jsonb, '["North America", "Western Europe"]'::jsonb,
     'Anatolia Textiles Export', 'sales@anatolia-textiles.example', '+15550100001',
     '{"warmReplyWhatsapp": true, "dailyDigestEmail": true}'::jsonb),
    ('00000000-0000-0000-0000-0000000000c2', '00000000-0000-0000-0000-000000000002',
     '{"minAnnualRevenueUsd": 250000, "keywords": ["ceramics", "tableware", "dinnerware"]}'::jsonb,
     '["ceramics & tableware", "furniture"]'::jsonb, '["Western Europe", "GCC"]'::jsonb,
     'Demo Ceramics Co', 'sales@demo-ceramics.example', '+15550100002',
     '{"warmReplyWhatsapp": true, "dailyDigestEmail": false}'::jsonb);

-- =========================================================
-- global_suppliers (SHARED POOL — no tenant_id)
-- =========================================================
INSERT INTO global_suppliers (id, company_name, domain, website_url, country, city, sector, description, source, last_scraped_at) VALUES
    ('00000000-0000-0000-0000-000000001001', 'Meadowbrook Home Co', 'meadowbrookhome.com', 'https://meadowbrookhome.com', 'United States', 'Charlotte', 'home textiles', 'US home goods retailer sourcing bedding and towels.', 'GOOGLE_MAPS', now() - interval '12 days'),
    ('00000000-0000-0000-0000-000000001002', 'Nordic Linen House', 'nordiclinenhouse.de', 'https://nordiclinenhouse.de', 'Germany', 'Hamburg', 'home textiles', 'German specialty linens importer.', 'B2B_DIRECTORY', now() - interval '10 days'),
    ('00000000-0000-0000-0000-000000001003', 'Bristol Textile Traders', 'bristoltextiletraders.co.uk', 'https://bristoltextiletraders.co.uk', 'United Kingdom', 'Bristol', 'home textiles', 'UK wholesale textile trading company.', 'GOOGLE_MAPS', now() - interval '9 days'),
    ('00000000-0000-0000-0000-000000001004', 'Provence Maison', 'provencemaison.fr', 'https://provencemaison.fr', 'France', 'Marseille', 'home textiles', 'French home decor and linens boutique chain.', 'TRADE_FAIR_UPLOAD', now() - interval '20 days'),
    ('00000000-0000-0000-0000-000000001005', 'Maple & Co Home Goods', 'mapleandcohome.ca', 'https://mapleandcohome.ca', 'Canada', 'Toronto', 'home textiles', 'Canadian home goods e-commerce brand.', 'GOOGLE_MAPS', now() - interval '7 days'),
    ('00000000-0000-0000-0000-000000001006', 'Gulf Living Interiors', 'gulflivinginteriors.ae', 'https://gulflivinginteriors.ae', 'United Arab Emirates', 'Dubai', 'home textiles', 'UAE interiors and soft furnishings retailer.', 'B2B_DIRECTORY', now() - interval '15 days'),
    ('00000000-0000-0000-0000-000000001007', 'Delft Table Co', 'delfttableco.nl', 'https://delfttableco.nl', 'Netherlands', 'Delft', 'ceramics & tableware', 'Dutch tableware importer and distributor.', 'GOOGLE_MAPS', now() - interval '11 days'),
    ('00000000-0000-0000-0000-000000001008', 'Milano Ceramica', 'milanoceramica.it', 'https://milanoceramica.it', 'Italy', 'Milan', 'ceramics & tableware', 'Italian ceramics wholesaler.', 'B2B_DIRECTORY', now() - interval '8 days'),
    ('00000000-0000-0000-0000-000000001009', 'Barcelona Vajilla', 'barcelonavajilla.es', 'https://barcelonavajilla.es', 'Spain', 'Barcelona', 'ceramics & tableware', 'Spanish dinnerware and tableware retailer.', 'TRADE_FAIR_UPLOAD', now() - interval '18 days'),
    ('00000000-0000-0000-0000-000000001010', 'Sydney Tableware Imports', 'sydneytablewareimports.com.au', 'https://sydneytablewareimports.com.au', 'Australia', 'Sydney', 'ceramics & tableware', 'Australian tableware import business.', 'GOOGLE_MAPS', now() - interval '6 days'),
    ('00000000-0000-0000-0000-000000001011', 'Heartland Dinnerware', 'heartlanddinnerware.com', 'https://heartlanddinnerware.com', 'United States', 'Columbus', 'ceramics & tableware', 'US Midwest dinnerware distributor.', 'GOOGLE_MAPS', now() - interval '5 days'),
    ('00000000-0000-0000-0000-000000001012', 'Berlin Furnishing Group', 'berlinfurnishinggroup.de', 'https://berlinfurnishinggroup.de', 'Germany', 'Berlin', 'furniture', 'German furniture retail group.', 'B2B_DIRECTORY', now() - interval '14 days'),
    ('00000000-0000-0000-0000-000000001013', 'Riviera Furniture Imports', 'rivierafurnitureimports.fr', 'https://rivierafurnitureimports.fr', 'France', 'Nice', 'furniture', 'French coastal furniture importer.', 'TRADE_FAIR_UPLOAD', now() - interval '19 days'),
    ('00000000-0000-0000-0000-000000001014', 'Toronto Home Furniture Co', 'torontohomefurnitureco.ca', 'https://torontohomefurnitureco.ca', 'Canada', 'Toronto', 'furniture', 'Canadian home furniture retailer.', 'GOOGLE_MAPS', now() - interval '4 days'),
    ('00000000-0000-0000-0000-000000001015', 'Emirates Furniture Trading', 'emiratesfurnituretrading.ae', 'https://emiratesfurnituretrading.ae', 'United Arab Emirates', 'Abu Dhabi', 'furniture', 'UAE furniture trading company.', 'B2B_DIRECTORY', now() - interval '13 days'),
    ('00000000-0000-0000-0000-000000001016', 'London Interior Supply', 'londoninteriorsupply.co.uk', 'https://londoninteriorsupply.co.uk', 'United Kingdom', 'London', 'furniture', 'UK interior and furniture supplier, not yet contacted.', 'GOOGLE_MAPS', now() - interval '2 days');

-- =========================================================
-- global_supplier_contacts (SHARED POOL — no tenant_id)
-- =========================================================
INSERT INTO global_supplier_contacts (id, global_supplier_id, full_name, job_title, email, phone, linkedin_url, is_primary, confidence_score, source) VALUES
    ('00000000-0000-0000-0000-000000002001', '00000000-0000-0000-0000-000000001001', 'Sarah Whitfield', 'Head of Procurement', 'sarah.whitfield@meadowbrookhome.com', '+17045550101', 'https://linkedin.com/in/example-sarah-whitfield', true, 0.910, 'WEBSITE_SCRAPE'),
    ('00000000-0000-0000-0000-000000002002', '00000000-0000-0000-0000-000000001001', 'Tom Reyes', 'Buyer', 'tom.reyes@meadowbrookhome.com', NULL, NULL, false, 0.700, 'WEBSITE_SCRAPE'),
    ('00000000-0000-0000-0000-000000002003', '00000000-0000-0000-0000-000000001002', 'Lukas Wagner', 'Einkaufsleiter', 'l.wagner@nordiclinenhouse.de', '+494055501012', NULL, true, 0.880, 'WEBSITE_SCRAPE'),
    ('00000000-0000-0000-0000-000000002004', '00000000-0000-0000-0000-000000001003', 'Emma Clarke', 'Purchasing Manager', 'emma.clarke@bristoltextiletraders.co.uk', '+441175550103', NULL, true, 0.850, 'WEBSITE_SCRAPE'),
    ('00000000-0000-0000-0000-000000002005', '00000000-0000-0000-0000-000000001004', 'Camille Fabre', 'Directrice des Achats', 'camille.fabre@provencemaison.fr', NULL, 'https://linkedin.com/in/example-camille-fabre', true, 0.780, 'ENRICHMENT_API'),
    ('00000000-0000-0000-0000-000000002006', '00000000-0000-0000-0000-000000001004', 'Julien Roux', 'Category Buyer', 'julien.roux@provencemaison.fr', NULL, NULL, false, 0.650, 'WEBSITE_SCRAPE'),
    ('00000000-0000-0000-0000-000000002007', '00000000-0000-0000-0000-000000001005', 'Olivia Bennett', 'Sourcing Lead', 'olivia.bennett@mapleandcohome.ca', '+14165550105', NULL, true, 0.900, 'WEBSITE_SCRAPE'),
    ('00000000-0000-0000-0000-000000002008', '00000000-0000-0000-0000-000000001006', 'Rashid Al Farsi', 'Procurement Director', 'rashid.alfarsi@gulflivinginteriors.ae', '+97145550106', NULL, true, 0.820, 'ENRICHMENT_API'),
    ('00000000-0000-0000-0000-000000002009', '00000000-0000-0000-0000-000000001007', 'Anke de Vries', 'Inkoopmanager', 'anke.devries@delfttableco.nl', NULL, NULL, true, 0.870, 'WEBSITE_SCRAPE'),
    ('00000000-0000-0000-0000-000000002010', '00000000-0000-0000-0000-000000001008', 'Marco Bellini', 'Responsabile Acquisti', 'marco.bellini@milanoceramica.it', '+3902550108', NULL, true, 0.860, 'WEBSITE_SCRAPE'),
    ('00000000-0000-0000-0000-000000002011', '00000000-0000-0000-0000-000000001008', 'Giulia Conti', 'Buyer', 'giulia.conti@milanoceramica.it', NULL, NULL, false, 0.600, 'WEBSITE_SCRAPE'),
    ('00000000-0000-0000-0000-000000002012', '00000000-0000-0000-0000-000000001009', 'Pablo Serrano', 'Director de Compras', 'pablo.serrano@barcelonavajilla.es', '+3493550109', NULL, true, 0.790, 'ENRICHMENT_API'),
    ('00000000-0000-0000-0000-000000002013', '00000000-0000-0000-0000-000000001010', 'Grace Mitchell', 'Import Manager', 'grace.mitchell@sydneytablewareimports.com.au', '+61255501010', NULL, true, 0.830, 'WEBSITE_SCRAPE'),
    ('00000000-0000-0000-0000-000000002014', '00000000-0000-0000-0000-000000001011', 'Derek Owens', 'VP Purchasing', 'derek.owens@heartlanddinnerware.com', '+16145550111', NULL, true, 0.910, 'WEBSITE_SCRAPE'),
    ('00000000-0000-0000-0000-000000002015', '00000000-0000-0000-0000-000000001012', 'Hannah Fischer', 'Einkaufsleiterin', 'hannah.fischer@berlinfurnishinggroup.de', NULL, NULL, true, 0.800, 'WEBSITE_SCRAPE'),
    ('00000000-0000-0000-0000-000000002016', '00000000-0000-0000-0000-000000001013', 'Nicolas Girard', 'Responsable Achats', 'nicolas.girard@rivierafurnitureimports.fr', NULL, NULL, true, 0.740, 'ENRICHMENT_API'),
    ('00000000-0000-0000-0000-000000002017', '00000000-0000-0000-0000-000000001014', 'Ashley Novak', 'Sourcing Manager', 'ashley.novak@torontohomefurnitureco.ca', '+14165550114', NULL, true, 0.870, 'WEBSITE_SCRAPE'),
    ('00000000-0000-0000-0000-000000002018', '00000000-0000-0000-0000-000000001015', 'Youssef Haddad', 'Procurement Manager', 'youssef.haddad@emiratesfurnituretrading.ae', '+97125550115', NULL, true, 0.760, 'WEBSITE_SCRAPE');

-- =========================================================
-- tenant_campaigns
-- =========================================================
INSERT INTO tenant_campaigns (id, tenant_id, name, description, status, buyer_criteria_snapshot) VALUES
    ('00000000-0000-0000-0000-000000003001', '00000000-0000-0000-0000-000000000001', 'Q3 2026 Home Textiles Outreach - North America', 'Cold outreach to US/Canada home goods buyers.', 'ACTIVE', '{"minAnnualRevenueUsd": 500000, "targetRegions": ["North America"]}'::jsonb),
    ('00000000-0000-0000-0000-000000003002', '00000000-0000-0000-0000-000000000001', 'Q3 2026 Home Textiles Outreach - Western Europe', 'Cold outreach to EU home goods buyers.', 'DRAFT', '{"minAnnualRevenueUsd": 500000, "targetRegions": ["Western Europe"]}'::jsonb),
    ('00000000-0000-0000-0000-000000003003', '00000000-0000-0000-0000-000000000002', 'Ceramics Export Push - EU', 'Cold outreach to EU tableware importers.', 'ACTIVE', '{"minAnnualRevenueUsd": 250000, "targetRegions": ["Western Europe"]}'::jsonb),
    ('00000000-0000-0000-0000-000000003004', '00000000-0000-0000-0000-000000000002', 'Ceramics & Furniture - GCC', 'Cold outreach to GCC buyers, ceramics and furniture.', 'PAUSED', '{"minAnnualRevenueUsd": 250000, "targetRegions": ["GCC"]}'::jsonb);

-- =========================================================
-- tenant_leads
-- Note the overlap: global_suppliers 1005-1010 are each independently
-- referenced by BOTH tenants below, with their own status per tenant --
-- this is the master-pool relationship in action.
-- =========================================================
INSERT INTO tenant_leads (id, tenant_id, global_supplier_id, tenant_campaign_id, status, qualification_score, qualification_notes, ai_match_metadata) VALUES
    ('00000000-0000-0000-0000-000000004001', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000001001', '00000000-0000-0000-0000-000000003001', 'EMAIL_SENT', 88.50, 'Strong match on revenue and sourcing keywords.', '{"matchedKeywords": ["home textiles", "bedding"]}'::jsonb),
    ('00000000-0000-0000-0000-000000004002', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000001002', '00000000-0000-0000-0000-000000003002', 'INTERESTED', 91.00, 'Replied positively, wants a catalog.', '{"matchedKeywords": ["home textiles", "linens"]}'::jsonb),
    ('00000000-0000-0000-0000-000000004003', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000001003', '00000000-0000-0000-0000-000000003002', 'NOT_INTERESTED', 74.00, 'Already has a supplier for this category.', '{"matchedKeywords": ["home textiles"]}'::jsonb),
    ('00000000-0000-0000-0000-000000004004', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000001004', '00000000-0000-0000-0000-000000003002', 'PENDING_APPROVAL', 69.50, 'Awaiting tenant review before first contact.', '{"matchedKeywords": ["home textiles", "decor"]}'::jsonb),
    ('00000000-0000-0000-0000-000000004005', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000001005', '00000000-0000-0000-0000-000000003001', 'APPROVED', 82.00, 'Approved, queued for first outreach email.', '{"matchedKeywords": ["home textiles"]}'::jsonb),
    ('00000000-0000-0000-0000-000000004006', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000001006', NULL, 'REJECTED', 41.00, 'Below minimum revenue threshold.', '{"matchedKeywords": []}'::jsonb),
    ('00000000-0000-0000-0000-000000004007', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000001007', NULL, 'EMAIL_SENT', 65.00, 'Opportunistic cross-sector outreach.', '{"matchedKeywords": ["home decor"]}'::jsonb),
    ('00000000-0000-0000-0000-000000004008', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000001008', NULL, 'NO_RESPONSE', 60.00, 'No reply after 2 follow-ups.', '{"matchedKeywords": []}'::jsonb),
    ('00000000-0000-0000-0000-000000004009', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000001009', NULL, 'BOUNCED', 55.00, 'Email address invalid, needs re-enrichment.', '{"matchedKeywords": []}'::jsonb),
    ('00000000-0000-0000-0000-000000004010', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000001010', '00000000-0000-0000-0000-000000003001', 'CONVERTED', 95.00, 'Signed first purchase order.', '{"matchedKeywords": ["home textiles", "import"]}'::jsonb),
    ('00000000-0000-0000-0000-000000004011', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000001005', NULL, 'PENDING_APPROVAL', 58.00, 'Opportunistic, outside core sector.', '{"matchedKeywords": []}'::jsonb),
    ('00000000-0000-0000-0000-000000004012', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000001006', '00000000-0000-0000-0000-000000003004', 'APPROVED', 76.00, 'Approved for GCC campaign.', '{"matchedKeywords": ["interiors"]}'::jsonb),
    ('00000000-0000-0000-0000-000000004013', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000001007', '00000000-0000-0000-0000-000000003003', 'EMAIL_SENT', 89.00, 'Strong tableware match.', '{"matchedKeywords": ["ceramics", "tableware"]}'::jsonb),
    ('00000000-0000-0000-0000-000000004014', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000001008', '00000000-0000-0000-0000-000000003003', 'INTERESTED', 93.00, 'Replied same day, requested pricing.', '{"matchedKeywords": ["ceramics"]}'::jsonb),
    ('00000000-0000-0000-0000-000000004015', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000001009', '00000000-0000-0000-0000-000000003003', 'NOT_INTERESTED', 71.00, 'Currently under contract elsewhere.', '{"matchedKeywords": ["tableware"]}'::jsonb),
    ('00000000-0000-0000-0000-000000004016', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000001010', NULL, 'NO_RESPONSE', 63.00, 'No reply after initial email.', '{"matchedKeywords": ["tableware"]}'::jsonb),
    ('00000000-0000-0000-0000-000000004017', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000001011', '00000000-0000-0000-0000-000000003003', 'PENDING_APPROVAL', 80.00, 'High revenue match, awaiting approval.', '{"matchedKeywords": ["dinnerware"]}'::jsonb),
    ('00000000-0000-0000-0000-000000004018', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000001012', '00000000-0000-0000-0000-000000003004', 'EMAIL_SENT', 68.00, 'Furniture cross-sell attempt.', '{"matchedKeywords": ["furniture"]}'::jsonb),
    ('00000000-0000-0000-0000-000000004019', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000001013', NULL, 'REJECTED', 38.00, 'Below minimum revenue threshold.', '{"matchedKeywords": []}'::jsonb),
    ('00000000-0000-0000-0000-000000004020', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000001014', '00000000-0000-0000-0000-000000003004', 'CONVERTED', 90.00, 'Signed first purchase order.', '{"matchedKeywords": ["furniture", "import"]}'::jsonb);

-- =========================================================
-- outreach_emails
-- =========================================================
INSERT INTO outreach_emails (id, tenant_id, tenant_lead_id, to_email, subject, body, status, provider_message_id, sent_at) VALUES
    ('00000000-0000-0000-0000-000000005001', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000004001', 'sarah.whitfield@meadowbrookhome.com', 'Turkish home textile manufacturer for Meadowbrook Home Co', 'Hi Sarah, noticed Meadowbrook Home Co recently expanded your bedding line...', 'SENT', 'mg-mock-0001', now() - interval '6 days'),
    ('00000000-0000-0000-0000-000000005002', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000004002', 'l.wagner@nordiclinenhouse.de', 'Direct-from-factory linens for Nordic Linen House', 'Hi Lukas, saw your recent trade fair presence in Frankfurt...', 'SENT', 'mg-mock-0002', now() - interval '5 days'),
    ('00000000-0000-0000-0000-000000005003', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000004003', 'emma.clarke@bristoltextiletraders.co.uk', 'Turkish home textile manufacturer for Bristol Textile Traders', 'Hi Emma, we specialize in home textiles manufactured in Turkey...', 'SENT', 'mg-mock-0003', now() - interval '5 days'),
    ('00000000-0000-0000-0000-000000005004', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000004007', 'anke.devries@delfttableco.nl', 'Home decor sourcing from Turkey for Delft Table Co', 'Hi Anke, beyond tableware we also produce complementary home textiles...', 'SENT', 'mg-mock-0004', now() - interval '4 days'),
    ('00000000-0000-0000-0000-000000005005', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000004009', 'pablo.serrano@barcelonavajilla.es', 'Turkish home textile manufacturer for Barcelona Vajilla', 'Hi Pablo, wanted to introduce our factory...', 'BOUNCED', NULL, now() - interval '4 days'),
    ('00000000-0000-0000-0000-000000005006', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000004010', 'grace.mitchell@sydneytablewareimports.com.au', 'Turkish home textile manufacturer for Sydney Tableware Imports', 'Hi Grace, following up after our call last week...', 'SENT', 'mg-mock-0006', now() - interval '9 days'),
    ('00000000-0000-0000-0000-000000005007', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000004013', 'anke.devries@delfttableco.nl', 'Ceramics manufacturer partnership with Delft Table Co', 'Hi Anke, we produce ceramics and tableware for export...', 'SENT', 'mg-mock-0007', now() - interval '3 days'),
    ('00000000-0000-0000-0000-000000005008', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000004014', 'marco.bellini@milanoceramica.it', 'Ceramics manufacturer partnership with Milano Ceramica', 'Hi Marco, noticed your growing tableware catalog...', 'SENT', 'mg-mock-0008', now() - interval '3 days'),
    ('00000000-0000-0000-0000-000000005009', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000004015', 'pablo.serrano@barcelonavajilla.es', 'Ceramics manufacturer partnership with Barcelona Vajilla', 'Hi Pablo, wanted to introduce our ceramics factory...', 'SENT', 'mg-mock-0009', now() - interval '2 days'),
    ('00000000-0000-0000-0000-000000005010', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000004018', 'hannah.fischer@berlinfurnishinggroup.de', 'Ceramics and furniture sourcing for Berlin Furnishing Group', 'Hi Hannah, alongside ceramics we also manufacture furniture...', 'SENT', 'mg-mock-0010', now() - interval '2 days'),
    ('00000000-0000-0000-0000-000000005011', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000004020', 'ashley.novak@torontohomefurnitureco.ca', 'Furniture manufacturer partnership with Toronto Home Furniture Co', 'Hi Ashley, following up after your team confirmed interest...', 'SENT', 'mg-mock-0011', now() - interval '10 days');

-- =========================================================
-- email_responses
-- =========================================================
INSERT INTO email_responses (id, tenant_id, outreach_email_id, from_email, subject, body, classified_intent, classification_metadata, received_at) VALUES
    ('00000000-0000-0000-0000-000000006001', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000005002', 'l.wagner@nordiclinenhouse.de', 'RE: Direct-from-factory linens for Nordic Linen House', 'This looks interesting, please send a full catalog and MOQs.', 'INTERESTED', '{"confidence": 0.94, "model": "mock"}'::jsonb, now() - interval '4 days'),
    ('00000000-0000-0000-0000-000000006002', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000005003', 'emma.clarke@bristoltextiletraders.co.uk', 'RE: Turkish home textile manufacturer for Bristol Textile Traders', 'Thanks but we already have a long-term supplier for this category.', 'NOT_INTERESTED', '{"confidence": 0.89, "model": "mock"}'::jsonb, now() - interval '4 days'),
    ('00000000-0000-0000-0000-000000006003', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000005001', 'sarah.whitfield@meadowbrookhome.com', 'RE: Turkish home textile manufacturer for Meadowbrook Home Co', 'Can you send more detail on certifications and lead times first?', 'NEEDS_INFO', '{"confidence": 0.81, "model": "mock"}'::jsonb, now() - interval '5 days'),
    ('00000000-0000-0000-0000-000000006004', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000005004', 'anke.devries@delfttableco.nl', 'Out of Office', 'I am out of the office until next month.', 'OUT_OF_OFFICE', '{"confidence": 0.99, "model": "mock"}'::jsonb, now() - interval '3 days'),
    ('00000000-0000-0000-0000-000000006005', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000005008', 'marco.bellini@milanoceramica.it', 'RE: Ceramics manufacturer partnership with Milano Ceramica', 'Very interested, can we schedule a call this week?', 'INTERESTED', '{"confidence": 0.96, "model": "mock"}'::jsonb, now() - interval '2 days'),
    ('00000000-0000-0000-0000-000000006006', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000005009', 'pablo.serrano@barcelonavajilla.es', 'RE: Ceramics manufacturer partnership with Barcelona Vajilla', 'Not a fit for us right now, please remove us from this list.', 'UNSUBSCRIBE', '{"confidence": 0.92, "model": "mock"}'::jsonb, now() - interval '1 days');

-- =========================================================
-- notifications
-- =========================================================
INSERT INTO notifications (id, tenant_id, tenant_user_id, type, channel, title, message, related_entity_type, related_entity_id, is_read, sent_at) VALUES
    ('00000000-0000-0000-0000-000000007001', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-0000000000a1', 'WARM_REPLY', 'WHATSAPP', 'Warm reply: Nordic Linen House', 'Nordic Linen House replied and wants a catalog + MOQs.', 'email_response', '00000000-0000-0000-0000-000000006001', false, now() - interval '4 days'),
    ('00000000-0000-0000-0000-000000007002', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-0000000000a1', 'NEW_LEAD', 'DASHBOARD', 'New lead pending approval', 'Provence Maison matched your buyer criteria and is awaiting approval.', 'tenant_lead', '00000000-0000-0000-0000-000000004004', true, now() - interval '13 days'),
    ('00000000-0000-0000-0000-000000007003', '00000000-0000-0000-0000-000000000001', NULL, 'SCRAPING_JOB_DONE', 'DASHBOARD', 'Scraping job completed', 'Google Maps scrape for North America home textiles finished: 10 companies found.', 'scraping_job', '00000000-0000-0000-0000-000000008001', true, now() - interval '13 days'),
    ('00000000-0000-0000-0000-000000007004', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-0000000000a2', 'BOUNCE_ALERT', 'EMAIL', 'Email bounced', 'Outreach email to Barcelona Vajilla bounced - contact needs re-enrichment.', 'outreach_email', '00000000-0000-0000-0000-000000005005', false, now() - interval '4 days'),
    ('00000000-0000-0000-0000-000000007005', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-0000000000b1', 'WARM_REPLY', 'WHATSAPP', 'Warm reply: Milano Ceramica', 'Milano Ceramica is interested and wants to schedule a call.', 'email_response', '00000000-0000-0000-0000-000000006005', false, now() - interval '2 days'),
    ('00000000-0000-0000-0000-000000007006', '00000000-0000-0000-0000-000000000002', NULL, 'SCRAPING_JOB_DONE', 'DASHBOARD', 'Scraping job completed', 'B2B directory scrape for EU ceramics buyers finished: 6 companies found.', 'scraping_job', '00000000-0000-0000-0000-000000008003', true, now() - interval '9 days');

-- =========================================================
-- scraping_jobs
-- =========================================================
INSERT INTO scraping_jobs (id, tenant_id, tenant_campaign_id, source, status, params, result_summary, companies_found, error_message, started_at, completed_at) VALUES
    ('00000000-0000-0000-0000-000000008001', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000003001', 'GOOGLE_MAPS', 'COMPLETED', '{"query": "home textiles importer", "regions": ["United States", "Canada"]}'::jsonb, '{"companiesFound": 10, "duplicatesSkipped": 2}'::jsonb, 10, NULL, now() - interval '13 days', now() - interval '13 days' + interval '25 minutes'),
    ('00000000-0000-0000-0000-000000008002', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000003002', 'B2B_DIRECTORY', 'RUNNING', '{"query": "home decor wholesaler", "regions": ["Western Europe"]}'::jsonb, '{}'::jsonb, 0, NULL, now() - interval '20 minutes', NULL),
    ('00000000-0000-0000-0000-000000008003', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000003003', 'B2B_DIRECTORY', 'COMPLETED', '{"query": "ceramics tableware importer", "regions": ["Western Europe"]}'::jsonb, '{"companiesFound": 6, "duplicatesSkipped": 1}'::jsonb, 6, NULL, now() - interval '9 days', now() - interval '9 days' + interval '18 minutes'),
    ('00000000-0000-0000-0000-000000008004', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000003004', 'TRADE_FAIR_UPLOAD', 'FAILED', '{"fileName": "gcc-trade-fair-2026.xlsx"}'::jsonb, '{}'::jsonb, 0, 'Row 42: unparseable company name column, job aborted.', now() - interval '7 days', now() - interval '7 days' + interval '2 minutes');
