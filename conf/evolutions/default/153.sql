# --- !Ups

BEGIN;

CREATE TABLE oauth1_token_shares (
  application_id TEXT NOT NULL REFERENCES oauth1_applications(id),
  user_id TEXT NOT NULL REFERENCES users(id),
  team_id TEXT NOT NULL REFERENCES teams(id),
  PRIMARY KEY (application_id, team_id)
);

CREATE INDEX oauth1_token_shares_application_id_index ON oauth1_applications(id);
CREATE INDEX oauth1_token_shares_user_id_index ON users(id);
CREATE INDEX oauth1_token_shares_team_id_index ON teams(id);

CREATE TABLE oauth2_token_shares (
  application_id TEXT NOT NULL REFERENCES oauth2_applications(id),
  user_id TEXT NOT NULL REFERENCES users(id),
  team_id TEXT NOT NULL REFERENCES teams(id),
  PRIMARY KEY (application_id, team_id)
);

CREATE INDEX oauth2_token_shares_application_id_index ON oauth2_applications(id);
CREATE INDEX oauth2_token_shares_user_id_index ON users(id);
CREATE INDEX oauth2_token_shares_team_id_index ON teams(id);

COMMIT;

# --- !Downs

BEGIN;

DROP INDEX IF EXISTS oauth1_token_shares_application_id_index;
DROP INDEX IF EXISTS oauth1_token_shares_user_id_index;
DROP INDEX IF EXISTS oauth1_token_shares_team_id_index;

DROP TABLE IF EXISTS oauth1_token_shares;

DROP INDEX IF EXISTS oauth2_token_shares_application_id_index;
DROP INDEX IF EXISTS oauth2_token_shares_user_id_index;
DROP INDEX IF EXISTS oauth2_token_shares_team_id_index;

DROP TABLE IF EXISTS oauth2_token_shares;

COMMIT;
