# --- !Ups

ALTER TABLE scheduled_messages ADD COLUMN time_zone TEXT;

# --- !Downs

ALTER TABLE scheduled_messages DROP COLUMN time_zone;
