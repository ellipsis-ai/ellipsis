# --- !Ups

UPDATE saved_answers AS s SET input_id = (
  SELECT i.input_id FROM inputs AS i WHERE i.id = s.input_id LIMIT 1
);

# --- !Downs

UPDATE saved_answers AS s SET input_id = (
  SELECT i.id FROM inputs AS i WHERE i.input_id = s.input_id LIMIT 1
);
