# --- !Ups

BEGIN;

ALTER TABLE data_type_configs
DROP CONSTRAINT data_type_configs_behavior_version_id_fkey,
ADD CONSTRAINT data_type_configs_behavior_version_id_fkey
   FOREIGN KEY (behavior_version_id)
   REFERENCES behavior_versions(id)
   ON DELETE CASCADE;

ALTER TABLE data_type_fields
DROP CONSTRAINT data_type_fields_config_id_fkey,
ADD CONSTRAINT data_type_fields_config_id_fkey
   FOREIGN KEY (config_id)
   REFERENCES data_type_configs(id)
   ON DELETE CASCADE;

ALTER TABLE default_storage_items
DROP CONSTRAINT default_storage_items_behavior_id_fkey,
ADD CONSTRAINT default_storage_items_behavior_id_fkey
   FOREIGN KEY (behavior_id)
   REFERENCES behaviors(id)
   ON DELETE CASCADE;

COMMIT;

# --- !Downs

BEGIN;

ALTER TABLE data_type_configs
DROP CONSTRAINT data_type_configs_behavior_version_id_fkey,
ADD CONSTRAINT data_type_configs_behavior_version_id_fkey
   FOREIGN KEY (behavior_version_id)
   REFERENCES behavior_versions(id)
   ON DELETE NO ACTION;

ALTER TABLE data_type_fields
DROP CONSTRAINT data_type_fields_config_id_fkey,
ADD CONSTRAINT data_type_fields_config_id_fkey
   FOREIGN KEY (config_id)
   REFERENCES data_type_configs(id)
   ON DELETE NO ACTION;

ALTER TABLE default_storage_items
DROP CONSTRAINT default_storage_items_behavior_id_fkey,
ADD CONSTRAINT default_storage_items_behavior_id_fkey
   FOREIGN KEY (behavior_id)
   REFERENCES behaviors(id)
   ON DELETE NO ACTION;

COMMIT;
