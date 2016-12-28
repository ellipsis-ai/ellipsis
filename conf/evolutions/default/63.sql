# --- !Ups

ALTER TABLE teams ADD COLUMN time_zone TEXT;

# --- !Downs

ALTER TABLE teams DROP COLUMN time_zone;
