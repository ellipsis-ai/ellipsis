# --- !Ups

BEGIN;

ALTER TABLE required_aws_configs ADD COLUMN required_id TEXT;

UPDATE required_aws_configs
SET required_id = (SELECT group_id FROM behavior_group_versions WHERE id = group_version_id) || '-aws-' || name_in_code;

ALTER TABLE required_aws_configs ALTER COLUMN required_id SET NOT NULL;

ALTER TABLE required_oauth2_api_configs ADD COLUMN required_id TEXT;

UPDATE required_oauth2_api_configs
SET required_id = (SELECT group_id FROM behavior_group_versions WHERE id = group_version_id) || '-' || api_id || '-' || name_in_code;

ALTER TABLE required_oauth2_api_configs ALTER COLUMN required_id SET NOT NULL;

ALTER TABLE required_simple_token_apis ADD COLUMN required_id TEXT;

UPDATE required_simple_token_apis
SET required_id = (SELECT group_id FROM behavior_group_versions WHERE id = group_version_id) || '-' || api_id || '-'|| name_in_code;

ALTER TABLE required_simple_token_apis ALTER COLUMN required_id SET NOT NULL;

COMMIT;

# --- !Downs

BEGIN;

ALTER TABLE required_aws_configs DROP COLUMN IF EXISTS required_id;
ALTER TABLE required_oauth2_api_configs DROP COLUMN IF EXISTS required_id;
ALTER TABLE required_simple_token_apis DROP COLUMN IF EXISTS required_id;

COMMIT;
