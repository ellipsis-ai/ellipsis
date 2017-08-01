# --- !Ups

ALTER TABLE data_type_fields ADD COLUMN is_label BOOLEAN NOT NULL DEFAULT FALSE;

# --- !Downs

ALTER TABLE data_type_fields DROP COLUMN is_label;
