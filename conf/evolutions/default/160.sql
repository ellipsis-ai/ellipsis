# --- !Ups

BEGIN;

CREATE INDEX linked_accounts_provider_id_index ON linked_accounts(provider_id);
CREATE INDEX linked_accounts_provider_key_index ON linked_accounts(provider_key);
CREATE INDEX linked_accounts_user_id_index ON linked_accounts(user_id);

CREATE INDEX environment_variables_team_id_index ON environment_variables(team_id);

CREATE INDEX dev_mode_channels_context_index ON dev_mode_channels(context);
CREATE INDEX dev_mode_channels_channel_index ON dev_mode_channels(channel);
CREATE INDEX dev_mode_channels_team_id_index ON dev_mode_channels(team_id);
CREATE INDEX dev_mode_channels_created_at_index ON dev_mode_channels(created_at);

ALTER TABLE behavior_versions ALTER COLUMN group_version_id SET NOT NULL;

COMMIT;

# --- !Downs

BEGIN;

DROP INDEX IF EXISTS linked_accounts_provider_id_index;
DROP INDEX IF EXISTS linked_accounts_provider_key_index;
DROP INDEX IF EXISTS linked_accounts_user_id_index;

DROP INDEX IF EXISTS environment_variables_team_id_index;

DROP INDEX IF EXISTS dev_mode_channels_context_index;
DROP INDEX IF EXISTS dev_mode_channels_channel_index;
DROP INDEX IF EXISTS dev_mode_channels_team_id_index;
DROP INDEX IF EXISTS dev_mode_channels_created_at_index;

ALTER TABLE behavior_versions ALTER COLUMN group_version_id DROP NOT NULL;

COMMIT;


