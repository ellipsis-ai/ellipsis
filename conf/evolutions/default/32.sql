# --- !Ups

CREATE TABLE api_tokens (
  id TEXT PRIMARY KEY,
  label TEXT NOT NULL,
  team_id TEXT NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
  is_revoked BOOL NOT NULL DEFAULT FALSE,
  last_used TIMESTAMP,
  created_at TIMESTAMP NOT NULL
);

# --- !Downs

DROP TABLE IF EXISTS api_tokens;
