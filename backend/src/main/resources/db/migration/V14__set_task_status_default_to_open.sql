-- Fix V3 default that became invalid after V10 added a CHECK constraint.
-- V3 set status DEFAULT 'TODO', but V10's CHECK now requires status IN ('OPEN','COMPLETED','CANCELLED').
-- Any INSERT that omits status would fail with a confusing CHECK violation.
-- Aligns the default with the constraint.

ALTER TABLE task ALTER COLUMN status SET DEFAULT 'OPEN';
