# --- !Ups

ALTER TABLE oauth2_applications ADD COLUMN custom_host TEXT;

# --- !Downs

ALTER TABLE oauth2_applications DROP COLUMN custom_host;
