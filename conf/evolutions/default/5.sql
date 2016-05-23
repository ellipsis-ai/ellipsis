# --- !Ups

CREATE TABLE environment_variables (
  name TEXT NOT NULL,
  value TEXT NOT NULL,
  team_id TEXT NOT NULL REFERENCES teams(id),
  created_at TIMESTAMP NOT NULL,
  PRIMARY KEY(name, team_id)
);

# --- !Downs

DROP TABLE IF EXISTS environment_variables;
