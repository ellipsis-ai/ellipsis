# --- !Ups

ALTER TABLE users ADD COLUMN team_id TEXT REFERENCES teams(id);

UPDATE users
SET team_id = joined.team_id
FROM
  (SELECT u.id AS user_id, sbp.team_id AS team_id FROM users u
	  JOIN linked_accounts la ON la.user_id = u.id
	  JOIN slack_profiles sp ON sp.provider_id = la.provider_id AND sp.provider_key = la.provider_key
	  JOIN slack_bot_profiles sbp ON sbp.slack_team_id = sp.team_id) AS joined
WHERE users.id = joined.user_id;

ALTER TABLE users ALTER COLUMN team_id SET NOT NULL;

# --- !Downs

ALTER TABLE users DROP COLUMN team_id;
