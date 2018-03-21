# --- !Ups

ALTER TABLE api_tokens ADD COLUMN expiry_seconds INT;
ALTER TABLE api_tokens ADD COLUMN is_one_time BOOLEAN NOT NULL DEFAULT FALSE;

# --- !Downs

ALTER TABLE api_tokens DROP COLUMN expiry_seconds;
ALTER TABLE api_tokens DROP COLUMN is_one_time;
