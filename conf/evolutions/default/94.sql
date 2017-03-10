# --- !Ups

BEGIN;

ALTER TABLE inputs ADD COLUMN input_id TEXT;

UPDATE inputs SET input_id = generate_pseudorandom_id();

ALTER TABLE inputs ALTER COLUMN input_id SET NOT NULL;

DELETE FROM behavior_group_versions
WHERE id NOT IN (SELECT current_version_id FROM behavior_groups);

ALTER TABLE saved_answers
DROP CONSTRAINT saved_answers_input_id_fkey;

COMMIT;

# --- !Downs

BEGIN;

DELETE FROM saved_answers;

ALTER TABLE saved_answers
ADD CONSTRAINT saved_answers_input_id_fkey
  FOREIGN KEY(input_id)
  REFERENCES inputs(id)
  ON DELETE CASCADE;

ALTER TABLE inputs DROP COLUMN input_id;

COMMIT;
