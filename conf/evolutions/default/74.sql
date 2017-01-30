# --- !Ups

TRUNCATE TABLE conversations CASCADE;
ALTER TABLE conversations ADD COLUMN channel TEXT;

# --- !Downs

TRUNCATE TABLE conversations CASCADE;
ALTER TABLE conversations DROP COLUMN channel;

