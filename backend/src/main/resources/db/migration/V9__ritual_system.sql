-- Ritual System: task deferral, simplified statuses, reflection types

-- 1. Task deferral fields
ALTER TABLE task
    ADD COLUMN visible_from DATE,
    ADD COLUMN scheduling_scope VARCHAR(10),
    ADD COLUMN deferral_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN cancelled_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE task
    ADD CONSTRAINT chk_task_scheduling_scope
    CHECK (scheduling_scope IN ('DAY', 'WEEK', 'MONTH') OR scheduling_scope IS NULL);

CREATE INDEX idx_task_active_visible
    ON task(user_id, status, visible_from)
    WHERE archived_at IS NULL;

-- 2. Simplify task statuses: TODO/IN_PROGRESS/BLOCKED/SKIPPED -> OPEN, DONE -> COMPLETED
UPDATE task SET status = 'OPEN' WHERE status IN ('TODO', 'IN_PROGRESS', 'BLOCKED', 'SKIPPED');
UPDATE task SET status = 'COMPLETED' WHERE status = 'DONE';

-- 3. Extend daily_reflection for weekly/monthly
ALTER TABLE daily_reflection
    ADD COLUMN reflection_type VARCHAR(10) NOT NULL DEFAULT 'DAILY';

ALTER TABLE daily_reflection
    DROP CONSTRAINT uq_daily_reflection_user_date;

ALTER TABLE daily_reflection
    ADD CONSTRAINT uq_reflection_user_date_type
    UNIQUE (user_id, reflection_date, reflection_type);

ALTER TABLE daily_reflection
    ADD CONSTRAINT chk_reflection_type
    CHECK (reflection_type IN ('DAILY', 'WEEKLY', 'MONTHLY'));
