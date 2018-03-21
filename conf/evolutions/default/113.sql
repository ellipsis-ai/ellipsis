# --- !Ups

ALTER TABLE behavior_group_versions ADD COLUMN git_sha TEXT;

# --- !Downs

ALTER TABLE behavior_group_versions DROP COLUMN git_sha;
