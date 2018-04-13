# --- !Ups

BEGIN;

CREATE TABLE parent_conversations (
  id TEXT PRIMARY KEY,
  parent_id TEXT NOT NULL REFERENCES conversations(id),
  param_id TEXT NOT NULL REFERENCES behavior_parameters(id)
);

ALTER TABLE conversations ADD COLUMN parent_id TEXT REFERENCES parent_conversations(id);

COMMIT;

# --- !Downs

BEGIN;

ALTER TABLE conversations DROP COLUMN parent_id;

DROP TABLE IF EXISTS parent_conversations;

COMMIT;
