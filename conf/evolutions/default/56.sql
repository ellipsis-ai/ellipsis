# --- !Ups

ALTER TABLE behavior_groups ADD COLUMN description TEXT;

# --- !Downs

ALTER TABLE behavior_groups DROP COLUMN description;
