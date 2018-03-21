# --- !Ups

BEGIN;

CREATE INDEX message_triggers_behavior_version_id_index ON message_triggers(behavior_version_id);
CREATE INDEX inputs_group_version_id_index ON inputs(group_version_id);

COMMIT;

# --- !Downs

BEGIN;

DROP INDEX IF EXISTS message_triggers_behavior_version_id_index;
DROP INDEX IF EXISTS inputs_group_version_id_index;

COMMIT;
