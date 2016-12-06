# --- !Ups

ALTER TABLE required_oauth2_api_configs
DROP CONSTRAINT required_oauth2_applications_application_id_fkey,
ADD CONSTRAINT required_oauth2_applications_application_id_fkey
  FOREIGN KEY(application_id)
  REFERENCES oauth2_applications(id)
  ON DELETE SET NULL;

# --- !Downs

ALTER TABLE required_oauth2_api_configs
DROP CONSTRAINT required_oauth2_applications_application_id_fkey,
ADD CONSTRAINT required_oauth2_applications_application_id_fkey
  FOREIGN KEY(application_id)
  REFERENCES oauth2_applications(id)
  ON DELETE CASCADE ;
