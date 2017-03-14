# --- !Ups

BEGIN;

DELETE FROM invocation_log_entries WHERE user_id IS NULL;
DELETE FROM invocation_log_entries WHERE user_id_for_context = 'api';
ALTER TABLE invocation_log_entries ALTER COLUMN user_id SET NOT NULL;

COMMIT;

# --- !Downs

ALTER TABLE invocation_log_entries ALTER COLUMN user_id DROP NOT NULL;
