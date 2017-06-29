# --- !Ups

BEGIN;

CREATE TABLE data_type_configs(
  id TEXT PRIMARY KEY,
  uses_code BOOLEAN,
  behavior_version_id TEXT NOT NULL REFERENCES behavior_versions(id)
);

CREATE TABLE data_type_fields(
  id TEXT PRIMARY KEY,
  field_id TEXT NOT NULL,
  name TEXT NOT NULL,
  field_type TEXT NOT NULL,
  config_id TEXT NOT NULL REFERENCES data_type_configs(id),
  rank INT NOT NULL
);

CREATE TABLE default_storage_items(
  id TEXT PRIMARY KEY,
  behavior_id TEXT NOT NULL REFERENCES behaviors(id),
  data JSONB NOT NULL
);

CREATE INDEX default_storage_items_behavior_id_index ON default_storage_items(behavior_id);

COMMIT;

# --- !Downs

BEGIN;

DROP INDEX IF EXISTS default_storage_items_behavior_id_index;
DROP INDEX IF EXISTS default_storage_items_behavior_group_id_index;
DROP INDEX IF EXISTS default_storage_items_type_name_index;

DROP TABLE IF EXISTS default_storage_items;
DROP TABLE IF EXISTS data_type_fields;
DROP TABLE IF EXISTS data_type_configs;

COMMIT;
