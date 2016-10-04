# --- !Ups

ALTER TABLE oauth2_apis ADD COLUMN grant_type TEXT NOT NULL DEFAULT 'AuthorizationCode';
ALTER TABLE oauth2_apis ALTER COLUMN authorization_url DROP NOT NULL;

# --- !Downs

ALTER TABLE oauth2_apis ALTER COLUMN authorization_url SET NOT NULL;
ALTER TABLE oauth2_apis DROP COLUMN grant_type;
