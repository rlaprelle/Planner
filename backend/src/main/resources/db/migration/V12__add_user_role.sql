-- V12__add_user_role.sql
--
-- Add a role column to app_user so SecurityConfig can differentiate admin-capable
-- users from regular users. Existing rows default to 'USER'; the seed migration in
-- V13 (dev profile only) promotes a single operator to 'ADMIN'.

ALTER TABLE app_user ADD COLUMN role VARCHAR(16) NOT NULL DEFAULT 'USER';

ALTER TABLE app_user ADD CONSTRAINT chk_app_user_role
    CHECK (role IN ('USER', 'ADMIN'));
