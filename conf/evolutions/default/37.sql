# --- !Ups

UPDATE behavior_parameters SET param_type = 'Text';
ALTER TABLE behavior_parameters ALTER COLUMN param_type SET DEFAULT 'Text';
ALTER TABLE behavior_parameters ALTER COLUMN param_type SET NOT NULL;

# --- !Downs

ALTER TABLE behavior_parameters ALTER COLUMN param_type DROP NOT NULL;
ALTER TABLE behavior_parameters ALTER COLUMN param_type DROP DEFAULT;
