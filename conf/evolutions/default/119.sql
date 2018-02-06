# --- !Ups

BEGIN;

CREATE TABLE behavior_group_version_shas (
  group_version_id TEXT NOT NULL REFERENCES behavior_group_versions(id) PRIMARY KEY,
  git_sha TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL
);

INSERT INTO behavior_group_version_shas (group_version_id, git_sha, created_at)
SELECT id, git_sha, created_at FROM behavior_group_versions WHERE git_sha IS NOT NULL;

COMMIT;


# --- !Downs

DROP TABLE IF EXISTS behavior_group_version_shas;
