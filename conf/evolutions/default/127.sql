# --- !Ups

ALTER TABLE behavior_versions ADD COLUMN can_be_memoized BOOL NOT NULL DEFAULT FALSE;

# --- !Downs

ALTER TABLE behavior_versions DROP COLUMN can_be_memoized;
