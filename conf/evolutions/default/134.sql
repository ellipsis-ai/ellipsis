# --- !Ups

ALTER TABLE slack_bot_profiles ADD COLUMN allow_shortcut_mention BOOLEAN NOT NULL DEFAULT TRUE;

# --- !Downs

ALTER TABLE slack_bot_profiles DROP COLUMN allow_shortcut_mention;
