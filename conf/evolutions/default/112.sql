# --- !Ups

CREATE TABLE linked_github_repos (
  owner TEXT NOT NULL,
  repo TEXT NOT NULL,
  group_id TEXT NOT NULL REFERENCES behavior_groups ON DELETE CASCADE,
  created_at TIMESTAMPTZ NOT NULL,
  UNIQUE(group_id),
  PRIMARY KEY(owner, repo, group_id)
);

# --- !Downs

DROP TABLE linked_github_repos;
