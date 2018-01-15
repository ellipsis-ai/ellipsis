# --- !Ups

CREATE TABLE dev_mode_channels (
  context TEXT NOT NULL,
  channel TEXT NOT NULL,
  team_id TEXT NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ NOT NULL,
  PRIMARY KEY(context, channel, team_id)
);

# --- !Downs

DROP TABLE dev_mode_channels;
