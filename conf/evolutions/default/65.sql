# --- !Ups

TRUNCATE TABLE invocation_tokens;
ALTER TABLE invocation_tokens ADD COLUMN behavior_id TEXT NOT NULL REFERENCES behaviors(id);

# --- !Downs

TRUNCATE TABLE invocation_tokens;
ALTER TABLE invocation_tokens DROP COLUMN behavior_id;
