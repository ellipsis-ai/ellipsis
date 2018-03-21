# --- !Ups

BEGIN;

CREATE INDEX required_oauth2_api_configs_api_id_index ON required_oauth2_api_configs(api_id);
CREATE INDEX required_oauth2_api_configs_group_version_id_index ON required_oauth2_api_configs(group_version_id);
CREATE INDEX required_oauth2_applications_application_id_index ON required_oauth2_api_configs(application_id);

CREATE INDEX conversations_behavior_version_id_index ON conversations(behavior_version_id);
CREATE INDEX conversations_scheduled_message_id_index ON conversations(scheduled_message_id);
CREATE INDEX conversations_trigger_id_index ON conversations(trigger_id);

CREATE INDEX library_versions_group_version_id_index ON library_versions(group_version_id);

COMMIT;

# --- !Downs

BEGIN;

DROP INDEX IF EXISTS required_oauth2_api_configs_api_id_index;
DROP INDEX IF EXISTS required_oauth2_api_configs_group_version_id_index;
DROP INDEX IF EXISTS required_oauth2_applications_application_id_index;

DROP INDEX IF EXISTS conversations_behavior_version_id_index;
DROP INDEX IF EXISTS conversations_scheduled_message_id_index;
DROP INDEX IF EXISTS conversations_trigger_id_index;

DROP INDEX IF EXISTS library_versions_group_version_id_index;

COMMIT;
