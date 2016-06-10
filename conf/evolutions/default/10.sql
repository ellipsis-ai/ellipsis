# --- !Ups

ALTER TABLE message_triggers ADD COLUMN requires_bot_mention BOOLEAN NOT NULL DEFAULT FALSE;

# --- !Downs

ALTER TABLE message_triggers DROP COLUMN requires_bot_mention;
