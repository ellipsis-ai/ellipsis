# --- !Ups

BEGIN;

CREATE TABLE message_listeners (
  id TEXT PRIMARY KEY,
  behavior_id TEXT NOT NULL REFERENCES behaviors(id),
  message_input_id TEXT NOT NULL,
  arguments JSONB NOT NULL,
  medium TEXT NOT NULL,
  channel TEXT NOT NULL,
  thread TEXT,
  user_id TEXT NOT NULL REFERENCES users(id),
  created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX message_listeners_medium_index ON message_listeners(medium);
CREATE INDEX message_listeners_channel_index ON message_listeners(channel);
CREATE INDEX message_listeners_thread_index ON message_listeners(thread);
CREATE INDEX message_listeners_user_id_index ON message_listeners(user_id);

COMMIT;

# --- !Downs

BEGIN;

DROP INDEX IF EXISTS message_listeners_medium_index;
DROP INDEX IF EXISTS message_listeners_channel_index;
DROP INDEX IF EXISTS message_listeners_thread_index;
DROP INDEX IF EXISTS message_listeners_user_id_index;

DROP TABLE IF EXISTS message_listeners;

COMMIT;
