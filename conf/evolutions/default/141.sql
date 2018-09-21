# --- !Ups

ALTER TABLE behavior_groups ADD COLUMN deleted_at TIMESTAMPTZ;

# --- !Downs

ALTER TABLE behavior_groups DROP COLUMN deleted_at;
