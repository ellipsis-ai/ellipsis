# --- !Ups

ALTER TABLE conversations ADD COLUMN scheduled_message_id TEXT REFERENCES scheduled_messages(id);

# --- !Downs

ALTER TABLE conversations DROP COLUMN scheduled_message_id;

