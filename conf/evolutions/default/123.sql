# --- !Ups

BEGIN;

ALTER TABLE linked_accounts
DROP CONSTRAINT IF EXISTS linked_accounts_provider_id_provider_key_key;

ALTER TABLE conversations ADD COLUMN team_id_for_context TEXT;

COMMIT;

# --- !Downs

ALTER TABLE conversations DROP COLUMN team_id_for_context;
