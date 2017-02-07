# --- !Ups

ALTER TABLE behavior_versions RENAME COLUMN short_name TO name;

# --- !Downs

ALTER TABLE behavior_versions RENAME COLUMN name TO short_name;

