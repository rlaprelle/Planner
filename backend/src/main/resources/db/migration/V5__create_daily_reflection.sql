CREATE TABLE daily_reflection (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES app_user(id),
    reflection_date DATE NOT NULL,
    energy_rating   SMALLINT NOT NULL CHECK (energy_rating BETWEEN 1 AND 5),
    mood_rating     SMALLINT NOT NULL CHECK (mood_rating BETWEEN 1 AND 5),
    reflection_notes TEXT,
    is_finalized    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, reflection_date)
);
