# --- !Ups

CREATE TABLE collected_parameter_values (
  parameter_id TEXT NOT NULL,
  conversation_id TEXT NOT NULL,
  value_string TEXT,
  PRIMARY KEY (parameter_id, conversation_id)
);

# --- !Downs

DROP TABLE IF EXISTS collected_parameter_values;
