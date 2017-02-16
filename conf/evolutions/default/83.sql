# --- !Ups

UPDATE inputs as i SET group_id =
  (SELECT DISTINCT(b.group_id) FROM behavior_parameters as bp
    JOIN behavior_versions as bv ON bp.behavior_version_id = bv.id
    JOIN behaviors as b ON bv.behavior_id = b.id
  WHERE bp.input_id = i.id);

# --- !Downs
