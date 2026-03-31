CREATE TABLE project (
    id          UUID                        NOT NULL DEFAULT gen_random_uuid(),
    user_id     UUID                        NOT NULL,
    name        VARCHAR(255)                NOT NULL,
    description TEXT,
    color       VARCHAR(7),
    icon        VARCHAR(50),
    is_active   BOOLEAN                     NOT NULL DEFAULT true,
    sort_order  INTEGER                     NOT NULL DEFAULT 0,
    archived_at TIMESTAMP WITH TIME ZONE,
    created_at  TIMESTAMP WITH TIME ZONE    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP WITH TIME ZONE    NOT NULL DEFAULT now(),

    CONSTRAINT pk_project PRIMARY KEY (id),
    CONSTRAINT fk_project_user FOREIGN KEY (user_id) REFERENCES app_user(id)
);

CREATE INDEX idx_project_user_id ON project(user_id);
