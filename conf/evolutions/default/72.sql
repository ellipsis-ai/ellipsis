# --- !Ups

ALTER TABLE conversations ADD COLUMN thread_id TEXT;

# --- !Downs

ALTER TABLE conversations DROP COLUMN thread_id;

