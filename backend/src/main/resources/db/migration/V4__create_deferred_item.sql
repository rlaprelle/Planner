CREATE TABLE deferred_item (
    id                   UUID                     NOT NULL DEFAULT gen_random_uuid(),
    user_id              UUID                     NOT NULL,
    raw_text             TEXT                     NOT NULL,
    is_processed         BOOLEAN                  NOT NULL DEFAULT false,
    captured_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_at         TIMESTAMP WITH TIME ZONE,
    resolved_task_id     UUID,
    resolved_project_id  UUID,
    deferred_until_date  DATE,
    deferral_count       INTEGER                  NOT NULL DEFAULT 0,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT pk_deferred_item PRIMARY KEY (id),
    CONSTRAINT fk_deferred_item_user FOREIGN KEY (user_id) REFERENCES app_user(id),
    CONSTRAINT fk_deferred_item_task FOREIGN KEY (resolved_task_id) REFERENCES task(id),
    CONSTRAINT fk_deferred_item_project FOREIGN KEY (resolved_project_id) REFERENCES project(id)
);

CREATE INDEX idx_deferred_item_user_id ON deferred_item(user_id);
CREATE INDEX idx_deferred_item_is_processed ON deferred_item(is_processed);

CREATE TRIGGER trg_deferred_item_updated_at
BEFORE UPDATE ON deferred_item
FOR EACH ROW EXECUTE FUNCTION set_updated_at();
