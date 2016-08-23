# --- !Ups

ALTER TABLE oauth2_apis ADD COLUMN new_application_url TEXT;
ALTER TABLE oauth2_apis ADD COLUMN scope_documentation_url TEXT;

# --- !Downs

ALTER TABLE oauth2_apis DROP COLUMN scope_documentation_url;
ALTER TABLE oauth2_apis DROP COLUMN new_application_url;
