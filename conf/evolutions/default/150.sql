# --- !Ups

CREATE TABLE forms (
  id TEXT PRIMARY KEY,
  config JSONB NOT NULL,
  created_at TIMESTAMPTZ NOT NULL
);

# --- !Downs

DROP TABLE IF EXISTS forms;
