# --- !Ups

ALTER TABLE required_oauth2_api_configs ADD COLUMN name_in_code TEXT NOT NULL DEFAULT 'default';

# --- !Downs

ALTER TABLE required_oauth2_api_configs DROP COLUMN name_in_code;
