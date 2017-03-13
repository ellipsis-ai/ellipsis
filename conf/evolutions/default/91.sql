# --- !Ups

ALTER TABLE scheduled_behaviors
DROP CONSTRAINT scheduled_behaviors_behavior_id_fkey,
ADD CONSTRAINT scheduled_behaviors_behavior_id_fkey
  FOREIGN KEY(behavior_id)
  REFERENCES behaviors(id)
  ON DELETE CASCADE;

# --- !Downs

ALTER TABLE scheduled_behaviors
DROP CONSTRAINT scheduled_behaviors_behavior_id_fkey,
ADD CONSTRAINT scheduled_behaviors_behavior_id_fkey
  FOREIGN KEY(behavior_id)
  REFERENCES behaviors(id)
  ON DELETE NO ACTION;
