# --- !Ups

ALTER TABLE conversations
DROP CONSTRAINT conversations_behavior_version_id_fkey,
ADD CONSTRAINT conversations_behavior_version_id_fkey
  FOREIGN KEY(behavior_version_id)
  REFERENCES behavior_versions(id)
  ON DELETE CASCADE;

# --- !Downs

ALTER TABLE conversations
DROP CONSTRAINT conversations_behavior_version_id_fkey,
ADD CONSTRAINT conversations_behavior_version_id_fkey
  FOREIGN KEY(behavior_version_id)
  REFERENCES behavior_versions(id)
  ON DELETE NO ACTION;
