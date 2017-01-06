# --- !Ups

ALTER TABLE saved_answers
DROP CONSTRAINT saved_answers_input_id_fkey,
ADD CONSTRAINT saved_answers_input_id_fkey
  FOREIGN KEY(input_id)
  REFERENCES inputs(id)
  ON DELETE CASCADE;

ALTER TABLE saved_answers
DROP CONSTRAINT saved_answers_user_id_fkey,
ADD CONSTRAINT saved_answers_user_id_fkey
  FOREIGN KEY(user_id)
  REFERENCES users(id)
  ON DELETE CASCADE;

# --- !Downs

ALTER TABLE saved_answers
DROP CONSTRAINT saved_answers_input_id_fkey,
ADD CONSTRAINT saved_answers_input_id_fkey
  FOREIGN KEY(input_id)
  REFERENCES inputs(id)
  ON DELETE SET NULL;

ALTER TABLE saved_answers
DROP CONSTRAINT saved_answers_user_id_fkey,
ADD CONSTRAINT saved_answers_user_id_fkey
  FOREIGN KEY(user_id)
  REFERENCES users(id)
  ON DELETE SET NULL;
