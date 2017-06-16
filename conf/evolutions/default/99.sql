# --- !Ups

CREATE TABLE data_type_configs(
  id TEXT PRIMARY KEY,
  behavior_version_id TEXT NOT NULL REFERENCES behavior_versions(id)
);

CREATE TABLE data_type_fields(
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  field_type TEXT NOT NULL,
  config_id TEXT NOT NULL REFERENCES data_type_configs(id)
);

CREATE TABLE default_storage_items(
  id TEXT PRIMARY KEY,
  type_name TEXT NOT NULL,
  behavior_group_id TEXT NOT NULL REFERENCES behavior_groups(id),
  data JSONB NOT NULL
);

CREATE INDEX default_storage_items_type_name_index ON default_storage_items(type_name);
CREATE INDEX default_storage_items_behavior_group_id_index ON default_storage_items(behavior_group_id);


# --- !Downs

DROP INDEX IF EXISTS default_storage_items_behavior_group_id_index;
DROP INDEX IF EXISTS default_storage_items_type_name_index;

DROP TABLE IF EXISTS default_storage_items;
DROP TABLE IF EXISTS data_type_fields;
DROP TABLE IF EXISTS data_type_configs;
