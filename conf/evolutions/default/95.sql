# --- !Ups

BEGIN;

ALTER TABLE required_oauth2_api_configs ADD COLUMN group_version_id TEXT REFERENCES behavior_group_versions(id);

UPDATE required_oauth2_api_configs as roac SET group_version_id = (SELECT group_version_id FROM behavior_versions WHERE id = roac.behavior_version_id LIMIT 1);

ALTER TABLE required_oauth2_api_configs ALTER COLUMN group_version_id SET NOT NULL;
ALTER TABLE required_oauth2_api_configs DROP COLUMN behavior_version_id;

DELETE FROM required_oauth2_api_configs WHERE id NOT IN (
  SELECT MIN(id) FROM required_oauth2_api_configs GROUP BY api_id, group_version_id
);

ALTER TABLE required_simple_token_apis ADD COLUMN group_version_id TEXT REFERENCES behavior_group_versions(id);

UPDATE required_simple_token_apis as rsta SET group_version_id = (SELECT group_version_id FROM behavior_versions WHERE id = rsta.behavior_version_id LIMIT 1);

ALTER TABLE required_simple_token_apis ALTER COLUMN group_version_id SET NOT NULL;
ALTER TABLE required_simple_token_apis DROP COLUMN behavior_version_id;

DELETE FROM required_simple_token_apis WHERE id NOT IN (
  SELECT MIN(id) FROM required_simple_token_apis GROUP BY api_id, group_version_id
);

COMMIT;

# --- !Downs

BEGIN;

-- since we can't reconstitute the previous required configs anyway, just delete them all

DELETE FROM required_simple_token_apis;
ALTER TABLE required_simple_token_apis ADD COLUMN behavior_version_id TEXT NOT NULL REFERENCES behavior_versions(id);
ALTER TABLE required_simple_token_apis DROP COLUMN group_version_id;

DELETE FROM required_oauth2_api_configs;
ALTER TABLE required_oauth2_api_configs ADD COLUMN behavior_version_id TEXT NOT NULL REFERENCES behavior_versions(id);
ALTER TABLE required_oauth2_api_configs DROP COLUMN group_version_id;

COMMIT;
