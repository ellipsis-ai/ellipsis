# --- !Ups

BEGIN;

ALTER TABLE invocation_log_entries ADD COLUMN channel TEXT;

CREATE INDEX invocation_log_entries_channel_index ON invocation_log_entries(channel);

COMMIT;

# --- !Downs

BEGIN;

DROP INDEX IF EXISTS invocation_log_entries_channel_index;

ALTER TABLE invocation_log_entries DROP COLUMN channel;

COMMIT;
