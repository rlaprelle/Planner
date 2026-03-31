CREATE TABLE task (
    id                 UUID                     NOT NULL DEFAULT gen_random_uuid(),
    project_id         UUID                     NOT NULL,
    user_id            UUID                     NOT NULL,
    title              VARCHAR(255)             NOT NULL,
    description        TEXT,
    parent_task_id     UUID,
    status             VARCHAR(20)              NOT NULL DEFAULT 'TODO',
    priority           SMALLINT                 NOT NULL DEFAULT 3,
    points_estimate    SMALLINT,
    actual_minutes     INTEGER,
    energy_level       VARCHAR(10),
    due_date           DATE,
    sort_order         INTEGER                  NOT NULL DEFAULT 0,
    blocked_by_task_id UUID,
    archived_at        TIMESTAMP WITH TIME ZONE,
    completed_at       TIMESTAMP WITH TIME ZONE,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT pk_task PRIMARY KEY (id),
    CONSTRAINT fk_task_project FOREIGN KEY (project_id) REFERENCES project(id),
    CONSTRAINT fk_task_user FOREIGN KEY (user_id) REFERENCES app_user(id),
    CONSTRAINT fk_task_parent FOREIGN KEY (parent_task_id) REFERENCES task(id),
    CONSTRAINT fk_task_blocked_by FOREIGN KEY (blocked_by_task_id) REFERENCES task(id)
);

CREATE INDEX idx_task_project_id ON task(project_id);
CREATE INDEX idx_task_user_id ON task(user_id);
CREATE INDEX idx_task_parent_task_id ON task(parent_task_id);

CREATE TRIGGER trg_task_updated_at
BEFORE UPDATE ON task
FOR EACH ROW EXECUTE FUNCTION set_updated_at();
