# --- !Ups

BEGIN;

TRUNCATE TABLE invocation_tokens;

ALTER TABLE invocation_tokens DROP COLUMN behavior_id;

ALTER TABLE invocation_tokens ADD COLUMN behavior_version_id TEXT NOT NULL REFERENCES behavior_versions(id);

COMMIT;

# --- !Downs

BEGIN;

TRUNCATE TABLE invocation_tokens;

ALTER TABLE invocation_tokens DROP COLUMN behavior_version_id;

ALTER TABLE invocation_tokens ADD COLUMN behavior_id TEXT NOT NULL REFERENCES behaviors(id);

COMMIT;
