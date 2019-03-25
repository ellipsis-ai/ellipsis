# --- !Ups

ALTER TABLE behavior_group_versions ADD COLUMN use_node_8 BOOLEAN;

# --- !Downs

ALTER TABLE behavior_group_versions DROP COLUMN use_node_8;
