# --- !Ups

<<<<<<< HEAD
ALTER TABLE teams ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;

# --- !Downs

ALTER TABLE teams DROP COLUMN created_at;
=======
BEGIN;

CREATE TABLE required_aws_configs (
  id TEXT PRIMARY KEY,
  name_in_code TEXT NOT NULL,
  group_version_id TEXT NOT NULL REFERENCES behavior_group_versions(id) ON DELETE CASCADE,
  config_id TEXT REFERENCES aws_configs(id) ON DELETE SET NULL
);

CREATE INDEX required_aws_configs_group_version_id_index ON required_aws_configs(group_version_id);

DELETE FROM aws_configs;

ALTER TABLE aws_configs ADD COLUMN name TEXT NOT NULL DEFAULT 'Default';
ALTER TABLE aws_configs ADD COLUMN team_id TEXT NOT NULL REFERENCES teams(id);
ALTER TABLE aws_configs ADD COLUMN access_key_id TEXT;
ALTER TABLE aws_configs ADD COLUMN secret_access_key TEXT;
ALTER TABLE aws_configs ADD COLUMN region TEXT;
ALTER TABLE aws_configs DROP COLUMN behavior_version_id;

ALTER TABLE required_oauth2_api_configs ADD COLUMN name_in_code TEXT NOT NULL DEFAULT 'default';
ALTER TABLE required_simple_token_apis ADD COLUMN name_in_code TEXT NOT NULL DEFAULT 'default';

COMMIT;

# --- !Downs

BEGIN;

ALTER TABLE required_oauth2_api_configs DROP COLUMN name_in_code;
ALTER TABLE required_simple_token_apis DROP COLUMN name_in_code;

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
>>>>>>> origin/master
