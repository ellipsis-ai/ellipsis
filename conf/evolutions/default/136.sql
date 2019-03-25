# --- !Ups

BEGIN;

ALTER TABLE recurrences ADD COLUMN times_has_run INTEGER NOT NULL DEFAULT 0;
ALTER TABLE recurrences ADD COLUMN total_times_to_run INTEGER DEFAULT NULL;

COMMIT;

# --- !Downs

BEGIN;

ALTER TABLE recurrences DROP COLUMN times_has_run;
ALTER TABLE recurrences DROP COLUMN total_times_to_run;

COMMIT;
