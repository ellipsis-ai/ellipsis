# --- !Ups

CREATE TABLE behaviors (
  id TEXT PRIMARY KEY,
  team_id TEXT NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
  description TEXT NOT NULL
);

CREATE TABLE regex_message_triggers (
  id TEXT PRIMARY KEY,
  behavior_id TEXT NOT NULL REFERENCES behaviors(id) ON DELETE CASCADE,
  regex TEXT NOT NULL
);

# --- !Downs

DROP TABLE IF EXISTS regex_message_triggers;
DROP TABLE IF EXISTS behaviors;

