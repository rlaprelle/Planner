CREATE TABLE daily_reflection (
    id               UUID                     NOT NULL DEFAULT gen_random_uuid(),
    user_id          UUID                     NOT NULL,
    reflection_date  DATE                     NOT NULL,
    energy_rating    SMALLINT                 NOT NULL CHECK (energy_rating BETWEEN 1 AND 5),
    mood_rating      SMALLINT                 NOT NULL CHECK (mood_rating BETWEEN 1 AND 5),
    reflection_notes TEXT,
    is_finalized     BOOLEAN                  NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT pk_daily_reflection PRIMARY KEY (id),
    CONSTRAINT fk_daily_reflection_user FOREIGN KEY (user_id) REFERENCES app_user(id),
    CONSTRAINT uq_daily_reflection_user_date UNIQUE (user_id, reflection_date)
);

CREATE INDEX idx_daily_reflection_user_id ON daily_reflection(user_id);

CREATE TRIGGER trg_daily_reflection_updated_at
BEFORE UPDATE ON daily_reflection
FOR EACH ROW EXECUTE FUNCTION set_updated_at();
