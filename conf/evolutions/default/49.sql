# --- !Ups

ALTER TABLE inputs ADD COLUMN is_saved_for_team BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE inputs ADD COLUMN is_saved_for_user BOOLEAN NOT NULL DEFAULT false;

# --- !Downs

ALTER TABLE inputs DROP COLUMN is_saved_for_user;
ALTER TABLE inputs DROP COLUMN is_saved_for_team;
