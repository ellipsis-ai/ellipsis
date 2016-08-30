# --- !Ups

CREATE TABLE login_tokens (
  value TEXT PRIMARY KEY,
  user_id TEXT NOT NULL REFERENCES users(id),
  is_used BOOL NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL
);

# --- !Downs

DROP TABLE IF EXISTS login_tokens;
