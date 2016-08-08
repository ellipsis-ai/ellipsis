# --- !Ups

ALTER TABLE custom_oauth2_configuration_templates DROP COLUMN get_profile_url;
ALTER TABLE custom_oauth2_configuration_templates DROP COLUMN get_profile_json_path;

# --- !Downs

ALTER TABLE custom_oauth2_configuration_templates ADD COLUMN get_profile_json_path TEXT NOT NULL;
ALTER TABLE custom_oauth2_configuration_templates ADD COLUMN get_profile_url TEXT NOT NULL;
