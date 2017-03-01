# --- !Ups

ALTER TABLE conversations ADD COLUMN channel TEXT;
UPDATE conversations SET channel = split_part(context, '#', 2);
UPDATE conversations SET context = split_part(context, '#', 1);

# --- !Downs

ALTER TABLE conversations DROP COLUMN channel;

