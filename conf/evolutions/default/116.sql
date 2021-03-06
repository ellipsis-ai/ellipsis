# --- !Ups

BEGIN;

CREATE TABLE behavior_group_deployments (
  id TEXT PRIMARY KEY,
  group_id TEXT NOT NULL REFERENCES behavior_groups(id) ON DELETE CASCADE,
  group_version_id TEXT NOT NULL REFERENCES behavior_group_versions(id) ON DELETE CASCADE,
  comment TEXT,
  user_id TEXT NOT NULL REFERENCES users(id),
  created_at TIMESTAMPTZ NOT NULL,
  UNIQUE (group_version_id)
);

CREATE INDEX behavior_group_deployments_group_id_index ON behavior_group_deployments(group_id);
CREATE INDEX behavior_group_deployments_group_version_id_index ON behavior_group_deployments(group_version_id);
CREATE INDEX behavior_group_deployments_user_id_index ON behavior_group_deployments(user_id);

INSERT INTO behavior_group_deployments
SELECT DISTINCT ON (group_id) generate_pseudorandom_id(), group_id, id, NULL, author_id, created_at
FROM behavior_group_versions
ORDER BY group_id, created_at DESC;

COMMIT;

# --- !Downs

BEGIN;

DROP INDEX IF EXISTS behavior_group_deployments_group_id_index;
DROP INDEX IF EXISTS behavior_group_deployments_group_version_id_index;
DROP INDEX IF EXISTS behavior_group_deployments_user_id_index;

DROP TABLE IF EXISTS behavior_group_deployments;

COMMIT;
