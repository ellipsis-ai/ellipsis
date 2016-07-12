# --- !Ups

DROP TABLE IF EXISTS behavior_outputs;

DROP INDEX IF EXISTS next_triggered_at_index;

DROP TABLE IF EXISTS schedule_triggers;

# --- !Downs

CREATE TABLE schedule_triggers (
  id TEXT PRIMARY KEY,
  behavior_version_id TEXT NOT NULL REFERENCES behavior_versions(id) ON DELETE CASCADE,
  recurrence_type TEXT NOT NULL,
  frequency INT NOT NULL,
  time_of_day TIME,
  minute_of_hour INT,
  day_of_week INT,
  day_of_month INT,
  nth_day_of_week INT,
  month INT,
  next_triggered_at TIMESTAMP NOT NULL,
  created_at TIMESTAMP NOT NULL
);

CREATE INDEX next_triggered_at_index ON schedule_triggers(next_triggered_at);

CREATE TABLE behavior_outputs (
  behavior_version_id TEXT PRIMARY KEY REFERENCES behavior_versions(id) ON DELETE CASCADE,
  type TEXT NOT NULL,
  channel_name TEXT
);
