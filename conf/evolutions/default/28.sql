# --- !Ups

CREATE TABLE custom_oauth2_configurations (
  name TEXT NOT NULL,
  authorization_url TEXT NOT NULL,
  access_token_url TEXT NOT NULL,
  get_profile_url TEXT NOT NULL,
  get_profile_json_path TEXT NOT NULL,
  client_id TEXT NOT NULL,
  client_secret TEXT NOT NULL,
  scope TEXT,
  team_id TEXT NOT NULL,
  PRIMARY KEY(name, team_id)
);

# --- !Downs

DROP TABLE IF EXISTS custom_oauth2_configurations;
