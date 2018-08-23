# --- !Ups

BEGIN;

CREATE TABLE oauth1_apis (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  request_token_url TEXT NOT NULL,
  access_token_url TEXT NOT NULL,
  authorization_url TEXT NOT NULL,
  new_application_url TEXT,
  team_id TEXT REFERENCES teams(id)
);

CREATE TABLE oauth1_applications (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  api_id TEXT NOT NULL REFERENCES oauth1_apis(id) ON DELETE CASCADE,
  consumer_key TEXT NOT NULL,
  consumer_secret TEXT NOT NULL,
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
  application_id TEXT NOT NULL REFERENCES oauth2_applications(id) ON DELETE CASCADE,
  PRIMARY KEY (user_id, application_id)
);

COMMIT;

# --- !Downs

BEGIN;

DROP TABLE IF EXISTS linked_oauth1_tokens;
DROP TABLE IF EXISTS oauth_1_tokens;
DROP TABLE IF EXISTS oauth1_applictions;
DROP TABLE IF EXISTS oauth1_apis;

COMMIT;
