# --- !Ups

ALTER TABLE behaviors ADD COLUMN response_template TEXT;

# --- !Downs

ALTER TABLE behaviors DROP COLUMN response_template;
