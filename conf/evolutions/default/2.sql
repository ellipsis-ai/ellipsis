# --- !Ups

CREATE TABLE invocation_tokens (
  id TEXT PRIMARY KEY,
  team_id TEXT NOT NULL REFERENCES teams(id),
  is_used BOOL NOT NULL,
  created_at TIMESTAMP NOT NULL
);

# --- !Downs

DROP TABLE IF EXISTS invocation_tokens;
