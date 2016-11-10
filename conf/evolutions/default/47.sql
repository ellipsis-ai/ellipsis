# --- !Ups

CREATE TABLE linked_simple_tokens (
  access_token TEXT NOT NULL,
  user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  api_id TEXT NOT NULL REFERENCES simple_token_apis ON DELETE CASCADE,
  PRIMARY KEY (user_id, api_id)
);

# --- !Downs

DROP TABLE IF EXISTS linked_simple_tokens;
