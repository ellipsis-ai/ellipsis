# --- !Ups

ALTER TABLE oauth1_applications ADD COLUMN shared_token_user_id TEXT;

# --- !Downs

ALTER TABLE oauth1_applications DROP COLUMN shared_token_user_id;
