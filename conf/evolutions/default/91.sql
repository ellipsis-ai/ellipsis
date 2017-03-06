# --- !Ups

ALTER TABLE behavior_group_versions
DROP CONSTRAINT behavior_group_versions_group_id_fkey,
ADD CONSTRAINT behavior_group_versions_group_id_fkey
  FOREIGN KEY(group_id)
  REFERENCES behavior_groups(id)
  ON DELETE CASCADE;

ALTER TABLE behavior_versions
DROP CONSTRAINT behavior_versions_group_version_id_fkey,
ADD CONSTRAINT behavior_versions_group_version_id_fkey
  FOREIGN KEY(group_version_id)
  REFERENCES behavior_group_versions(id)
  ON DELETE CASCADE;

# --- !Downs

ALTER TABLE behavior_group_versions
DROP CONSTRAINT behavior_group_versions_group_id_fkey,
ADD CONSTRAINT behavior_group_versions_group_id_fkey
  FOREIGN KEY(group_id)
  REFERENCES behavior_groups(id)
  ON DELETE NO ACTION;


ALTER TABLE behavior_versions
DROP CONSTRAINT behavior_versions_group_version_id_fkey,
ADD CONSTRAINT behavior_versions_group_version_id_fkey
  FOREIGN KEY(group_version_id)
  REFERENCES behavior_group_versions(id)
  ON DELETE NO ACTION;
