# --- !Ups

ALTER TABLE conversations
DROP CONSTRAINT conversations_scheduled_message_id_fkey,
ADD CONSTRAINT conversations_scheduled_message_id_fkey
  FOREIGN KEY(scheduled_message_id)
  REFERENCES scheduled_messages(id)
  ON DELETE SET NULL;

# --- !Downs

ALTER TABLE conversations
DROP CONSTRAINT conversations_scheduled_message_id_fkey,
ADD CONSTRAINT conversations_scheduled_message_id_fkey
  FOREIGN KEY(scheduled_message_id)
  REFERENCES scheduled_messages(id)
  ON DELETE RESTRICT;
