# --- !Ups

BEGIN;

ALTER TABLE required_oauth2_api_configs
DROP CONSTRAINT required_oauth2_api_configs_group_version_id_fkey,
ADD CONSTRAINT required_oauth2_api_configs_group_version_id_fkey
  FOREIGN KEY(group_version_id)
  REFERENCES behavior_group_versions(id)
  ON DELETE CASCADE;

ALTER TABLE required_simple_token_apis
DROP CONSTRAINT required_simple_token_apis_group_version_id_fkey,
ADD CONSTRAINT required_simple_token_apis_group_version_id_fkey
  FOREIGN KEY(group_version_id)
  REFERENCES behavior_group_versions(id)
  ON DELETE CASCADE;

COMMIT;

# --- !Downs

BEGIN;

ALTER TABLE required_oauth2_api_configs
DROP CONSTRAINT required_oauth2_api_configs_group_version_id_fkey,
ADD CONSTRAINT required_oauth2_api_configs_group_version_id_fkey
  FOREIGN KEY(group_version_id)
  REFERENCES behavior_group_versions(id)
  ON DELETE NO ACTION;

ALTER TABLE required_simple_token_apis
DROP CONSTRAINT required_simple_token_apis_group_version_id_fkey,
ADD CONSTRAINT required_simple_token_apis_group_version_id_fkey
  FOREIGN KEY(group_version_id)
  REFERENCES behavior_group_versions(id)
  ON DELETE NO ACTION;

COMMIT;
