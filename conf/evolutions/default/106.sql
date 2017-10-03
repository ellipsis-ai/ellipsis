# --- !Ups

ALTER TABLE teams ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;

# --- !Downs

ALTER TABLE teams DROP COLUMN created_at;