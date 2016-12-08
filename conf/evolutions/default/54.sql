# --- !Ups

ALTER TABLE behaviors
DROP CONSTRAINT behaviors_group_id_fkey,
ADD CONSTRAINT behaviors_group_id_fkey
  FOREIGN KEY(group_id)
  REFERENCES behavior_groups(id)
  ON DELETE CASCADE;

ALTER TABLE inputs
DROP CONSTRAINT inputs_group_id_fkey,
ADD CONSTRAINT inputs_group_id_fkey
  FOREIGN KEY(group_id)
  REFERENCES behavior_groups(id)
  ON DELETE CASCADE;

# --- !Downs

ALTER TABLE behaviors
DROP CONSTRAINT behaviors_group_id_fkey,
ADD CONSTRAINT behaviors_group_id_fkey
  FOREIGN KEY(group_id)
  REFERENCES behavior_groups(id);

ALTER TABLE inputs
DROP CONSTRAINT inputs_group_id_fkey,
ADD CONSTRAINT inputs_group_id_fkey
  FOREIGN KEY(group_id)
  REFERENCES behavior_groups(id);
