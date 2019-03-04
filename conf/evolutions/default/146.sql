# --- !Ups

BEGIN;

ALTER TABLE invocation_log_entries ADD COLUMN event_type TEXT;

CREATE INDEX invocation_log_entries_event_type_index ON invocation_log_entries(event_type);

COMMIT;

# --- !Downs

BEGIN;

DROP INDEX IF EXISTS invocation_log_entries_event_type_index;

ALTER TABLE invocation_log_entries DROP COLUMN event_type;

COMMIT;
