# --- !Ups

ALTER TABLE behaviors ADD COLUMN data_type_name TEXT;

UPDATE behaviors
SET data_type_name = data_types.name
FROM
  (SELECT id, name, behavior_id
   FROM behavior_backed_data_types) AS data_types
WHERE behaviors.id = data_types.behavior_id;

# --- !Downs

ALTER TABLE behaviors DROP COLUMN data_type_name;
