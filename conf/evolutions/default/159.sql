# --- !Ups

BEGIN;

CREATE INDEX conversations_context_index ON conversations(context);
CREATE INDEX conversations_user_id_for_context_index ON conversations(user_id_for_context);
CREATE INDEX conversations_thread_id_index ON conversations(thread_id);

COMMIT;

# --- !Downs

BEGIN;

DROP INDEX IF EXISTS conversations_context_index;
DROP INDEX IF EXISTS conversations_user_id_for_context_index;
DROP INDEX IF EXISTS conversations_thread_id_index;

COMMIT;


