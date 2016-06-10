# --- !Ups

CREATE TABLE message_triggers (
  id TEXT PRIMARY KEY,
  behavior_id TEXT NOT NULL REFERENCES behaviors(id) ON DELETE CASCADE,
  pattern TEXT NOT NULL,
  treat_as_regex BOOLEAN NOT NULL
);

INSERT INTO message_triggers (
  SELECT id, behavior_id, regex, TRUE
  FROM regex_message_triggers
);

INSERT INTO message_triggers
(
  SELECT id, behavior_id, template, FALSE
  FROM template_message_triggers
);

# --- !Downs

DROP TABLE IF EXISTS message_triggers;
