# --- !Ups

DROP TABLE user_environment_variables;

# --- !Downs

CREATE TABLE user_environment_variables (
  name TEXT NOT NULL,
  value TEXT NOT NULL,
  user_id TEXT NOT NULL REFERENCES users(id),
  created_at TIMESTAMPTZ NOT NULL,
  PRIMARY KEY(name, user_id)
);
