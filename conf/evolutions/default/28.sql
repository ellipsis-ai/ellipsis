# --- !Ups

CREATE TABLE custom_oauth2_configuration_templates (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  authorization_url TEXT NOT NULL,
  access_token_url TEXT NOT NULL,
  team_id TEXT
);


CREATE TABLE custom_oauth2_configurations (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  template_id TEXT NOT NULL REFERENCES custom_oauth2_configuration_templates(id),
  client_id TEXT NOT NULL,
  client_secret TEXT NOT NULL,
  scope TEXT,
  team_id TEXT NOT NULL
);

CREATE TABLE linked_oauth_2_tokens (
  access_token TEXT NOT NULL,
  token_type TEXT,
  expiration_time TIMESTAMP,
  refresh_token TEXT,
  scope_granted TEXT,
  user_id TEXT NOT NULL REFERENCES users(id),
  config_id TEXT NOT NULL REFERENCES custom_oauth2_configurations(id),
  PRIMARY KEY (user_id, config_id)
);

# --- !Downs

DROP TABLE IF EXISTS linked_oauth_2_tokens;
DROP TABLE IF EXISTS custom_oauth2_configurations;
DROP TABLE IF EXISTS custom_oauth2_configuration_templates;
