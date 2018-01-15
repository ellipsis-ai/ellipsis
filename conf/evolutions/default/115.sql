# --- !Ups

CREATE TABLE billing_accounts (
  id TEXT PRIMARY KEY,
  chargebee_id TEXT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE billing_subscriptions (
  chargebee_plan_id TEXT NOT NULL,
  billing_account_id TEXT NOT NULL REFERENCES billing_accounts(id) ON DELETE CASCADE,
  team_id TEXT NOT NULL REFERENCES teams(id),
  seats_count INTEGER,
  status TEXT,
  status_updated_at TIMESTAMP,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

# --- !Downs

DROP TABLE IF EXISTS billing_accounts;
DROP TABLE IF EXISTS billing_subscriptions;

