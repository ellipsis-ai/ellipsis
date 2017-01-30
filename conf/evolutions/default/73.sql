# --- !Ups

ALTER TABLE conversations ADD COLUMN trigger_message TEXT NOT NULL DEFAULT '';

# --- !Downs

ALTER TABLE conversations DROP COLUMN trigger_message;

