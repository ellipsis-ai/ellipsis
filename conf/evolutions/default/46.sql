# --- !Ups

CREATE TABLE simple_token_apis (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  token_url TEXT,
  team_id TEXT REFERENCES teams(id) ON DELETE CASCADE
);

CREATE TABLE required_simple_token_apis (
  id TEXT PRIMARY KEY,
  behavior_version_id TEXT NOT NULL REFERENCES behavior_versions(id) ON DELETE CASCADE,
  api_id TEXT NOT NULL REFERENCES simple_token_apis(id) ON DELETE CASCADE
);

# --- !Downs

DROP TABLE IF EXISTS required_simple_token_apis;
DROP TABLE IF EXISTS simple_token_apis;
