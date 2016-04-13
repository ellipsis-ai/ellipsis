# --- !Ups

CREATE TABLE slack_bot_profiles (
  user_id TEXT PRIMARY KEY,
  team_id TEXT NOT NULL,
  token TEXT NOT NULL
);

# --- !Downs

DROP TABLE IF EXISTS slack_bot_profiles;
