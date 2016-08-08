# --- !Ups

ALTER TABLE linked_oauth_2_tokens ADD COLUMN scope_granted TEXT;

# --- !Downs

ALTER TABLE linked_oauth_2_tokens DROP COLUMN scope_granted;

