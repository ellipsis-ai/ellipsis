# --- !Ups

ALTER TABLE oauth2_apis ADD COLUMN audience TEXT;

# --- !Downs

ALTER TABLE oauth2_apis DROP COLUMN audience;
