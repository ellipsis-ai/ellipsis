# --- !Ups

CREATE TABLE forms (
  id TEXT PRIMARY KEY,
  config JSONB NOT NULL,
  created_from_behavior_group_version_id TEXT NOT NULL REFERENCES behavior_group_versions(id),
  created_at TIMESTAMPTZ NOT NULL
);

# --- !Downs

DROP TABLE IF EXISTS forms;
