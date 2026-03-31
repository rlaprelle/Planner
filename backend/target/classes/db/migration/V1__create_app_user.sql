CREATE TABLE app_user (
    id          UUID                        NOT NULL DEFAULT gen_random_uuid(),
    email       VARCHAR(255)                NOT NULL,
    password_hash VARCHAR(255)              NOT NULL,
    display_name VARCHAR(255)               NOT NULL,
    timezone    VARCHAR(100)                NOT NULL DEFAULT 'UTC',
    created_at  TIMESTAMP WITH TIME ZONE    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP WITH TIME ZONE    NOT NULL DEFAULT now(),

    CONSTRAINT pk_app_user PRIMARY KEY (id),
    CONSTRAINT uq_app_user_email UNIQUE (email)
);
