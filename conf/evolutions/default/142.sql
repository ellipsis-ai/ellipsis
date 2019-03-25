# --- !Ups

BEGIN;

ALTER TABLE invocation_tokens ADD COLUMN team_id_for_context TEXT;

CREATE INDEX invocation_tokens_team_id_for_context_index ON invocation_tokens(team_id_for_context);

COMMIT;

# --- !Downs

BEGIN;

DROP INDEX IF EXISTS invocation_tokens_team_id_for_context_index;

ALTER TABLE invocation_tokens DROP COLUMN team_id_for_context;

COMMIT;
