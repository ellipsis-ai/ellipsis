# --- !Ups

BEGIN;

CREATE TABLE behavior_version_user_involvements (
  id TEXT PRIMARY KEY,
  behavior_version_id TEXT NOT NULL REFERENCES behavior_versions(id),
  user_id TEXT NOT NULL REFERENCES users(id),
  created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX behavior_version_user_involvements_behavior_version_id_index ON behavior_version_user_involvements(behavior_version_id);
CREATE INDEX behavior_version_user_involvements_user_id_index ON behavior_version_user_involvements(user_id);
CREATE INDEX behavior_version_user_involvements_when_index ON behavior_version_user_involvements(created_at);

COMMIT;

# --- !Downs

BEGIN;

DROP INDEX IF EXISTS behavior_version_user_involvements_behavior_version_id_index;
DROP INDEX IF EXISTS behavior_version_user_involvements_user_id_index;
DROP INDEX IF EXISTS behavior_version_user_involvements_when_index;

DROP TABLE IF EXISTS behavior_version_user_involvements;

COMMIT;
