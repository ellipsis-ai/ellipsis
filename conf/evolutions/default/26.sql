# --- !Ups

CREATE TABLE invocation_log_entries (
  id TEXT PRIMARY KEY,
  behavior_version_id TEXT NOT NULL REFERENCES behavior_versions(id) ON DELETE CASCADE,
  result_type TEXT NOT NULL,
  result_text TEXT NOT NULL,
  context TEXT NOT NULL,
  user_id_for_context TEXT,
  runtime_in_milliseconds INT,
  created_at TIMESTAMP NOT NULL
);

# --- !Downs

DROP TABLE IF EXISTS invocation_log_entries;
