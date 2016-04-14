# --- !Ups

CREATE TABLE teams (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL
);

CREATE TABLE calls (
  id TEXT PRIMARY KEY,
  text TEXT NOT NULL,
  team_id TEXT NOT NULL
);

CREATE TABLE responses (
  id TEXT PRIMARY KEY,
  text TEXT NOT NULL,
  call_id TEXT NOT NULL REFERENCES calls(id)
);

CREATE TABLE slack_bot_profiles (
  user_id TEXT PRIMARY KEY,
  team_id TEXT NOT NULL REFERENCES teams(id),
  slack_team_id TEXT NOT NULL,
  token TEXT NOT NULL
);

# --- !Downs

DROP TABLE IF EXISTS slack_bot_profiles;
DROP TABLE IF EXISTS responses;
DROP TABLE IF EXISTS calls;
DROP TABLE IF EXISTS teams;
