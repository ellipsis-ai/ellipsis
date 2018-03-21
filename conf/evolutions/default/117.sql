  # --- !Ups

CREATE TABLE organizations (
  id TEXT PRIMARY KEY,
  chargebee_customer_id TEXT,
  name TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL
);

ALTER TABLE teams ADD COLUMN organization_id TEXT;

# --- !Downs

DROP TABLE IF EXISTS organizations;
ALTER TABLE teams DROP COLUMN IF EXISTS organization_id;
