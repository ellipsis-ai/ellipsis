# --- !Ups

CREATE TABLE aws_configs (
  id TEXT PRIMARY KEY,
  behavior_version_id TEXT NOT NULL REFERENCES behavior_versions(id) ON DELETE CASCADE,
  access_key_name TEXT,
  secret_key_name TEXT,
  region_name TEXT
);

# --- !Downs

DROP TABLE IF EXISTS aws_configs;
