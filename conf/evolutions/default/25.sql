# --- !Ups

ALTER TABLE behavior_versions ADD COLUMN author_id TEXT REFERENCES users(id);

# --- !Downs

ALTER TABLE behavior_versions DROP COLUMN author_id;
