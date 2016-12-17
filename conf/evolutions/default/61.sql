# --- !Ups

TRUNCATE TABLE invocation_tokens;
ALTER TABLE invocation_tokens ADD COLUMN user_id TEXT NOT NULL REFERENCES users(id);
ALTER TABLE invocation_tokens DROP COLUMN team_id;

# --- !Downs

TRUNCATE TABLE invocation_tokens;
ALTER TABLE invocation_tokens ADD COLUMN team_id TEXT NOT NULL REFERENCES teams(id);
ALTER TABLE invocation_tokens DROP COLUMN user_id;
