# --- !Ups

BEGIN;

ALTER TABLE behavior_versions ADD COLUMN is_test BOOL NOT NULL DEFAULT FALSE;

CREATE TABLE behavior_test_results (
  id TEXT PRIMARY KEY,
  behavior_version_id TEXT NOT NULL REFERENCES behavior_versions(id),
  is_pass BOOL NOT NULL,
  output TEXT NOT NULL,
  run_at TIMESTAMPTZ NOT NULL
);

COMMIT;

# --- !Downs

BEGIN;

DROP TABLE IF EXISTS behavior_test_results;

ALTER TABLE behavior_versions DROP COLUMN is_test;

COMMIT;
