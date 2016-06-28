# --- !Ups

ALTER TABLE behaviors ADD COLUMN imported_id TEXT;

# --- !Downs

ALTER TABLE behaviors DROP COLUMN imported_id;
