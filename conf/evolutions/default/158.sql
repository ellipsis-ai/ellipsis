# --- !Ups

BEGIN;

ALTER TABLE message_listeners ADD COLUMN last_queried_for_copilot_at TIMESTAMPTZ;

CREATE INDEX message_listeners_last_queried_for_copilot_at_index ON message_listeners(last_queried_for_copilot_at);

COMMIT;

# --- !Downs

BEGIN;

DROP INDEX IF EXISTS message_listeners_last_queried_for_copilot_at_index;

ALTER TABLE message_listeners DROP COLUMN last_queried_for_copilot_at;

COMMIT;
