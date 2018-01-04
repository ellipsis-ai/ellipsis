# --- !Ups

BEGIN;

ALTER TABLE required_aws_configs ADD COLUMN export_id TEXT;

UPDATE required_aws_configs
SET export_id = (SELECT group_id FROM behavior_group_versions WHERE id = group_version_id) || '-aws-' || name_in_code;

ALTER TABLE required_aws_configs ALTER COLUMN export_id SET NOT NULL;

ALTER TABLE required_oauth2_api_configs ADD COLUMN export_id TEXT;

UPDATE required_oauth2_api_configs
SET export_id = (SELECT group_id FROM behavior_group_versions WHERE id = group_version_id) || '-' || api_id || '-' || name_in_code;

ALTER TABLE required_oauth2_api_configs ALTER COLUMN export_id SET NOT NULL;

ALTER TABLE required_simple_token_apis ADD COLUMN export_id TEXT;

UPDATE required_simple_token_apis
SET export_id = (SELECT group_id FROM behavior_group_versions WHERE id = group_version_id) || '-' || api_id || '-'|| name_in_code;

ALTER TABLE required_simple_token_apis ALTER COLUMN export_id SET NOT NULL;

COMMIT;

# --- !Downs

BEGIN;

ALTER TABLE required_aws_configs DROP COLUMN IF EXISTS export_id;
ALTER TABLE required_oauth2_api_configs DROP COLUMN IF EXISTS export_id;
ALTER TABLE required_simple_token_apis DROP COLUMN IF EXISTS export_id;

COMMIT;
