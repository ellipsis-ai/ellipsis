# --- !Ups

ALTER TABLE custom_oauth2_configuration_templates RENAME TO oauth2_apis;
ALTER TABLE custom_oauth2_configurations RENAME TO oauth2_applications;
ALTER TABLE linked_oauth_2_tokens RENAME TO linked_oauth2_tokens;
ALTER TABLE oauth2_applications RENAME COLUMN template_id TO api_id;


# --- !Downs

ALTER TABLE oauth2_applications RENAME COLUMN api_id TO template_id;
ALTER TABLE oauth2_apis RENAME TO custom_oauth2_configuration_templates;
ALTER TABLE oauth2_applications RENAME TO custom_oauth2_configurations;
ALTER TABLE linked_oauth2_tokens RENAME TO linked_oauth_2_tokens;
