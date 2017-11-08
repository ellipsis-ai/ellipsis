# --- !Ups

CREATE TABLE github_profiles (
  provider_id TEXT NOT NULL,
  provider_key TEXT NOT NULL,
  token TEXT NOT NULL,
  PRIMARY KEY(provider_id, provider_key)
);

# --- !Downs

DROP TABLE github_profiles;
