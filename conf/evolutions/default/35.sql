# --- !Ups

CREATE TABLE login_tokens (
  value TEXT PRIMARY KEY,
  user_id TEXT NOT NULL REFERENCES users(id),
  created_at TIMESTAMP NOT NULL
);

# --- !Downs

DROP TABLE IF EXISTS login_tokens;
