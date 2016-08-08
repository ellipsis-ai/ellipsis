# --- !Ups

CREATE TABLE custom_oauth2_configuration_templates (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  authorization_url TEXT NOT NULL,
  access_token_url TEXT NOT NULL,
  get_profile_url TEXT NOT NULL,
  get_profile_json_path TEXT NOT NULL,
  team_id TEXT
);


CREATE TABLE custom_oauth2_configurations (
  id TEXT PRIMARY KEY,
  template_id TEXT NOT NULL REFERENCES custom_oauth2_configuration_templates(id),
  client_id TEXT NOT NULL,
  client_secret TEXT NOT NULL,
  scope TEXT,
  team_id TEXT NOT NULL
);

# --- !Downs

DROP TABLE IF EXISTS custom_oauth2_configurations;
DROP TABLE IF EXISTS custom_oauth2_configuration_templates;
