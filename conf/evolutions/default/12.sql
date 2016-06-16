# --- !Ups

ALTER TABLE behavior_versions ADD COLUMN behavior_id TEXT;

UPDATE behavior_versions
SET behavior_id = id;

CREATE TABLE behaviors (
  id TEXT PRIMARY KEY,
  team_id TEXT NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO behaviors (
  SELECT behavior_id, team_id, created_at
  FROM behavior_versions
);

ALTER TABLE behavior_versions ADD  FOREIGN KEY(behavior_id) REFERENCES behaviors(id);
ALTER TABLE behavior_versions ALTER COLUMN behavior_id SET NOT NULL;

ALTER TABLE behavior_versions DROP COLUMN team_id;

# --- !Downs

ALTER TABLE behavior_versions ADD COLUMN team_id TEXT REFERENCES teams(id);
ALTER TABLE behavior_versions DROP COLUMN behavior_id;
DROP TABLE IF EXISTS behaviors;
