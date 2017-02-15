# --- !Ups

ALTER TABLE behavior_groups ADD COLUMN icon TEXT;

# --- !Downs

ALTER TABLE behavior_groups DROP COLUMN icon;
