# --- !Ups

ALTER TABLE invocation_log_entries ADD COLUMN event_data JSONB;

# --- !Downs

ALTER TABLE invocation_log_entries DROP COLUMN event_data;
