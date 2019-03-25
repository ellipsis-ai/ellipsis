# --- !Ups

BEGIN;

ALTER TABLE behavior_versions ADD COLUMN response_type TEXT NOT NULL DEFAULT 'Normal';
UPDATE behavior_versions SET response_type = 'Private' WHERE private_response = TRUE;

COMMIT;

# --- !Downs

ALTER TABLE behavior_versions DROP COLUMN response_type;
