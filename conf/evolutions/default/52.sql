# --- !Ups

ALTER TABLE behaviors ADD COLUMN group_id TEXT REFERENCES behavior_groups(id);

# --- !Downs

ALTER TABLE behaviors DROP COLUMN group_id;
