# --- !Ups

ALTER TABLE slack_profiles ALTER COLUMN team_name DROP NOT NULL;
ALTER TABLE slack_profiles ALTER COLUMN team_url DROP NOT NULL;
ALTER TABLE slack_profiles ALTER COLUMN user_name DROP NOT NULL;

# --- !Downs

ALTER TABLE slack_profiles ALTER COLUMN team_name SET NOT NULL;
ALTER TABLE slack_profiles ALTER COLUMN team_url SET NOT NULL;
ALTER TABLE slack_profiles ALTER COLUMN user_name SET NOT NULL;
