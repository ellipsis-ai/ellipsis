# --- !Ups

ALTER TABLE behaviors ADD COLUMN current_version_id TEXT;

UPDATE behaviors
SET current_version_id = versions.id
FROM
  (SELECT DISTINCT ON (behavior_id) id, behavior_id, created_at
   FROM behavior_versions
   ORDER BY behavior_id, created_at DESC) AS versions
WHERE behaviors.id = versions.behavior_id;

CREATE OR REPLACE FUNCTION update_current_version_id() RETURNS TRIGGER
AS
$$
  BEGIN
      UPDATE behaviors
      SET current_version_id = NEW.id
      WHERE id = NEW.behavior_id;;

      RETURN NEW;;
  END
$$
LANGUAGE plpgsql;

CREATE TRIGGER new_behavior_version_trigger
  AFTER INSERT
  ON behavior_versions
  FOR EACH ROW
  EXECUTE PROCEDURE update_current_version_id();

# --- !Downs

DROP TRIGGER IF EXISTS new_behavior_version_trigger ON behavior_versions;
DROP FUNCTION IF EXISTS update_current_version_id();

ALTER TABLE behaviors DROP COLUMN current_version_id;
