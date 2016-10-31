# --- !Ups

CREATE INDEX invocation_log_entries_created_at_index ON invocation_log_entries(created_at);
CREATE INDEX invocation_log_entries_day_truncated_created_at_index ON invocation_log_entries(date_trunc('day', created_at));

# --- !Downs

DROP INDEX IF EXISTS invocation_log_entries_day_truncated_created_at_index;
DROP INDEX IF EXISTS invocation_log_entries_created_at_index;

