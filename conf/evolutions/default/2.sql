# --- !Ups

ALTER TABLE behaviors ALTER COLUMN description DROP NOT NULL;
ALTER TABLE behaviors ADD COLUMN short_name TEXT;
ALTER TABLE behaviors ADD COLUMN has_code BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE behavior_parameters (
  id TEXT PRIMARY KEY,
  behavior_id TEXT NOT NULL REFERENCES behaviors(id),
  name TEXT NOT NULL,
  question TEXT,
  param_type TEXT
);

CREATE TABLE template_message_triggers (
  id TEXT PRIMARY KEY,
  behavior_id TEXT NOT NULL REFERENCES behaviors(id),
  template TEXT NOT NULL
);

CREATE TABLE conversations (
  id TEXT PRIMARY KEY,
  behavior_id TEXT NOT NULL REFERENCES behaviors(id),
  conversation_type TEXT NOT NULL, -- learning_behavior, editing_behavior, invoking_behavior
  context TEXT NOT NULL, -- Slack, etc
  user_id_for_context TEXT NOT NULL, -- Slack user id, etc
  started_at TIMESTAMP NOT NULL,
  state TEXT NOT NULL
);

# --- !Downs

DROP TABLE IF EXISTS learn_behavior_conversations;
DROP TABLE IF EXISTS template_message_triggers;
DROP TABLE IF EXISTS behavior_parameters;

ALTER TABLE behaviors DROP COLUMN has_code;
ALTER TABLE behaviors DROP COLUMN short_name;
ALTER TABLE behaviors ALTER COLUMN description SET NOT NULL;
