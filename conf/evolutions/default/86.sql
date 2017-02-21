# --- !Ups

ALTER TABLE conversations ALTER COLUMN trigger_message DROP NOT NULL;


# --- !Downs

DELETE FROM conversations WHERE trigger_message IS NULL;
ALTER TABLE conversations ALTER COLUMN trigger_message SET NOT NULL;
