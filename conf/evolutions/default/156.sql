# --- !Ups

BEGIN;

ALTER TABLE invocation_log_entries ADD COLUMN message_listener_id TEXT REFERENCES message_listeners(id);

CREATE INDEX invocation_log_entries_message_listener_id_index ON invocation_log_entries(message_listener_id);

COMMIT;

# --- !Downs

BEGIN;

DROP INDEX IF EXISTS invocation_log_entries_message_listener_id_index;

ALTER TABLE invocation_log_entries DROP COLUMN message_listener_id;

COMMIT;
