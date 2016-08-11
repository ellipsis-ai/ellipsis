# --- !Ups

CREATE TABLE required_oauth2_applications (
  id TEXT PRIMARY KEY,
  behavior_version_id TEXT NOT NULL REFERENCES behavior_versions(id) ON DELETE CASCADE,
  application_id TEXT NOT NULL REFERENCES oauth2_applications(id) ON DELETE CASCADE
);

# --- !Downs

DROP TABLE IF EXISTS required_oauth2_applications;
