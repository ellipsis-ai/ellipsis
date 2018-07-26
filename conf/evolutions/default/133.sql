# --- !Ups

BEGIN;

CREATE INDEX behavior_versions_behavior_id_index ON behavior_versions(behavior_id);
CREATE INDEX behavior_versions_group_version_id_index ON behavior_versions(group_version_id);
CREATE INDEX behavior_backed_data_types_behavior_id_index ON behavior_backed_data_types(behavior_id);
CREATE INDEX behavior_group_version_shas_group_version_id_index ON behavior_group_version_shas(group_version_id);
CREATE INDEX data_type_configs_behavior_version_id_index ON data_type_configs(behavior_version_id);
CREATE INDEX data_type_fields_config_id_index ON data_type_fields(config_id);
CREATE INDEX data_type_fields_field_id_index ON data_type_fields(field_id);
CREATE INDEX default_storage_items_updated_by_user_id_index ON default_storage_items(updated_by_user_id);
CREATE INDEX invocation_tokens_user_id_index ON invocation_tokens(user_id);
CREATE INDEX invocation_tokens_scheduled_message_id_index ON invocation_tokens(scheduled_message_id);
CREATE INDEX invocation_tokens_behavior_version_id_index ON invocation_tokens(behavior_version_id);
CREATE INDEX inputs_param_type_index ON inputs(param_type);
CREATE INDEX conversations_last_interaction_at_index ON conversations(last_interaction_at);
CREATE INDEX conversations_started_at_index ON conversations(started_at);
CREATE INDEX conversations_channel_index ON conversations(channel);
CREATE INDEX conversations_state_index ON conversations(state);
CREATE INDEX saved_answers_input_id_index ON saved_answers(input_id);
CREATE INDEX saved_answers_user_id_index ON saved_answers(user_id);
CREATE INDEX slack_bot_profiles_team_id_index ON slack_bot_profiles(team_id);
CREATE INDEX users_team_id_index ON users(team_id);

COMMIT;

# --- !Downs

BEGIN;

DROP INDEX IF EXISTS behavior_versions_behavior_id_index;
DROP INDEX IF EXISTS behavior_versions_group_version_id_index;
DROP INDEX IF EXISTS behavior_backed_data_types_behavior_id_index;
DROP INDEX IF EXISTS behavior_group_version_shas_group_version_id_index;
DROP INDEX IF EXISTS data_type_configs_behavior_version_id_index;
DROP INDEX IF EXISTS data_type_fields_config_id_index;
DROP INDEX IF EXISTS data_type_fields_field_id_index;
DROP INDEX IF EXISTS default_storage_items_updated_by_user_id_index;
DROP INDEX IF EXISTS invocation_tokens_user_id_index;
DROP INDEX IF EXISTS invocation_tokens_scheduled_message_id_index;
DROP INDEX IF EXISTS invocation_tokens_behavior_version_id_index;
DROP INDEX IF EXISTS inputs_param_type_index;
DROP INDEX IF EXISTS conversations_last_interaction_at_index;
DROP INDEX IF EXISTS conversations_started_at_index;
DROP INDEX IF EXISTS conversations_channel_index;
DROP INDEX IF EXISTS conversations_state_index;
DROP INDEX IF EXISTS saved_answers_input_id_index;
DROP INDEX IF EXISTS saved_answers_user_id_index;
DROP INDEX IF EXISTS slack_bot_profiles_team_id_index;
DROP INDEX IF EXISTS users_team_id_index;

COMMIT;


