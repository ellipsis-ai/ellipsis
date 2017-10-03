# --- !Ups

ALTER TABLE behavior_groups ADD COLUMN is_builtin BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE behavior_versions ADD COLUMN builtin_name TEXT;

# --- !Downs

ALTER TABLE behavior_versions DROP COLUMN builtin_name;
ALTER TABLE behavior_groups DROP COLUMN is_builtin;
