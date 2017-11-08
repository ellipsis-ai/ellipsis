# --- !Ups

ALTER TABLE conversations ADD COLUMN original_event_type TEXT;

# --- !Downs

ALTER TABLE conversations DROP COLUMN original_event_type;
