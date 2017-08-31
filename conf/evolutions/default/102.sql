# --- !Ups

BEGIN;

CREATE TABLE node_module_versions(
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  version TEXT NOT NULL,
  group_version_id TEXT NOT NULL REFERENCES behavior_group_versions(id) ON DELETE CASCADE
);

CREATE INDEX node_module_versions_group_version_id_index ON node_module_versions(group_version_id);

COMMIT;

# --- !Downs

BEGIN;

DROP INDEX IF EXISTS node_module_versions_group_version_id_index;

DROP TABLE IF EXISTS node_module_versions;

COMMIT;
