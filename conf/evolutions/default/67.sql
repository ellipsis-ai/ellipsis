# --- !Ups

TRUNCATE TABLE invocation_tokens;
ALTER TABLE invocation_tokens
DROP CONSTRAINT invocation_tokens_behavior_id_fkey,
ADD CONSTRAINT invocation_tokens_behavior_id_fkey
  FOREIGN KEY(behavior_id)
  REFERENCES behaviors(id)
  ON DELETE CASCADE;

# --- !Downs

TRUNCATE TABLE invocation_tokens;
ALTER TABLE invocation_tokens
DROP CONSTRAINT invocation_tokens_behavior_id_fkey,
ADD CONSTRAINT invocation_tokens_behavior_id_fkey
  FOREIGN KEY(behavior_id)
  REFERENCES behaviors(id)
  ON DELETE SET NULL;
