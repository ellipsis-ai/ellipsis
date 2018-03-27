# --- !Ups

ALTER TABLE linked_accounts
DROP CONSTRAINT linked_accounts_provider_id_provider_key_key;

# --- !Downs

ALTER TABLE linked_accounts
ADD CONSTRAINT linked_accounts_provider_id_provider_key_key UNIQUE (provider_id, provider_key);
