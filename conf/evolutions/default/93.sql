# --- !Ups

ALTER TABLE inputs
DROP CONSTRAINT inputs_group_version_id_fkey,
ADD CONSTRAINT inputs_group_version_id_fkey
  FOREIGN KEY(group_version_id)
  REFERENCES behavior_group_versions(id)
  ON DELETE CASCADE;

# --- !Downs

ALTER TABLE inputs
DROP CONSTRAINT inputs_group_version_id_fkey,
ADD CONSTRAINT inputs_group_version_id_fkey
  FOREIGN KEY(group_version_id)
  REFERENCES behavior_group_versions(id)
  ON DELETE NO ACTION;
