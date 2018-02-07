# --- !Ups

CREATE TABLE active_user_records (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL REFERENCES users(id),
  created_at TIMESTAMPTZ NOT NULL
);


# --- !Downs

DROP TABLE IF EXISTS active_user_records;
