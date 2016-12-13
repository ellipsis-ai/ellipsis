# --- !Ups

ALTER TABLE invocation_log_entries ADD COLUMN param_values JSONB;

# --- !Downs

ALTER TABLE invocation_log_entries DROP COLUMN param_values;
