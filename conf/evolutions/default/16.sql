# --- !Ups

ALTER TABLE behavior_versions DROP COLUMN is_active;

# --- !Downs

ALTER TABLE behavior_versions ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT false;

