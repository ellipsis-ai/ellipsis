CREATE USER ellipsis WITH PASSWORD 'ellipsis';

CREATE DATABASE ellipsis OWNER ellipsis;
GRANT ALL PRIVILEGES ON DATABASE ellipsis TO ellipsis;

CREATE DATABASE "ellipsis-test" OWNER ellipsis;
GRANT ALL PRIVILEGES ON DATABASE "ellipsis-test" TO ellipsis;

DROP DATABASE IF EXISTS "ellipsis"
DROP DATABASE IF EXISTS "ellipsis-test"
