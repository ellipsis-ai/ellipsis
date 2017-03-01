# --- !Ups

ALTER TABLE invocation_tokens
ADD COLUMN scheduled_message_id TEXT
REFERENCES scheduled_messages(id)
ON DELETE SET NULL;

# --- !Downs

ALTER TABLE invocation_tokens DROP COLUMN scheduled_message_id;

