# --- !Ups

CREATE TABLE api_backed_data_types (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  team_id TEXT NOT NULL REFERENCES teams(id),
  current_version_id TEXT,
  imported_id TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE api_backed_data_type_versions (
  id TEXT PRIMARY KEY,
  data_type_id TEXT NOT NULL REFERENCES api_backed_data_types(id) ON DELETE CASCADE,
  http_method TEXT,
  url TEXT,
  request_body TEXT,
  function_body TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE OR REPLACE FUNCTION update_current_api_backed_data_type_version_id() RETURNS TRIGGER
AS
$$
  BEGIN
      UPDATE api_backed_data_types
      SET current_version_id = NEW.id
      WHERE id = NEW.data_type_id;;

      RETURN NEW;;
  END
$$
LANGUAGE plpgsql;

CREATE TRIGGER new_api_backed_data_type_version_trigger
  AFTER INSERT
  ON api_backed_data_type_versions
  FOR EACH ROW
  EXECUTE PROCEDURE update_current_api_backed_data_type_version_id();

# --- !Downs

DROP TRIGGER IF EXISTS new_api_backed_data_type_version_trigger ON api_backed_data_types;
DROP FUNCTION IF EXISTS update_current_api_backed_data_type_version_id();

DROP TABLE IF EXISTS api_backed_data_type_versions;
DROP TABLE IF EXISTS api_backed_data_type;
