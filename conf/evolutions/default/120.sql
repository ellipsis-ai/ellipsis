# --- !Ups

CREATE TABLE logged_events (
  id TEXT PRIMARY KEY,
  cause_type TEXT NOT NULL,
  cause_details JSONB NOT NULL,
  result_type TEXT NOT NULL,
  result_details JSONB NOT NULL,
  user_id TEXT REFERENCES users(id),
  medium TEXT,
  channel TEXT,
  channel_details JSONB,
  created_at TIMESTAMPTZ NOT NULL
);

# --- !Downs

DROP TABLE IF EXISTS logged_events;
