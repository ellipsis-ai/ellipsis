# --- !Ups

ALTER TABLE custom_oauth2_configurations ADD COLUMN name TEXT NOT NULL;

CREATE TABLE linked_oauth_2_tokens (
  access_token TEXT NOT NULL,
  token_type TEXT,
  expiration_time TIMESTAMP,
  refresh_token TEXT,
  user_id TEXT NOT NULL REFERENCES users(id),
  config_id TEXT NOT NULL REFERENCES custom_oauth2_configurations(id),
  PRIMARY KEY (user_id, config_id)
);

# --- !Downs

DROP TABLE IF EXISTS linked_oauth_2_tokens;

ALTER TABLE custom_oauth2_configurations DROP COLUMN name;

