# --- !Ups

ALTER TABLE behavior_parameters DROP COLUMN name;
ALTER TABLE behavior_parameters DROP COLUMN question;
ALTER TABLE behavior_parameters DROP COLUMN param_type;

# --- !Downs

ALTER TABLE behavior_parameters ADD COLUMN name TEXT;
ALTER TABLE behavior_parameters ADD COLUMN question TEXT;
ALTER TABLE behavior_parameters ADD COLUMN param_type TEXT;
