# --- !Ups

CREATE TABLE scheduled_messages (
  id TEXT PRIMARY KEY,
  text TEXT NOT NULL,
  team_id TEXT NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
  channel_name TEXT,
  recurrence_type TEXT NOT NULL,
  frequency INT NOT NULL,
  time_of_day TIME,
  minute_of_hour INT,
  day_of_week INT,
  day_of_month INT,
  nth_day_of_week INT,
  month INT,
  next_sent_at TIMESTAMP NOT NULL,
  created_at TIMESTAMP NOT NULL
);

CREATE INDEX next_sent_at_index ON scheduled_messages(next_sent_at);

# --- !Downs

DROP INDEX IF EXISTS next_sent_at_index;

DROP TABLE IF EXISTS scheduled_messages;
