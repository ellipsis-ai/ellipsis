# --- !Ups

ALTER TABLE invocation_log_entries ADD COLUMN user_id TEXT REFERENCES users(id);

# --- !Downs

ALTER TABLE invocation_log_entries DROP COLUMN user_id;
