# --- !Ups

ALTER TABLE invocation_log_entries ALTER runtime_in_milliseconds SET NOT NULL;

# --- !Downs

ALTER TABLE invocation_log_entries ALTER runtime_in_milliseconds DROP NOT NULL;
