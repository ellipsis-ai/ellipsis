# --- !Ups

CREATE TABLE behavior_outputs (
  behavior_version_id TEXT PRIMARY KEY REFERENCES behavior_versions(id) ON DELETE CASCADE,
  type TEXT NOT NULL,
  channel_name TEXT
);

# --- !Downs

DROP TABLE IF EXISTS behavior_outputs;
