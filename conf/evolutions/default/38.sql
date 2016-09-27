# --- !Ups

CREATE TABLE behavior_backed_data_types (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  behavior_id TEXT NOT NULL REFERENCES behaviors(id)
);

# --- !Downs

DROP TABLE IF EXISTS behavior_backed_data_types;
