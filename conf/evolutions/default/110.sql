# --- !Ups

ALTER TABLE invocation_log_entries ADD COLUMN scheduled BOOLEAN;

# --- !Downs

ALTER TABLE invocation_log_entries DROP COLUMN scheduled;
