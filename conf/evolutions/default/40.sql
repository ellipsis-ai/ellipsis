# --- !Ups

ALTER TABLE behavior_versions ADD COLUMN private_response BOOLEAN NOT NULL DEFAULT FALSE;

# --- !Downs

ALTER TABLE behavior_versions DROP COLUMN private_response;
