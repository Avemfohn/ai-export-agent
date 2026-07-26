-- Records why a send attempt FAILED (Mailgun rejection, network error, etc).
-- Nullable, no default -- only ever populated when status transitions to
-- FAILED. Mirrors the existing scraping_jobs.error_message pattern.

ALTER TABLE outreach_emails ADD COLUMN error_message TEXT;
