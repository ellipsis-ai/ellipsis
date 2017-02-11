# --- !Ups

ALTER TABLE behavior_parameters DROP COLUMN name;
ALTER TABLE behavior_parameters DROP COLUMN question;
ALTER TABLE behavior_parameters DROP COLUMN param_type;

# --- !Downs

ALTER TABLE behavior_parameters ADD COLUMN name TEXT;
ALTER TABLE behavior_parameters ADD COLUMN question TEXT;
ALTER TABLE behavior_parameters ADD COLUMN param_type TEXT;

UPDATE behavior_parameters as bp SET name = (SELECT name FROM inputs as i WHERE i.id = bp.input_id);
UPDATE behavior_parameters as bp SET question = (SELECT question FROM inputs as i WHERE i.id = bp.input_id);
UPDATE behavior_parameters as bp SET param_type = (SELECT param_type FROM inputs as i WHERE i.id = bp.input_id);

ALTER TABLE behavior_parameters ALTER COLUMN name SET NOT NULL;
ALTER TABLE behavior_parameters ALTER COLUMN param_type SET NOT NULL;
