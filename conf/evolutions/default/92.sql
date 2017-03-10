# --- !Ups

BEGIN;

ALTER TABLE inputs ADD COLUMN group_version_id TEXT REFERENCES behavior_group_versions(id);

DELETE FROM behavior_group_versions
WHERE id NOT IN (SELECT current_version_id FROM behavior_groups);

DELETE FROM inputs WHERE group_id IS NULL;

DELETE FROM inputs AS i WHERE i.id NOT IN (
  SELECT bp.input_id FROM behavior_parameters AS bp WHERE bp.input_id IS NOT NULL
);

UPDATE inputs AS i SET group_version_id = (
  SELECT bg.current_version_id FROM behavior_groups AS bg
  WHERE bg.id = i.group_id
);

ALTER TABLE inputs ALTER COLUMN group_version_id SET NOT NULL;

ALTER TABLE inputs DROP COLUMN group_id;

COMMIT;

# --- !Downs

BEGIN;

ALTER TABLE inputs ADD COLUMN group_id TEXT REFERENCES behavior_groups(id);

ALTER TABLE inputs DROP COLUMN group_version_id;

COMMIT;
