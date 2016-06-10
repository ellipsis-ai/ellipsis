# --- !Ups

ALTER TABLE message_triggers ADD COLUMN is_case_sensitive BOOLEAN;

UPDATE message_triggers
SET is_case_sensitive = TRUE
WHERE treat_as_regex = TRUE;

UPDATE message_triggers
SET is_case_sensitive = FALSE
WHERE treat_as_regex = FALSE;

ALTER TABLE message_triggers ALTER COLUMN is_case_sensitive SET NOT NULL;

# --- !Downs

ALTER TABLE message_triggers DROP COLUMN is_case_sensitive;
