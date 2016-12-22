# --- !Ups


DROP INDEX IF EXISTS invocation_log_entries_day_truncated_created_at_index;
-- CREATE INDEX invocation_log_entries_day_truncated_created_at_index ON invocation_log_entries(date_trunc('day', created_at AT TIME ZONE('UTC')));

ALTER TABLE api_tokens ALTER created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC';
ALTER TABLE api_tokens ALTER last_used TYPE timestamptz USING last_used AT TIME ZONE 'UTC';
ALTER TABLE behavior_groups ALTER created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC';
ALTER TABLE behavior_versions ALTER created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC';
ALTER TABLE behaviors ALTER created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC';
ALTER TABLE conversations ALTER started_at TYPE timestamptz USING started_at AT TIME ZONE 'UTC';
ALTER TABLE environment_variables ALTER created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC';
ALTER TABLE invocation_log_entries ALTER created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC';
ALTER TABLE invocation_tokens ALTER created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC';
ALTER TABLE linked_accounts ALTER created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC';
ALTER TABLE linked_oauth2_tokens ALTER expiration_time TYPE timestamptz USING expiration_time AT TIME ZONE 'UTC';
ALTER TABLE login_tokens ALTER created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC';
ALTER TABLE oauth_2_tokens ALTER expiration_time TYPE timestamptz USING expiration_time AT TIME ZONE 'UTC';
ALTER TABLE saved_answers ALTER created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC';
ALTER TABLE scheduled_messages ALTER created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC';
ALTER TABLE scheduled_messages ALTER next_sent_at TYPE timestamptz USING next_sent_at AT TIME ZONE 'UTC';
ALTER TABLE slack_bot_profiles ALTER created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC';
ALTER TABLE user_environment_variables ALTER created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC';

# --- !Downs

ALTER TABLE api_tokens ALTER created_at TYPE timestamp;
ALTER TABLE api_tokens ALTER last_used TYPE timestamp;
ALTER TABLE behavior_groups ALTER created_at TYPE timestamp;
ALTER TABLE behavior_versions ALTER created_at TYPE timestamp;
ALTER TABLE behaviors ALTER created_at TYPE timestamp;
ALTER TABLE conversations ALTER started_at TYPE timestamp;
ALTER TABLE environment_variables ALTER created_at TYPE timestamp;
ALTER TABLE invocation_log_entries ALTER created_at TYPE timestamp;
ALTER TABLE invocation_tokens ALTER created_at TYPE timestamp;
ALTER TABLE linked_accounts ALTER created_at TYPE timestamp;
ALTER TABLE linked_oauth2_tokens ALTER expiration_time TYPE timestamp;
ALTER TABLE login_tokens ALTER created_at TYPE timestamp;
ALTER TABLE oauth_2_tokens ALTER expiration_time TYPE timestamp;
ALTER TABLE saved_answers ALTER created_at TYPE timestamp;
ALTER TABLE scheduled_messages ALTER created_at TYPE timestamp;
ALTER TABLE scheduled_messages ALTER next_sent_at TYPE timestamp;
ALTER TABLE slack_bot_profiles ALTER created_at TYPE timestamp;
ALTER TABLE user_environment_variables ALTER created_at TYPE timestamp;
