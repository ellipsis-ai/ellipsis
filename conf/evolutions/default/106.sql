# --- !Ups

ALTER TABLE required_oauth2_api_configs ADD COLUMN name_in_code TEXT NOT NULL DEFAULT 'default';
ALTER TABLE required_simple_token_apis ADD COLUMN name_in_code TEXT NOT NULL DEFAULT 'default';

# --- !Downs

ALTER TABLE required_oauth2_api_configs DROP COLUMN name_in_code;
ALTER TABLE required_simple_token_apis DROP COLUMN name_in_code;
