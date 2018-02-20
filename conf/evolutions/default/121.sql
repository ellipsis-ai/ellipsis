# --- !Ups

BEGIN;

ALTER TABLE behavior_group_version_shas
DROP CONSTRAINT behavior_group_version_shas_group_version_id_fkey,
ADD CONSTRAINT behavior_group_version_shas_group_version_id_fkey
  FOREIGN KEY(group_version_id)
  REFERENCES behavior_group_versions(id)
  ON DELETE CASCADE;

ALTER TABLE behavior_group_deployments
DROP CONSTRAINT behavior_group_deployments_group_version_id_fkey,
ADD CONSTRAINT behavior_group_deployments_group_version_id_fkey
  FOREIGN KEY(group_version_id)
  REFERENCES behavior_group_versions(id)
  ON DELETE NO ACTION;

COMMIT;

# --- !Downs

BEGIN;

ALTER TABLE behavior_group_version_shas
DROP CONSTRAINT behavior_group_version_shas_group_version_id_fkey,
ADD CONSTRAINT behavior_group_version_shas_group_version_id_fkey
  FOREIGN KEY(group_version_id)
  REFERENCES behavior_group_versions(id)
  ON DELETE NO ACTION;

COMMIT;
