# --- !Ups

CREATE TABLE active_user_records (
  id TEXT PRIMARY KEY,
  team_id TEXT NOT NULL,
  user_id TEXT NULL,
  external_user_id TEXT NULL,
  derived_user_id TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL
);

ALTER TABLE active_user_records ADD CONSTRAINT chk_active_user_records CHECK (user_id is not null or external_user_id is not null);

# --- !Downs

DROP TABLE IF EXISTS active_user_records;

