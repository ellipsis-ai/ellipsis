# --- !Ups

BEGIN;

ALTER TABLE behavior_parameters
DROP CONSTRAINT IF EXISTS behavior_parameters_input_id_fkey;

UPDATE behavior_parameters AS bp SET input_id = (
  SELECT i.input_id FROM inputs i WHERE i.id = bp.input_id LIMIT 1
);

COMMIT;

# --- !Downs

BEGIN;

UPDATE behavior_parameters AS bp SET input_id = (
  SELECT i.id FROM inputs i JOIN behavior_versions bv ON bv.group_version_id = i.group_version_id
  WHERE i.input_id = bp.input_id AND bp.behavior_version_id = bv.id
  LIMIT 1
);

ALTER TABLE behavior_parameters
ADD CONSTRAINT behavior_parameters_input_id_fkey FOREIGN KEY (input_id)
REFERENCES inputs(id);

COMMIT;
