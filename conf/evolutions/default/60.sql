# --- !Ups

ALTER TABLE scheduled_messages ADD COLUMN is_for_individual_members BOOLEAN NOT NULL DEFAULT FALSE;

# --- !Downs

ALTER TABLE scheduled_messages DROP COLUMN is_for_individual_members;


