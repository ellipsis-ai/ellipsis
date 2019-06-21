# --- !Ups

ALTER TABLE oauth2_applications ADD COLUMN shared_token_user_id TEXT;

# --- !Downs

ALTER TABLE oauth2_applications DROP COLUMN shared_token_user_id;
