# --- !Ups

ALTER TABLE behaviors ADD COLUMN code TEXT;
ALTER TABLE behaviors DROP COLUMN has_code;

# --- !Downs

ALTER TABLE behaviors ADD COLUMN has_code BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE behaviors DROP COLUMN code;
