# --- !Ups

CREATE TABLE users (
  id TEXT PRIMARY KEY,
  email TEXT,
  UNIQUE (email)
);

CREATE TABLE oauth_2_tokens (
  access_token TEXT NOT NULL,
  slack_scopes TEXT,
  token_type TEXT,
  expiration_time TIMESTAMP,
  refresh_token TEXT,
  provider_id TEXT NOT NULL,
  provider_key TEXT NOT NULL,
  PRIMARY KEY(provider_id, provider_key)
);

CREATE TABLE linked_accounts (
  provider_id TEXT NOT NULL,
  provider_key TEXT NOT NULL,
  user_id TEXT NOT NULL REFERENCES users(id),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(provider_id, provider_key, user_id),
  UNIQUE (provider_id, provider_key)
);

CREATE TABLE slack_profiles (
  provider_id TEXT NOT NULL,
  provider_key TEXT NOT NULL,
  team_url TEXT NOT NULL,
  team_name TEXT NOT NULL,
  user_name TEXT NOT NULL,
  team_id TEXT NOT NULL,
  PRIMARY KEY(provider_id, provider_key)
);

# --- !Downs

DROP TABLE IF EXISTS oauth_2_tokens;
DROP TABLE IF EXISTS linked_accounts;
DROP TABLE IF EXISTS slack_profiles;
DROP TABLE IF EXISTS users;
