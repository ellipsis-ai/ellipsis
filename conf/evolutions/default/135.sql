# --- !Ups

BEGIN;

CREATE TABLE oauth1_apis (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  request_token_url TEXT NOT NULL,
  access_token_url TEXT NOT NULL,
  authorization_url TEXT NOT NULL,
  new_application_url TEXT,
  scope_documentation_url TEXT,
  team_id TEXT REFERENCES teams(id)
);

CREATE TABLE oauth1_applications (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  api_id TEXT NOT NULL REFERENCES oauth1_apis(id) ON DELETE CASCADE,
  consumer_key TEXT NOT NULL,
  consumer_secret TEXT NOT NULL,
  scope TEXT,
  team_id TEXT NOT NULL REFERENCES teams(id),
  is_shared BOOL NOT NULL
);

CREATE TABLE oauth_1_tokens (
  token TEXT NOT NULL,
  secret TEXT NOT NULL,
  provider_id TEXT NOT NULL,
  provider_key TEXT NOT NULL,
  PRIMARY KEY (provider_id, provider_key)
);

CREATE TABLE linked_oauth1_tokens (
  access_token TEXT NOT NULL,
  secret TEXT NOT NULL,
  user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  application_id TEXT NOT NULL REFERENCES oauth1_applications(id) ON DELETE CASCADE,
  PRIMARY KEY (user_id, application_id)
);

CREATE TABLE required_oauth1_api_configs (
  id TEXT PRIMARY KEY,
  application_id TEXT REFERENCES oauth1_applications(id) ON DELETE CASCADE,
  api_id TEXT NOT NULL REFERENCES oauth1_apis(id) ON DELETE CASCADE,
  recommended_scope TEXT,
  group_version_id TEXT NOT NULL REFERENCES behavior_group_versions(id) ON DELETE CASCADE,
  name_in_code TEXT NOT NULL DEFAULT 'detault',
  export_id TEXT NOT NULL
);

CREATE INDEX required_oauth1_api_configs_api_id_index ON required_oauth1_api_configs(api_id);
CREATE INDEX required_oauth1_api_configs_group_version_id_index ON required_oauth1_api_configs(group_version_id);
CREATE INDEX required_oauth1_applications_application_id_index ON required_oauth1_api_configs(application_id);

COMMIT;

# --- !Downs

BEGIN;

DROP INDEX IF EXISTS required_oauth1_api_configs_api_id_index;
DROP INDEX IF EXISTS required_oauth1_api_configs_group_version_id_index;
DROP INDEX IF EXISTS required_oauth1_applications_application_id_index;

DROP TABLE IF EXISTS required_oauth1_api_configs;
DROP TABLE IF EXISTS linked_oauth1_tokens;
DROP TABLE IF EXISTS oauth_1_tokens;
DROP TABLE IF EXISTS oauth1_applications;
DROP TABLE IF EXISTS oauth1_apis;

COMMIT;
