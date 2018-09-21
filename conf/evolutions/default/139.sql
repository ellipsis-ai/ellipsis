# --- !Ups

ALTER TABLE message_listeners DROP COLUMN message_input_id;

# --- !Downs

BEGIN;

DELETE FROM message_listeners;
ALTER TABLE message_listeners ADD COLUMN message_input_id TEXT NOT NULL;

COMMIT;
