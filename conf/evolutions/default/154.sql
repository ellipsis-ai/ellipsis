# --- !Ups

BEGIN;

DELETE FROM invocation_log_entries WHERE runtime_in_milliseconds IS NULL;
ALTER TABLE invocation_log_entries ALTER runtime_in_milliseconds SET NOT NULL;

COMMIT;

# --- !Downs

ALTER TABLE invocation_log_entries ALTER runtime_in_milliseconds DROP NOT NULL;
