# --- !Ups

ALTER TABLE parent_conversations
DROP CONSTRAINT parent_conversations_parent_id_fkey,
ADD CONSTRAINT parent_conversations_parent_id_fkey
  FOREIGN KEY(parent_id)
  REFERENCES conversations(id)
  ON DELETE CASCADE;

ALTER TABLE parent_conversations
DROP CONSTRAINT parent_conversations_param_id_fkey,
ADD CONSTRAINT parent_conversations_param_id_fkey
  FOREIGN KEY(param_id)
  REFERENCES behavior_parameters(id)
  ON DELETE CASCADE;

# --- !Downs

ALTER TABLE parent_conversations
DROP CONSTRAINT parent_conversations_parent_id_fkey,
ADD CONSTRAINT parent_conversations_parent_id_fkey
  FOREIGN KEY(parent_id)
  REFERENCES conversations(id)
  ON DELETE NO ACTION;

ALTER TABLE parent_conversations
DROP CONSTRAINT parent_conversations_param_id_fkey,
ADD CONSTRAINT parent_conversations_param_id_fkey
  FOREIGN KEY(param_id)
  REFERENCES behavior_parameters(id)
  ON DELETE NO ACTION;
