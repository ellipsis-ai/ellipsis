# --- !Ups

BEGIN;

CREATE TABLE required_aws_configs (
  id TEXT PRIMARY KEY,
  group_version_id TEXT NOT NULL REFERENCES behavior_group_versions(id) ON DELETE CASCADE,
  config_id TEXT REFERENCES aws_configs(id) ON DELETE SET NULL
);

CREATE INDEX required_aws_configs_group_version_id_index ON required_aws_configs(group_version_id);

ALTER TABLE aws_configs ADD COLUMN name TEXT NOT NULL DEFAULT 'Default';
ALTER TABLE aws_configs ADD COLUMN team_id TEXT REFERENCES teams(id);
ALTER TABLE aws_configs ADD COLUMN access_key_id TEXT;
ALTER TABLE aws_configs ADD COLUMN secret_access_key TEXT;
ALTER TABLE aws_configs ADD COLUMN region TEXT;

UPDATE aws_configs a SET team_id = (
  SELECT b.team_id FROM behavior_versions bv JOIN behaviors b ON bv.behavior_id = b.id
  WHERE a.behavior_version_id = bv.id
  LIMIT 1
);

UPDATE aws_configs a SET access_key_id = (
  SELECT e.value FROM environment_variables e WHERE e.team_id = a.team_id AND e.name = a.access_key_name LIMIT 1
);

UPDATE aws_configs a SET secret_access_key = (
  SELECT e.value FROM environment_variables e WHERE e.team_id = a.team_id AND e.name = a.secret_key_name LIMIT 1
);

UPDATE aws_configs a SET region = (
  SELECT e.value FROM environment_variables e WHERE e.team_id = a.team_id AND e.name = a.region_name LIMIT 1
);

ALTER TABLE aws_configs ALTER COLUMN team_id SET NOT NULL;
ALTER TABLE aws_configs DROP COLUMN behavior_version_id;

COMMIT;

# --- !Downs

BEGIN;

DELETE FROM aws_configs;

ALTER TABLE aws_configs ADD COLUMN behavior_version_id TEXT NOT NULL REFERENCES behavior_versions(id);

ALTER TABLE aws_configs DROP COLUMN team_id;
ALTER TABLE aws_configs DROP COLUMN name;
ALTER TABLE aws_configs DROP COLUMN access_key_id;
ALTER TABLE aws_configs DROP COLUMN secret_access_key;
ALTER TABLE aws_configs DROP COLUMN region;

DROP INDEX IF EXISTS required_aws_configs_group_version_id_index;

DROP TABLE IF EXISTS required_aws_configs;

COMMIT;
