# --- !Ups

CREATE TABLE behavior_groups (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  team_id TEXT NOT NULL REFERENCES teams(id),
  created_at TIMESTAMP NOT NULL
);

ALTER TABLE inputs ADD COLUMN group_id TEXT REFERENCES behavior_groups(id);

# --- !Downs

ALTER TABLE inputs DROP COLUMN group_id;

DROP TABLE IF EXISTS behavior_groups;
