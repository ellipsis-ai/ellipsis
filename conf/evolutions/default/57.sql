# --- !Ups

ALTER TABLE linked_oauth2_tokens
DROP CONSTRAINT linked_oauth_2_tokens_config_id_fkey,
ADD CONSTRAINT linked_oauth_2_tokens_config_id_fkey
  FOREIGN KEY(config_id)
  REFERENCES oauth2_applications(id)
  ON DELETE CASCADE;

# --- !Downs

ALTER TABLE linked_oauth2_tokens
DROP CONSTRAINT linked_oauth_2_tokens_config_id_fkey,
ADD CONSTRAINT linked_oauth_2_tokens_config_id_fkey
  FOREIGN KEY(config_id)
  REFERENCES oauth2_applications(id);
