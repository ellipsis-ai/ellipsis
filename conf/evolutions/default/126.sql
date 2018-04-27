# --- !Ups

ALTER TABLE conversations
DROP CONSTRAINT conversations_parent_id_fkey,
ADD CONSTRAINT conversations_parent_id_fkey
  FOREIGN KEY(parent_id)
  REFERENCES parent_conversations(id)
  ON DELETE CASCADE;

# --- !Downs

ALTER TABLE conversations
DROP CONSTRAINT conversations_parent_id_fkey,
ADD CONSTRAINT conversations_parent_id_fkey
  FOREIGN KEY(parent_id)
  REFERENCES parent_conversations(id)
  ON DELETE NO ACTION;
