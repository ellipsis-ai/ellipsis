# --- !Ups

BEGIN;

ALTER TABLE behavior_groups DROP COLUMN name;
ALTER TABLE behavior_groups DROP COLUMN icon;
ALTER TABLE behavior_groups DROP COLUMN description;

COMMIT;

# --- !Downs

BEGIN;

ALTER TABLE behavior_groups ADD COLUMN name TEXT NOT NULL DEFAULT '';
ALTER TABLE behavior_groups ADD COLUMN icon TEXT;
ALTER TABLE behavior_groups ADD COLUMN description TEXT;

COMMIT;
