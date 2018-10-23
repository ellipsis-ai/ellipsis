# --- !Ups

BEGIN;

ALTER TABLE slack_bot_profiles ADD COLUMN enterprise_id TEXT;

ALTER TABLE slack_bot_profiles DROP CONSTRAINT slack_bot_profiles_pkey;

ALTER TABLE slack_bot_profiles ADD PRIMARY KEY (user_id, slack_team_id);

CREATE INDEX slack_bot_profiles_enterprise_id_index ON slack_bot_profiles(enterprise_id);

COMMIT;

# --- !Downs

BEGIN;

DROP INDEX IF EXISTS slack_bot_profiles_enterprise_id_index;

ALTER TABLE slack_bot_profiles DROP COLUMN enterprise_id;

COMMIT;
