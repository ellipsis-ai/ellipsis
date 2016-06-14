# --- !Ups

ALTER TABLE behaviors RENAME TO behavior_versions;
ALTER TABLE behavior_parameters RENAME COLUMN behavior_id TO behavior_version_id;
ALTER TABLE conversations RENAME COLUMN behavior_id TO behavior_version_id;
ALTER TABLE message_triggers RENAME COLUMN behavior_id TO behavior_version_id;

# --- !Downs

ALTER TABLE message_triggers RENAME COLUMN behavior_version_id TO behavior_id;
ALTER TABLE conversations RENAME COLUMN behavior_version_id TO behavior_id;
ALTER TABLE behavior_parameters RENAME COLUMN behavior_version_id TO behavior_id;
ALTER TABLE behavior_versions RENAME TO behaviors;
