# --- !Ups

DELETE FROM api_tokens;

ALTER TABLE api_tokens DROP COLUMN team_id;
ALTER TABLE api_tokens ADD COLUMN user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE;

# --- !Downs

DELETE FROM api_tokens;

ALTER TABLE api_tokens ADD COLUMN team_id TEXT NOT NULL REFERENCES teams(id) ON DELETE CASCADE;
ALTER TABLE api_tokens DROP COLUMN user_id;
