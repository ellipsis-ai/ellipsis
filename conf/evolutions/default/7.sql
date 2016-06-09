# --- !Ups

ALTER TABLE slack_profiles DROP COLUMN team_name;
ALTER TABLE slack_profiles DROP COLUMN team_url;
ALTER TABLE slack_profiles DROP COLUMN user_name;

# --- !Downs

ALTER TABLE slack_profiles ADD COLUMN team_name TEXT NOT NULL;
ALTER TABLE slack_profiles ADD COLUMN team_url TEXT NOT NULL;
ALTER TABLE slack_profiles ADD COLUMN user_name TEXT NOT NULL;
