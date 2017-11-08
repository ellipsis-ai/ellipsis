# --- !Ups

ALTER TABLE invocation_log_entries ADD COLUMN original_event_type TEXT;

# --- !Downs

ALTER TABLE invocation_log_entries DROP COLUMN original_event_type;
