# --- !Ups

ALTER TABLE scheduled_messages ADD COLUMN user_id TEXT REFERENCES users(id);

# --- !Downs

ALTER TABLE scheduled_messages DROP COLUMN user_id;
