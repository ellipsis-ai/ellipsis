# --- !Ups

ALTER TABLE invocation_tokens
DROP CONSTRAINT invocation_tokens_behavior_version_id_fkey,
ADD CONSTRAINT invocation_tokens_behavior_version_id_fkey
  FOREIGN KEY(behavior_version_id)
  REFERENCES behavior_versions(id)
  ON DELETE CASCADE;

# --- !Downs

ALTER TABLE invocation_tokens
DROP CONSTRAINT invocation_tokens_behavior_version_id_fkey,
ADD CONSTRAINT invocation_tokens_behavior_version_id_fkey
  FOREIGN KEY(behavior_version_id)
  REFERENCES behavior_versions(id)
  ON DELETE NO ACTION;
