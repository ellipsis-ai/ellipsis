# --- !Ups

ALTER TABLE conversations
DROP CONSTRAINT conversations_trigger_id_fkey,
ADD CONSTRAINT conversations_trigger_id_fkey
  FOREIGN KEY (trigger_id)
  REFERENCES message_triggers (id)
  ON DELETE CASCADE;

# --- !Downs

ALTER TABLE conversations
DROP CONSTRAINT conversations_trigger_id_fkey,
ADD CONSTRAINT conversations_trigger_id_fkey
  FOREIGN KEY (trigger_id)
  REFERENCES message_triggers (id);
