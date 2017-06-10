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
  behavior_group_id TEXT NOT NULL REFERENCES behavior_groups(id),
  data JSONB NOT NULL
);

# --- !Downs

DROP TABLE IF EXISTS default_storage_items;
DROP TABLE IF EXISTS data_type_fields;
DROP TABLE IF EXISTS data_type_configs;
