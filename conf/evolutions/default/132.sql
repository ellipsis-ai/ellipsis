# --- !Ups

BEGIN;

CREATE INDEX collected_parameter_values_conversation_id_index ON collected_parameter_values(conversation_id);
CREATE INDEX collected_parameter_values_parameter_id_index ON collected_parameter_values(parameter_id);
CREATE INDEX behavior_parameters_behavior_version_id_index ON behavior_parameters(behavior_version_id);
CREATE INDEX behavior_parameters_input_id_index ON behavior_parameters(input_id);
CREATE INDEX inputs_input_id_index ON inputs(input_id);
CREATE INDEX behavior_group_versions_group_id_index ON behavior_group_versions(group_id);
CREATE INDEX behavior_group_versions_author_id_index ON behavior_group_versions(author_id);
CREATE INDEX behaviors_group_id_index ON behaviors(group_id);
CREATE INDEX behaviors_team_id_index ON behaviors(team_id);
CREATE INDEX behavior_groups_team_id_index ON behavior_groups(team_id);
CREATE INDEX parent_conversations_parent_id_index ON parent_conversations(parent_id);
CREATE INDEX parent_conversations_param_id_index ON parent_conversations(param_id);
CREATE INDEX conversations_parent_id_index ON conversations(parent_id);

COMMIT;

# --- !Downs

BEGIN;

DROP INDEX IF EXISTS collected_parameter_values_conversation_id_index;
DROP INDEX IF EXISTS collected_parameter_values_parameter_id_index;
DROP INDEX IF EXISTS behavior_parameters_behavior_version_id_index;
DROP INDEX IF EXISTS behavior_parameters_input_id_index;
DROP INDEX IF EXISTS inputs_input_id_index;
DROP INDEX IF EXISTS behavior_group_versions_group_id_index;
DROP INDEX IF EXISTS behavior_group_versions_author_id_index;
DROP INDEX IF EXISTS behaviors_group_id_index;
DROP INDEX IF EXISTS behaviors_team_id_index;
DROP INDEX IF EXISTS behavior_groups_team_id_index;
DROP INDEX IF EXISTS parent_conversations_parent_id_index;
DROP INDEX IF EXISTS parent_conversations_param_id_index;
DROP INDEX IF EXISTS conversations_parent_id_index;

COMMIT;
