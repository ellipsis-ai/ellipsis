# --- !Ups

ALTER TABLE inputs ADD COLUMN export_id TEXT;

# --- !Downs

ALTER TABLE inputs DROP COLUMN export_id;
