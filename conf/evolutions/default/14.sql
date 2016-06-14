# --- !Ups

ALTER TABLE behavior_versions
DROP CONSTRAINT behavior_versions_behavior_id_fkey,
ADD CONSTRAINT behavior_versions_behavior_id_fkey
   FOREIGN KEY (behavior_id)
   REFERENCES behaviors(id)
   ON DELETE CASCADE;

# --- !Downs

ALTER TABLE behavior_versions
DROP CONSTRAINT behavior_versions_behavior_id_fkey,
ADD CONSTRAINT behavior_versions_behavior_id_fkey
   FOREIGN KEY (behavior_id)
   REFERENCES behaviors(id);


