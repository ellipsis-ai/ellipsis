# --- !Ups

BEGIN;

ALTER TABLE message_listeners ADD COLUMN is_enabled BOOL NOT NULL DEFAULT true;

CREATE INDEX message_listeners_is_enabled_index ON message_listeners(is_enabled);

COMMIT;

# --- !Downs

BEGIN;

DROP INDEX IF EXISTS message_listeners_is_enabled_index;

ALTER TABLE message_listeners DROP COLUMN is_enabled;

COMMIT;
