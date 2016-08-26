# --- !Ups

ALTER TABLE required_oauth2_applications RENAME TO required_oauth2_api_configs;

ALTER TABLE required_oauth2_api_configs ALTER COLUMN application_id DROP NOT NULL;
ALTER TABLE required_oauth2_api_configs ADD COLUMN api_id TEXT REFERENCES oauth2_apis(id);
ALTER TABLE required_oauth2_api_configs ADD COLUMN required_scope TEXT NOT NULL DEFAULT '';

UPDATE required_oauth2_api_configs
SET api_id = apps.api_id, required_scope = apps.scope
FROM
  (SELECT id, api_id, scope
   FROM oauth2_applications) AS apps
WHERE required_oauth2_api_configs.application_id = apps.id;

ALTER TABLE required_oauth2_api_configs ALTER COLUMN api_id SET NOT NULL;

# --- !Downs

ALTER TABLE required_oauth2_api_configs DROP COLUMN required_scope;
ALTER TABLE required_oauth2_api_configs DROP COLUMN api_id;
ALTER TABLE required_oauth2_api_configs ALTER COLUMN application_id SET NOT NULL;

ALTER TABLE required_oauth2_api_configs RENAME TO required_oauth2_applications;
