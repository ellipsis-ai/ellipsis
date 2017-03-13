# --- !Ups

BEGIN;

ALTER TABLE behaviors ADD COLUMN is_data_type BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE behaviors SET is_data_type = TRUE
WHERE data_type_name IS NOT NULL;

UPDATE behavior_versions AS bv
SET name = b.data_type_name
FROM behaviors AS b
WHERE bv.behavior_id = b.id AND b.is_data_type IS TRUE;

ALTER TABLE behaviors DROP COLUMN data_type_name;

COMMIT;

# --- !Downs

BEGIN;

ALTER TABLE behaviors ADD COLUMN data_type_name TEXT;

UPDATE behaviors AS b
SET data_type_name = bv.name
FROM behavior_versions AS bv
WHERE bv.behavior_id = b.id
  AND bv.id = (SELECT id FROM behavior_versions WHERE behavior_id = b.id ORDER BY created_at DESC LIMIT 1)
  AND b.is_data_type IS TRUE;

ALTER TABLE behaviors DROP COLUMN is_data_type;

COMMIT;
