# --- !Ups

BEGIN;

ALTER TABLE linked_accounts
DROP CONSTRAINT IF EXISTS linked_accounts_provider_id_provider_key_key;

DROP TABLE IF EXISTS slack_profiles;

ALTER TABLE conversations ADD COLUMN team_id_for_context TEXT;

COMMIT;

# --- !Downs

BEGIN;

CREATE TABLE slack_profiles (
  provider_id TEXT NOT NULL,
  provider_key TEXT NOT NULL,
  team_id TEXT NOT NULL,
  PRIMARY KEY(provider_id, provider_key)
);

ALTER TABLE conversations DROP COLUMN team_id_for_context;

COMMIT;
