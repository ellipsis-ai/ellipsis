# --- !Ups

ALTER TABLE conversations ADD COLUMN last_interaction_at TIMESTAMPTZ;

# --- !Downs

ALTER TABLE conversations DROP COLUMN last_interaction_at;
