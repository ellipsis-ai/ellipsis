# --- !Ups

ALTER TABLE behavior_test_results
DROP CONSTRAINT behavior_test_results_behavior_version_id_fkey,
ADD CONSTRAINT behavior_test_results_behavior_version_id_fkey
  FOREIGN KEY(behavior_version_id)
  REFERENCES behavior_versions(id)
  ON DELETE CASCADE;

ALTER TABLE managed_behavior_groups
DROP CONSTRAINT managed_behavior_groups_group_id_fkey,
ADD CONSTRAINT managed_behavior_groups_group_id_fkey
  FOREIGN KEY(group_id)
  REFERENCES behavior_groups(id)
  ON DELETE CASCADE;

# --- !Downs

ALTER TABLE behavior_test_results
DROP CONSTRAINT behavior_test_results_behavior_version_id_fkey,
ADD CONSTRAINT behavior_test_results_behavior_version_id_fkey
  FOREIGN KEY(behavior_version_id)
  REFERENCES behavior_versions(id)
  ON DELETE NO ACTION;

ALTER TABLE managed_behavior_groups
DROP CONSTRAINT managed_behavior_groups_group_id_fkey,
ADD CONSTRAINT managed_behavior_groups_group_id_fkey
  FOREIGN KEY(group_id)
  REFERENCES behavior_groups(id)
  ON DELETE NO ACTION;
