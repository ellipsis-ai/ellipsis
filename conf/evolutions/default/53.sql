# --- !Ups

ALTER TABLE behavior_groups ADD COLUMN imported_id TEXT;

# --- !Downs

ALTER TABLE behavior_groups DROP COLUMN imported_id;
