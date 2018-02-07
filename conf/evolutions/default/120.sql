# --- !Ups

CREATE TABLE logged_events (
  id TEXT PRIMARY KEY,
  type TEXT NOT NULL,
  user_id TEXT REFERENCES users(id),
  medium TEXT,
  channel TEXT,
  details JSONB NOT NULL,
  created_at TIMESTAMPTZ NOT NULL
);

# --- !Downs

DROP TABLE IF EXISTS logged_events;
