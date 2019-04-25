# --- !Ups

BEGIN;

CREATE TABLE slack_member_statuses (
  id TEXT PRIMARY KEY,
  slack_team_id TEXT NOT NULL,
  slack_user_id TEXT NOT NULL,
  is_deleted BOOLEAN NOT NULL,
  is_bot_or_app BOOLEAN NOT NULL,
  first_observed_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX slack_member_statuses_slack_team_id_index ON slack_member_statuses(slack_team_id);
CREATE INDEX slack_member_statuses_slack_user_id_index ON slack_member_statuses(slack_user_id);
CREATE INDEX slack_member_statuses_first_observed_at_index ON slack_member_statuses(first_observed_at);

COMMIT;

# --- !Downs

BEGIN;

DROP INDEX IF EXISTS slack_member_statuses_slack_team_id_index;
DROP INDEX IF EXISTS slack_member_statuses_slack_user_id_index;
DROP INDEX IF EXISTS slack_member_statuses_first_observed_at_index;

DROP TABLE IF EXISTS slack_member_statuses;

COMMIT;
