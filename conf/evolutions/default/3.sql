# --- !Ups

CREATE TABLE link_shortcuts (
  label TEXT PRIMARY KEY,
  link TEXT NOT NULL,
  team_id TEXT NOT NULL
);

# --- !Downs

DROP TABLE IF EXISTS link_shortcuts;
