# --- !Ups

ALTER TABLE behavior_version_user_involvements
DROP CONSTRAINT behavior_version_user_involvements_behavior_version_id_fkey,
ADD CONSTRAINT behavior_version_user_involvements_behavior_version_id_fkey
  FOREIGN KEY(behavior_version_id)
  REFERENCES behavior_versions(id)
  ON DELETE CASCADE;

# --- !Downs

ALTER TABLE behavior_version_user_involvements
DROP CONSTRAINT behavior_version_user_involvements_behavior_version_id_fkey,
ADD CONSTRAINT behavior_version_user_involvements_behavior_version_id_fkey
  FOREIGN KEY(behavior_version_id)
  REFERENCES behavior_versions(id)
  ON DELETE NO ACTION;
