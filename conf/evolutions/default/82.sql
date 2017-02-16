# --- !Ups

ALTER TABLE behaviors RENAME COLUMN imported_id TO export_id;
ALTER TABLE behavior_groups RENAME COLUMN imported_id TO export_id;

# --- !Downs

ALTER TABLE behaviors RENAME COLUMN export_id TO imported_id;
ALTER TABLE behavior_groups RENAME COLUMN export_id TO imported_id;
