# --- !Ups

DELETE FROM conversations;

ALTER TABLE conversations ADD COLUMN trigger_id TEXT NOT NULL REFERENCES message_triggers(id);
ALTER TABLE conversations DROP COLUMN behavior_version_id;

# --- !Downs

ALTER TABLE conversations ADD COLUMN behavior_version_id TEXT NOT NULL REFERENCES behavior_versions(id);
ALTER TABLE conversations DROP COLUMN trigger_id;
