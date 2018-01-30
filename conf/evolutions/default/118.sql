# --- !Ups

ALTER TABLE linked_github_repos ADD COLUMN current_branch TEXT;

# --- !Downs

ALTER TABLE linked_github_repos DROP COLUMN IF EXISTS current_branch;
