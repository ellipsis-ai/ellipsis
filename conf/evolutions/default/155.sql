# --- !Ups

BEGIN;

ALTER TABLE message_listeners ADD COLUMN is_for_copilot BOOL NOT NULL DEFAULT false;

CREATE INDEX message_listeners_is_for_copilot_index ON message_listeners(is_for_copilot);

COMMIT;

# --- !Downs

BEGIN;

DROP INDEX IF EXISTS message_listeners_is_for_copilot_index;

ALTER TABLE message_listeners DROP COLUMN is_for_copilot;

COMMIT;
