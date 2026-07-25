CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE tenants (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    slug            VARCHAR(100) NOT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'ACTIVE'
                        CHECK (status IN ('ACTIVE', 'SUSPENDED', 'TRIAL', 'CANCELLED')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_tenants_slug UNIQUE (slug)
);

CREATE TABLE tenant_users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    email           VARCHAR(320) NOT NULL,
    full_name       VARCHAR(255),
    role            VARCHAR(30) NOT NULL DEFAULT 'MEMBER'
                        CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER')),
    status          VARCHAR(30) NOT NULL DEFAULT 'ACTIVE'
                        CHECK (status IN ('ACTIVE', 'INVITED', 'DISABLED')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_tenant_users_tenant_email UNIQUE (tenant_id, email)
);
CREATE INDEX idx_tenant_users_tenant_id ON tenant_users(tenant_id);

CREATE TABLE tenant_settings (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    buyer_criteria          JSONB NOT NULL DEFAULT '{}'::jsonb,
    target_sectors          JSONB NOT NULL DEFAULT '[]'::jsonb,
    target_regions          JSONB NOT NULL DEFAULT '[]'::jsonb,
    email_sender_name       VARCHAR(255),
    email_sender_address    VARCHAR(320),
    whatsapp_notify_number  VARCHAR(50),
    notification_prefs      JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_tenant_settings_tenant UNIQUE (tenant_id)
);
CREATE INDEX idx_tenant_settings_tenant_id ON tenant_settings(tenant_id);

-- SHARED POOL — no tenant_id. Core IP asset, deduped by domain.
CREATE TABLE global_suppliers (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_name        VARCHAR(500) NOT NULL,
    domain              VARCHAR(500) NOT NULL,
    website_url         VARCHAR(1000),
    country             VARCHAR(100),
    city                VARCHAR(255),
    sector              VARCHAR(255),
    description         TEXT,
    source              VARCHAR(50) NOT NULL DEFAULT 'GOOGLE_MAPS'
                            CHECK (source IN ('GOOGLE_MAPS', 'B2B_DIRECTORY', 'TRADE_FAIR_UPLOAD', 'MANUAL')),
    enrichment_data      JSONB NOT NULL DEFAULT '{}'::jsonb,
    last_scraped_at      TIMESTAMPTZ,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_global_suppliers_domain UNIQUE (domain)
);
CREATE INDEX idx_global_suppliers_domain ON global_suppliers(domain);
CREATE INDEX idx_global_suppliers_sector ON global_suppliers(sector);

-- SHARED POOL — no tenant_id.
CREATE TABLE global_supplier_contacts (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    global_supplier_id  UUID NOT NULL REFERENCES global_suppliers(id) ON DELETE CASCADE,
    full_name           VARCHAR(255),
    job_title            VARCHAR(255),
    email               VARCHAR(320),
    phone               VARCHAR(50),
    linkedin_url        VARCHAR(1000),
    is_primary          BOOLEAN NOT NULL DEFAULT false,
    confidence_score     NUMERIC(4,3),
    source              VARCHAR(50) NOT NULL DEFAULT 'WEBSITE_SCRAPE'
                            CHECK (source IN ('WEBSITE_SCRAPE', 'ENRICHMENT_API', 'MANUAL')),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_global_supplier_contacts_supplier_id ON global_supplier_contacts(global_supplier_id);
CREATE UNIQUE INDEX uq_global_supplier_contacts_supplier_email
    ON global_supplier_contacts(global_supplier_id, email) WHERE email IS NOT NULL;

CREATE TABLE tenant_campaigns (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id                UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name                     VARCHAR(255) NOT NULL,
    description              TEXT,
    status                   VARCHAR(30) NOT NULL DEFAULT 'DRAFT'
                                 CHECK (status IN ('DRAFT', 'ACTIVE', 'PAUSED', 'COMPLETED', 'ARCHIVED')),
    buyer_criteria_snapshot  JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_tenant_campaigns_tenant_id ON tenant_campaigns(tenant_id);

-- Bridge table: tenant <-> global_supplier. One row per relationship, own status lifecycle.
CREATE TABLE tenant_leads (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id            UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    global_supplier_id   UUID NOT NULL REFERENCES global_suppliers(id) ON DELETE CASCADE,
    tenant_campaign_id   UUID REFERENCES tenant_campaigns(id) ON DELETE SET NULL,
    status               VARCHAR(30) NOT NULL DEFAULT 'PENDING_APPROVAL'
                             CHECK (status IN (
                                 'PENDING_APPROVAL', 'APPROVED', 'REJECTED',
                                 'EMAIL_SENT', 'NO_RESPONSE', 'INTERESTED',
                                 'NOT_INTERESTED', 'BOUNCED', 'CONVERTED'
                             )),
    qualification_score   NUMERIC(5,2),
    qualification_notes   TEXT,
    ai_match_metadata     JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_tenant_leads_tenant_supplier UNIQUE (tenant_id, global_supplier_id)
);
CREATE INDEX idx_tenant_leads_tenant_id ON tenant_leads(tenant_id);
CREATE INDEX idx_tenant_leads_global_supplier_id ON tenant_leads(global_supplier_id);
CREATE INDEX idx_tenant_leads_tenant_campaign_id ON tenant_leads(tenant_campaign_id);
CREATE INDEX idx_tenant_leads_status ON tenant_leads(status);

CREATE TABLE outreach_emails (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id             UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    tenant_lead_id        UUID NOT NULL REFERENCES tenant_leads(id) ON DELETE CASCADE,
    to_email              VARCHAR(320) NOT NULL,
    subject               VARCHAR(500) NOT NULL,
    body                  TEXT NOT NULL,
    status                VARCHAR(30) NOT NULL DEFAULT 'DRAFT'
                              CHECK (status IN ('DRAFT', 'QUEUED', 'SENT', 'FAILED', 'BOUNCED')),
    provider_message_id   VARCHAR(255),
    sent_at               TIMESTAMPTZ,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_outreach_emails_tenant_id ON outreach_emails(tenant_id);
CREATE INDEX idx_outreach_emails_tenant_lead_id ON outreach_emails(tenant_lead_id);
CREATE INDEX idx_outreach_emails_status ON outreach_emails(status);

CREATE TABLE email_responses (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id                 UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    outreach_email_id         UUID NOT NULL REFERENCES outreach_emails(id) ON DELETE CASCADE,
    from_email                VARCHAR(320),
    subject                   VARCHAR(500),
    body                      TEXT,
    classified_intent         VARCHAR(30)
                                  CHECK (classified_intent IN (
                                      'INTERESTED', 'NOT_INTERESTED', 'NEEDS_INFO',
                                      'OUT_OF_OFFICE', 'UNSUBSCRIBE', 'SPAM', 'UNKNOWN'
                                  )),
    classification_metadata   JSONB NOT NULL DEFAULT '{}'::jsonb,
    received_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_email_responses_tenant_id ON email_responses(tenant_id);
CREATE INDEX idx_email_responses_outreach_email_id ON email_responses(outreach_email_id);

CREATE TABLE notifications (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id             UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    tenant_user_id        UUID REFERENCES tenant_users(id) ON DELETE SET NULL,
    type                  VARCHAR(50) NOT NULL
                              CHECK (type IN ('WARM_REPLY', 'NEW_LEAD', 'SCRAPING_JOB_DONE', 'BOUNCE_ALERT', 'SYSTEM')),
    channel               VARCHAR(30) NOT NULL DEFAULT 'DASHBOARD'
                              CHECK (channel IN ('DASHBOARD', 'WHATSAPP', 'EMAIL')),
    title                 VARCHAR(255) NOT NULL,
    message               TEXT,
    related_entity_type   VARCHAR(50),
    related_entity_id     UUID,
    is_read               BOOLEAN NOT NULL DEFAULT false,
    sent_at               TIMESTAMPTZ,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_notifications_tenant_id ON notifications(tenant_id);
CREATE INDEX idx_notifications_tenant_user_id ON notifications(tenant_user_id);
CREATE INDEX idx_notifications_is_read ON notifications(tenant_id, is_read);

CREATE TABLE scraping_jobs (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id             UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    tenant_campaign_id    UUID REFERENCES tenant_campaigns(id) ON DELETE SET NULL,
    source                VARCHAR(50) NOT NULL
                              CHECK (source IN ('GOOGLE_MAPS', 'B2B_DIRECTORY', 'TRADE_FAIR_UPLOAD', 'MANUAL')),
    status                VARCHAR(30) NOT NULL DEFAULT 'PENDING'
                              CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED')),
    params                JSONB NOT NULL DEFAULT '{}'::jsonb,
    result_summary        JSONB NOT NULL DEFAULT '{}'::jsonb,
    companies_found       INTEGER NOT NULL DEFAULT 0,
    error_message         TEXT,
    started_at            TIMESTAMPTZ,
    completed_at          TIMESTAMPTZ,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_scraping_jobs_tenant_id ON scraping_jobs(tenant_id);
CREATE INDEX idx_scraping_jobs_tenant_campaign_id ON scraping_jobs(tenant_campaign_id);
CREATE INDEX idx_scraping_jobs_status ON scraping_jobs(status);
