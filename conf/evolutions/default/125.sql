# --- !Ups

ALTER TABLE conversations ADD COLUMN team_id_for_context TEXT;

# --- !Downs

ALTER TABLE conversations DROP COLUMN team_id_for_context;
