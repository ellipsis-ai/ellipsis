# --- !Ups

ALTER TABLE slack_bot_profiles ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

# --- !Downs

ALTER TABLE slack_bot_profiles DROP COLUMN created_at;
