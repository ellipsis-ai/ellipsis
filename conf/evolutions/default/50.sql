# --- !Ups

CREATE TABLE saved_answers (
  id TEXT PRIMARY KEY,
  input_id TEXT NOT NULL REFERENCES inputs(id),
  value_string TEXT NOT NULL,
  user_id TEXT REFERENCES users(id),
  created_at TIMESTAMP NOT NULL
);

# --- !Downs

DROP TABLE IF EXISTS saved_answers;
