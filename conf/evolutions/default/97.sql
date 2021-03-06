# --- !Ups

CREATE TABLE library_versions(
  id TEXT PRIMARY KEY,
  library_id TEXT NOT NULL,
  export_id TEXT,
  name TEXT NOT NULL,
  description TEXT,
  function_body TEXT NOT NULL,
  group_version_id TEXT NOT NULL REFERENCES behavior_group_versions(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ NOT NULL
);

# --- !Downs

DROP TABLE IF EXISTS library_versions;
