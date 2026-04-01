CREATE TABLE time_block (
    id         UUID    NOT NULL DEFAULT gen_random_uuid(),
    user_id    UUID    NOT NULL,
    block_date DATE    NOT NULL,
    task_id    UUID,
    start_time TIME    NOT NULL,
    end_time   TIME    NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT pk_time_block PRIMARY KEY (id),
    CONSTRAINT fk_time_block_user FOREIGN KEY (user_id) REFERENCES app_user(id),
    CONSTRAINT fk_time_block_task FOREIGN KEY (task_id) REFERENCES task(id)
);

CREATE INDEX idx_time_block_user_date ON time_block(user_id, block_date);
