# --- !Ups

BEGIN;

ALTER TABLE aws_configs ADD COLUMN group_version_id TEXT REFERENCES behavior_group_versions(id) ON DELETE CASCADE;

UPDATE aws_configs a_outer
SET group_version_id =
  (SELECT bv.group_version_id
  FROM aws_configs a_inner JOIN behavior_versions bv ON bv.id = a_inner.behavior_version_id
  WHERE a_inner.id = a_outer.id);

ALTER TABLE aws_configs ALTER COLUMN group_version_id SET NOT NULL;

ALTER TABLE aws_configs DROP COLUMN behavior_version_id;

COMMIT;

# --- !Downs

BEGIN;

DELETE FROM aws_configs;

ALTER TABLE aws_configs ADD COLUMN behavior_version_id TEXT NOT NULL REFERENCES behavior_versions(id);
ALTER TABLE aws_configs DROP COLUMN group_version_id;

COMMIT;
