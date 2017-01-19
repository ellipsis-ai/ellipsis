# --- !Ups

CREATE INDEX invocation_log_entries_behavior_version_id_index ON invocation_log_entries(behavior_version_id);

# --- !Downs

DROP INDEX IF EXISTS invocation_log_entries_behavior_version_id_index;

