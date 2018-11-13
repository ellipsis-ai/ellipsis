# --- !Ups

BEGIN;

CREATE TABLE ms_teams_bot_profiles (
  team_id TEXT NOT NULL REFERENCES teams(id),
  tenant_id TEXT NOT NULL PRIMARY KEY,
  created_at TIMESTAMPTZ NOT NULL,
  allow_shortcut_mention BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX ms_teams_bot_profiles_team_id_index ON ms_teams_bot_profiles(team_id);
CREATE INDEX ms_teams_bot_profiles_tenant_id_index ON ms_teams_bot_profiles(tenant_id);

COMMIT;

# --- !Downs

BEGIN;

DROP INDEX IF EXISTS ms_teams_bot_profiles_team_id_index;
DROP INDEX IF EXISTS ms_teams_bot_profiles_tenant_id_index;

DROP TABLE IF EXISTS ms_teams_bot_profiles;

COMMIT;
