# --- !Ups

BEGIN;

DELETE FROM slack_profiles;

ALTER TABLE slack_profiles
ADD CONSTRAINT slack_profiles_provider_id_provider_key_key UNIQUE (provider_id, provider_key);

COMMIT;

# --- !Downs

ALTER TABLE slack_profiles
DROP CONSTRAINT slack_profiles_provider_id_provider_key_key;
