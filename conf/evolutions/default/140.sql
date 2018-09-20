# --- !Ups

ALTER TABLE message_triggers ADD COLUMN trigger_type TEXT NOT NULL DEFAULT 'MessageSent';

# --- !Downs

ALTER TABLE message_triggers DROP COLUMN trigger_type;
