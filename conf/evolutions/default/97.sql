# --- !Ups

CREATE TABLE library_versions(
  id TEXT PRIMARY KEY,
  library_id TEXT NOT NULL,
  name TEXT NOT NULL,
  code TEXT NOT NULL,
  group_version_id TEXT NOT NULL REFERENCES behavior_group_versions(id),
  created_at TIMESTAMPTZ NOT NULL
);

# --- !Downs

DROP TABLE IF EXISTS library_versions;
