# --- !Ups

CREATE TABLE behavior_test_results (
  id TEXT PRIMARY KEY,
  behavior_version_id TEXT NOT NULL REFERENCES behavior_versions(id),
  is_pass BOOL NOT NULL,
  output TEXT NOT NULL,
  run_at TIMESTAMPTZ NOT NULL
);

# --- !Downs

DROP TABLE IF EXISTS behavior_test_results;
