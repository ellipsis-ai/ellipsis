# --- !Ups

BEGIN;

CREATE INDEX linked_simple_tokens_user_id_index ON linked_simple_tokens(user_id);

COMMIT;

# --- !Downs

BEGIN;

DROP INDEX IF EXISTS linked_simple_tokens_user_id_index;

COMMIT;


