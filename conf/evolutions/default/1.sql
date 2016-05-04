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

CREATE TABLE teams (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL
);

CREATE TABLE slack_bot_profiles (
  user_id TEXT PRIMARY KEY,
  team_id TEXT NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
  slack_team_id TEXT NOT NULL,
  token TEXT NOT NULL
);

CREATE TABLE behaviors (
  id TEXT PRIMARY KEY,
  team_id TEXT NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
  description TEXT,
  short_name TEXT,
  has_code BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE regex_message_triggers (
  id TEXT PRIMARY KEY,
  behavior_id TEXT NOT NULL REFERENCES behaviors(id) ON DELETE CASCADE,
  regex TEXT NOT NULL
);

CREATE TABLE behavior_parameters (
  id TEXT PRIMARY KEY,
  behavior_id TEXT NOT NULL REFERENCES behaviors(id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  rank INT NOT NULL,
  question TEXT,
  param_type TEXT,
  UNIQUE(behavior_id, rank)
);

CREATE TABLE template_message_triggers (
  id TEXT PRIMARY KEY,
  behavior_id TEXT NOT NULL REFERENCES behaviors(id) ON DELETE CASCADE,
  template TEXT NOT NULL
);

CREATE TABLE conversations (
  id TEXT PRIMARY KEY,
  behavior_id TEXT NOT NULL REFERENCES behaviors(id) ON DELETE CASCADE,
  conversation_type TEXT NOT NULL, -- learning_behavior, editing_behavior, invoking_behavior
  context TEXT NOT NULL, -- Slack, etc
  user_id_for_context TEXT NOT NULL, -- Slack user id, etc
  started_at TIMESTAMP NOT NULL,
  state TEXT NOT NULL
);

CREATE TABLE collected_parameter_values (
  parameter_id TEXT NOT NULL REFERENCES behavior_parameters(id) ON DELETE CASCADE,
  conversation_id TEXT NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
  value_string TEXT,
  PRIMARY KEY (parameter_id, conversation_id)
);

# --- !Downs

DROP TABLE IF EXISTS collected_parameter_values;
DROP TABLE IF EXISTS conversations;
DROP TABLE IF EXISTS template_message_triggers;
DROP TABLE IF EXISTS behavior_parameters;
DROP TABLE IF EXISTS regex_message_triggers;
DROP TABLE IF EXISTS behaviors;
DROP TABLE IF EXISTS slack_bot_profiles;
DROP TABLE IF EXISTS teams;
DROP TABLE IF EXISTS oauth_2_tokens;
DROP TABLE IF EXISTS linked_accounts;
DROP TABLE IF EXISTS slack_profiles;
DROP TABLE IF EXISTS users;
