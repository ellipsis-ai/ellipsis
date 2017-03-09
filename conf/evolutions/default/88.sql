# --- !Ups

BEGIN;

DROP TRIGGER IF EXISTS new_behavior_version_trigger ON behavior_versions;
DROP FUNCTION IF EXISTS update_current_version_id();

CREATE TABLE behavior_group_versions(
  id TEXT PRIMARY KEY,
  group_id TEXT NOT NULL REFERENCES behavior_groups(id),
  name TEXT NOT NULL,
  icon TEXT,
  description TEXT,
  author_id TEXT REFERENCES users(id),
  created_at TIMESTAMPTZ NOT NULL
);

CREATE OR REPLACE FUNCTION generate_pseudorandom_id() RETURNS TEXT
AS
$$
  BEGIN
    RETURN (SELECT array_to_string(array((
             SELECT SUBSTRING('abcdefghjklmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789-_'
             FROM mod((random()*58)::int, 58)+1 FOR 1)
           FROM generate_series(1,22))),''));;
  END
$$
LANGUAGE plpgsql;

INSERT INTO behavior_group_versions (
  SELECT generate_pseudorandom_id(), g.id, g.name, g.icon, g.description, MAX(bv.author_id), MAX(bv.created_at)
  FROM behavior_groups AS g JOIN behaviors AS b ON b.group_id = g.id JOIN behavior_versions AS bv ON bv.behavior_id = b.id
  GROUP BY g.id
);

ALTER TABLE behavior_versions ADD COLUMN group_version_id TEXT REFERENCES behavior_group_versions(id);

DELETE FROM behavior_versions
WHERE id NOT IN (SELECT current_version_id FROM behaviors WHERE current_version_id IS NOT NULL);

UPDATE behavior_versions AS bv SET group_version_id =
  (SELECT bgv.id FROM behavior_group_versions AS bgv JOIN behaviors as b ON b.group_id = bgv.group_id
  WHERE bv.behavior_id = b.id LIMIT 1);

ALTER TABLE behaviors DROP COLUMN current_version_id;

CREATE OR REPLACE FUNCTION update_current_group_version_id() RETURNS TRIGGER
AS
$$
  BEGIN
      UPDATE behavior_groups
      SET current_version_id = NEW.id
      WHERE id = NEW.group_id;;

      RETURN NEW;;
  END
$$
LANGUAGE plpgsql;

CREATE TRIGGER new_behavior_group_version_trigger
  AFTER INSERT
  ON behavior_group_versions
  FOR EACH ROW
  EXECUTE PROCEDURE update_current_group_version_id();

COMMIT;

# --- !Downs

BEGIN;

DROP TRIGGER IF EXISTS new_behavior_group_version_trigger ON behavior_group_versions;
DROP FUNCTION IF EXISTS update_current_group_version_id();

ALTER TABLE behaviors ADD COLUMN current_version_id TEXT REFERENCES behavior_versions(id);

ALTER TABLE behavior_versions DROP COLUMN group_version_id;

DROP FUNCTION IF EXISTS generate_pseudorandom_id();

DROP TABLE IF EXISTS behavior_group_versions;

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

COMMIT;
