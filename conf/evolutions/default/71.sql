# --- !Ups

ALTER TABLE login_tokens DROP COLUMN is_used;

# --- !Downs

ALTER TABLE login_tokens ADD COLUMN is_used BOOL NOT NULL DEFAULT FALSE;
