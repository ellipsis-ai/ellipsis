# --- !Ups

ALTER TABLE invocation_tokens DROP COLUMN is_used;

# --- !Downs

ALTER TABLE invocation_tokens ADD COLUMN is_used BOOL NOT NULL DEFAULT FALSE;
