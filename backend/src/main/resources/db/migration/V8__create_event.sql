-- V8__create_event.sql

-- 1. Create event table
CREATE TABLE event (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES app_user(id),
    project_id      UUID NOT NULL REFERENCES project(id),
    title           VARCHAR(255) NOT NULL,
    description     TEXT,
    energy_level    VARCHAR(20),
    block_date      DATE NOT NULL,
    start_time      TIME NOT NULL,
    end_time        TIME NOT NULL,
    archived_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_event_user_date ON event(user_id, block_date);
CREATE INDEX idx_event_project ON event(project_id);

CREATE TRIGGER trg_event_updated_at
    BEFORE UPDATE ON event
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- 2. Clean up any orphan time_block rows with null task_id
-- (could exist from extend feature)
DELETE FROM time_block WHERE task_id IS NULL;

-- 3. Add event_id to time_block
ALTER TABLE time_block
    ADD COLUMN event_id UUID REFERENCES event(id);

-- 4. Enforce exactly one of task_id or event_id is set
ALTER TABLE time_block
    ADD CONSTRAINT chk_time_block_task_or_event
    CHECK (
        (task_id IS NOT NULL AND event_id IS NULL) OR
        (task_id IS NULL AND event_id IS NOT NULL)
    );

-- 5. Add resolved_event_id to deferred_item
ALTER TABLE deferred_item
    ADD COLUMN resolved_event_id UUID REFERENCES event(id);
