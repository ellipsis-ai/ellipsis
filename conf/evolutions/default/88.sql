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

ALTER TABLE behavior_groups ADD COLUMN current_version_id TEXT REFERENCES behavior_group_versions(id);

UPDATE behavior_groups AS bg SET current_version_id = (
  SELECT bgv.id FROM behavior_group_versions AS bgv
  WHERE bgv.group_id = bg.id
  LIMIT 1
);

ALTER TABLE behavior_groups DROP COLUMN name;
ALTER TABLE behavior_groups DROP COLUMN icon;
ALTER TABLE behavior_groups DROP COLUMN description;

ALTER TABLE behavior_group_versions
DROP CONSTRAINT behavior_group_versions_group_id_fkey,
ADD CONSTRAINT behavior_group_versions_group_id_fkey
  FOREIGN KEY(group_id)
  REFERENCES behavior_groups(id)
  ON DELETE CASCADE;

ALTER TABLE behavior_versions
DROP CONSTRAINT behavior_versions_group_version_id_fkey,
ADD CONSTRAINT behavior_versions_group_version_id_fkey
  FOREIGN KEY(group_version_id)
  REFERENCES behavior_group_versions(id)
  ON DELETE CASCADE;

ALTER TABLE inputs ADD COLUMN group_version_id TEXT REFERENCES behavior_group_versions(id);

DELETE FROM behavior_group_versions
WHERE id NOT IN (SELECT current_version_id FROM behavior_groups);

DELETE FROM inputs WHERE group_id IS NULL;

DELETE FROM inputs AS i WHERE i.id NOT IN (
  SELECT bp.input_id FROM behavior_parameters AS bp WHERE bp.input_id IS NOT NULL
);

UPDATE inputs AS i SET group_version_id = (
  SELECT bg.current_version_id FROM behavior_groups AS bg
  WHERE bg.id = i.group_id
);

ALTER TABLE inputs ALTER COLUMN group_version_id SET NOT NULL;

ALTER TABLE inputs DROP COLUMN group_id;

ALTER TABLE inputs
DROP CONSTRAINT inputs_group_version_id_fkey,
ADD CONSTRAINT inputs_group_version_id_fkey
  FOREIGN KEY(group_version_id)
  REFERENCES behavior_group_versions(id)
  ON DELETE CASCADE;

ALTER TABLE inputs ADD COLUMN input_id TEXT;

UPDATE inputs SET input_id = generate_pseudorandom_id();

ALTER TABLE inputs ALTER COLUMN input_id SET NOT NULL;

DELETE FROM behavior_group_versions
WHERE id NOT IN (SELECT current_version_id FROM behavior_groups);

ALTER TABLE saved_answers
DROP CONSTRAINT saved_answers_input_id_fkey;

UPDATE saved_answers AS s SET input_id = (
  SELECT i.input_id FROM inputs AS i WHERE i.id = s.input_id LIMIT 1
);

COMMIT;

# --- !Downs

BEGIN;

UPDATE saved_answers AS s SET input_id = (
  SELECT i.id FROM inputs AS i WHERE i.input_id = s.input_id LIMIT 1
);

DELETE FROM saved_answers;

ALTER TABLE saved_answers
ADD CONSTRAINT saved_answers_input_id_fkey
  FOREIGN KEY(input_id)
  REFERENCES inputs(id)
  ON DELETE CASCADE;

ALTER TABLE inputs DROP COLUMN input_id;

ALTER TABLE inputs
DROP CONSTRAINT inputs_group_version_id_fkey,
ADD CONSTRAINT inputs_group_version_id_fkey
  FOREIGN KEY(group_version_id)
  REFERENCES behavior_group_versions(id)
  ON DELETE NO ACTION;

ALTER TABLE inputs ADD COLUMN group_id TEXT REFERENCES behavior_groups(id);

ALTER TABLE inputs DROP COLUMN group_version_id;

ALTER TABLE behavior_group_versions
DROP CONSTRAINT behavior_group_versions_group_id_fkey,
ADD CONSTRAINT behavior_group_versions_group_id_fkey
  FOREIGN KEY(group_id)
  REFERENCES behavior_groups(id)
  ON DELETE NO ACTION;


ALTER TABLE behavior_versions
DROP CONSTRAINT behavior_versions_group_version_id_fkey,
ADD CONSTRAINT behavior_versions_group_version_id_fkey
  FOREIGN KEY(group_version_id)
  REFERENCES behavior_group_versions(id)
  ON DELETE NO ACTION;

ALTER TABLE behavior_groups ADD COLUMN name TEXT NOT NULL DEFAULT '';
ALTER TABLE behavior_groups ADD COLUMN icon TEXT;
ALTER TABLE behavior_groups ADD COLUMN description TEXT;

ALTER TABLE behavior_groups DROP COLUMN current_version_id;

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
