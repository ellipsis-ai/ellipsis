# --- !Ups

CREATE TABLE recurrences (
  id TEXT PRIMARY KEY,
  recurrence_type TEXT NOT NULL,
  frequency INT NOT NULL,
  time_of_day TIME,
  minute_of_hour INT,
  day_of_week INT,
  day_of_month INT,
  nth_day_of_week INT,
  month INT,
  time_zone TEXT,
  monday BOOL,
  tuesday BOOL,
  wednesday BOOL,
  thursday BOOL,
  friday BOOL,
  saturday BOOL,
  sunday BOOL
);

DELETE FROM scheduled_messages;

ALTER TABLE scheduled_messages ADD COLUMN recurrence_id TEXT NOT NULL REFERENCES recurrences(id) ON DELETE CASCADE;
ALTER TABLE scheduled_messages DROP COLUMN recurrence_type;
ALTER TABLE scheduled_messages DROP COLUMN frequency;
ALTER TABLE scheduled_messages DROP COLUMN time_of_day;
ALTER TABLE scheduled_messages DROP COLUMN minute_of_hour;
ALTER TABLE scheduled_messages DROP COLUMN day_of_week;
ALTER TABLE scheduled_messages DROP COLUMN day_of_month;
ALTER TABLE scheduled_messages DROP COLUMN nth_day_of_week;
ALTER TABLE scheduled_messages DROP COLUMN month;
ALTER TABLE scheduled_messages DROP COLUMN time_zone;
ALTER TABLE scheduled_messages DROP COLUMN monday;
ALTER TABLE scheduled_messages DROP COLUMN tuesday;
ALTER TABLE scheduled_messages DROP COLUMN wednesday;
ALTER TABLE scheduled_messages DROP COLUMN thursday;
ALTER TABLE scheduled_messages DROP COLUMN friday;
ALTER TABLE scheduled_messages DROP COLUMN saturday;
ALTER TABLE scheduled_messages DROP COLUMN sunday;

CREATE TABLE scheduled_behaviors (
  id TEXT PRIMARY KEY,
  behavior_id TEXT NOT NULL REFERENCES behaviors(id),
  team_id TEXT NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
  channel_name TEXT,
  recurrence_id TEXT NOT NULL REFERENCES recurrences(id) ON DELETE CASCADE,
  next_sent_at TIMESTAMP NOT NULL,
  created_at TIMESTAMP NOT NULL
);

# --- !Downs

DROP TABLE IF EXISTS scheduled_behaviors;

DELETE FROM scheduled_messages;
ALTER TABLE scheduled_messages DROP COLUMN recurrence_id;
ALTER TABLE scheduled_messages ADD COLUMN recurrence_type TEXT NOT NULL;
ALTER TABLE scheduled_messages ADD COLUMN frequency INT NOT NULL;
ALTER TABLE scheduled_messages ADD COLUMN time_of_day TIME;
ALTER TABLE scheduled_messages ADD COLUMN minute_of_hour INT;
ALTER TABLE scheduled_messages ADD COLUMN day_of_week INT;
ALTER TABLE scheduled_messages ADD COLUMN day_of_month INT;
ALTER TABLE scheduled_messages ADD COLUMN nth_day_of_week INT;
ALTER TABLE scheduled_messages ADD COLUMN month INT;
ALTER TABLE scheduled_messages ADD COLUMN time_zone TEXT;
ALTER TABLE scheduled_messages ADD COLUMN monday BOOL;
ALTER TABLE scheduled_messages ADD COLUMN tuesday BOOL;
ALTER TABLE scheduled_messages ADD COLUMN wednesday BOOL;
ALTER TABLE scheduled_messages ADD COLUMN thursday BOOL;
ALTER TABLE scheduled_messages ADD COLUMN friday BOOL;
ALTER TABLE scheduled_messages ADD COLUMN saturday BOOL;
ALTER TABLE scheduled_messages ADD COLUMN sunday BOOL;

DROP TABLE IF EXISTS recurrences;
