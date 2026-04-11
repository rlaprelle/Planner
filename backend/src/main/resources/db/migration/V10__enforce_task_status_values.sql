-- Enforce valid task status values at the DB level.
-- Prevents old code from inserting deprecated statuses (TODO, IN_PROGRESS, etc.)
ALTER TABLE task
    ADD CONSTRAINT chk_task_status
    CHECK (status IN ('OPEN', 'COMPLETED', 'CANCELLED'));
