# --- !Ups

ALTER TABLE conversations ADD COLUMN behavior_version_id TEXT REFERENCES behavior_versions(id);

UPDATE conversations as c SET behavior_version_id =
  (SELECT behavior_version_id FROM message_triggers AS m WHERE m.id = c.trigger_id);

ALTER TABLE conversations ALTER COLUMN trigger_id DROP NOT NULL;


# --- !Downs

DELETE FROM TABLE conversations WHERE trigger_id IS NULL;
ALTER TABLE conversations ALTER COLUMN trigger_id SET NOT NULL;
ALTER TABLE conversations DROP COLUMN behavior_version_id;
