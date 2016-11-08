# --- !Ups

CREATE TABLE user_environment_variables (
  name TEXT NOT NULL,
  value TEXT NOT NULL,
  user_id TEXT NOT NULL REFERENCES users(id),
  created_at TIMESTAMP NOT NULL,
  PRIMARY KEY(name, user_id)
);

# --- !Downs

DROP TABLE IF EXISTS user_environment_variables;
