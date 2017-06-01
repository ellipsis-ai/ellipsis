# --- !Ups

ALTER TABLE oauth2_applications ADD COLUMN is_shared BOOLEAN NOT NULL DEFAULT FALSE;

# --- !Downs

ALTER TABLE oauth2_applications DROP COLUMN is_shared;
