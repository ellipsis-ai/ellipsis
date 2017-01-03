# --- !Ups

ALTER TABLE scheduled_messages ADD COLUMN monday BOOL;
ALTER TABLE scheduled_messages ADD COLUMN tuesday BOOL;
ALTER TABLE scheduled_messages ADD COLUMN wednesday BOOL;
ALTER TABLE scheduled_messages ADD COLUMN thursday BOOL;
ALTER TABLE scheduled_messages ADD COLUMN friday BOOL;
ALTER TABLE scheduled_messages ADD COLUMN saturday BOOL;
ALTER TABLE scheduled_messages ADD COLUMN sunday BOOL;

# --- !Downs

ALTER TABLE scheduled_messages DROP COLUMN monday;
ALTER TABLE scheduled_messages DROP COLUMN tuesday;
ALTER TABLE scheduled_messages DROP COLUMN wednesday;
ALTER TABLE scheduled_messages DROP COLUMN thursday;
ALTER TABLE scheduled_messages DROP COLUMN friday;
ALTER TABLE scheduled_messages DROP COLUMN saturday;
ALTER TABLE scheduled_messages DROP COLUMN sunday;
