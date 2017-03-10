# --- !Ups

BEGIN;

ALTER TABLE linked_accounts
DROP CONSTRAINT linked_accounts_user_id_fkey,
ADD CONSTRAINT linked_accounts_user_id_fkey
  FOREIGN KEY(user_id)
  REFERENCES users(id)
  ON DELETE CASCADE;

ALTER TABLE invocation_tokens
DROP CONSTRAINT invocation_tokens_user_id_fkey,
ADD CONSTRAINT invocation_tokens_user_id_fkey
  FOREIGN KEY(user_id)
  REFERENCES users(id)
  ON DELETE CASCADE;

ALTER TABLE invocation_log_entries
DROP CONSTRAINT invocation_log_entries_user_id_fkey,
ADD CONSTRAINT invocation_log_entries_user_id_fkey
  FOREIGN KEY(user_id)
  REFERENCES users(id)
  ON DELETE CASCADE;

COMMIT;

# --- !Downs

BEGIN;

ALTER TABLE linked_accounts
DROP CONSTRAINT linked_accounts_user_id_fkey,
ADD CONSTRAINT linked_accounts_user_id_fkey
  FOREIGN KEY(user_id)
  REFERENCES users(id)
  ON DELETE NO ACTION;

ALTER TABLE invocation_tokens
DROP CONSTRAINT invocation_tokens_user_id_fkey,
ADD CONSTRAINT invocation_tokens_user_id_fkey
  FOREIGN KEY(user_id)
  REFERENCES users(id)
  ON DELETE NO ACTION;

ALTER TABLE invocation_log_entries
DROP CONSTRAINT invocation_log_entries_user_id_fkey,
ADD CONSTRAINT invocation_log_entries_user_id_fkey
  FOREIGN KEY(user_id)
  REFERENCES users(id)
  ON DELETE NO ACTION;

COMMIT;
