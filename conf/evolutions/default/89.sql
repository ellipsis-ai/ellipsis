# --- !Ups

BEGIN;

ALTER TABLE behavior_groups ADD COLUMN current_version_id TEXT REFERENCES behavior_group_versions(id);

UPDATE behavior_groups AS bg SET current_version_id = (
  SELECT bgv.id FROM behavior_group_versions AS bgv
  WHERE bgv.group_id = bg.id
  LIMIT 1
);

COMMIT;

# --- !Downs

ALTER TABLE behavior_groups DROP COLUMN current_version_id;
