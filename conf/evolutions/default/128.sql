# --- !Ups

CREATE TABLE managed_behavior_groups (
  id TEXT PRIMARY KEY,
  group_id TEXT NOT NULL REFERENCES behavior_groups(id)
);

# --- !Downs

DROP TABLE IF EXISTS managed_behavior_groups;
