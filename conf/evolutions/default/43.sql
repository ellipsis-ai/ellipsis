# --- !Ups

ALTER TABLE invocation_log_entries ADD COLUMN message_text TEXT NOT NULL DEFAULT '<not captured>';

# --- !Downs

ALTER TABLE invocation_log_entries DROP COLUMN message_text;
