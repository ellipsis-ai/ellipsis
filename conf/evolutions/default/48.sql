# --- !Ups

CREATE TABLE inputs (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  question TEXT,
  param_type TEXT NOT NULL
);

ALTER TABLE behavior_parameters ADD COLUMN input_id TEXT REFERENCES inputs(id);

# --- !Downs

ALTER TABLE behavior_parameters DROP COLUMN input_id;

DROP TABLE IF EXISTS inputs;
