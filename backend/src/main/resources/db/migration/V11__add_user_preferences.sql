-- V11__add_user_preferences.sql

-- Active preferences (exposed in settings UI)
ALTER TABLE app_user ADD COLUMN default_start_time    TIME     NOT NULL DEFAULT '08:00';
ALTER TABLE app_user ADD COLUMN default_end_time      TIME     NOT NULL DEFAULT '17:00';
ALTER TABLE app_user ADD COLUMN default_session_minutes SMALLINT NOT NULL DEFAULT 60;
ALTER TABLE app_user ADD COLUMN week_start_day         SMALLINT NOT NULL DEFAULT 1;
ALTER TABLE app_user ADD COLUMN ceremony_day           SMALLINT NOT NULL DEFAULT 5;

-- Dormant preferences (columns for future features, no UI yet)
ALTER TABLE app_user ADD COLUMN timer_warning_minutes  SMALLINT NOT NULL DEFAULT 5;
ALTER TABLE app_user ADD COLUMN timer_type             VARCHAR(10) NOT NULL DEFAULT 'countdown';
ALTER TABLE app_user ADD COLUMN enable_chime           BOOLEAN  NOT NULL DEFAULT true;
ALTER TABLE app_user ADD COLUMN quick_capture_keep_open BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE app_user ADD COLUMN max_daily_tasks        SMALLINT;
ALTER TABLE app_user ADD COLUMN streak_celebration_threshold SMALLINT;
ALTER TABLE app_user ADD COLUMN locale                 VARCHAR(10) NOT NULL DEFAULT 'en';

-- Constraints
ALTER TABLE app_user ADD CONSTRAINT chk_week_start_day
    CHECK (week_start_day BETWEEN 1 AND 7);

ALTER TABLE app_user ADD CONSTRAINT chk_ceremony_day
    CHECK (ceremony_day BETWEEN 1 AND 7);

ALTER TABLE app_user ADD CONSTRAINT chk_default_session_minutes
    CHECK (default_session_minutes > 0 AND default_session_minutes <= 240
           AND default_session_minutes % 15 = 0);

ALTER TABLE app_user ADD CONSTRAINT chk_timer_type
    CHECK (timer_type IN ('countdown', 'countup'));

ALTER TABLE app_user ADD CONSTRAINT chk_timer_warning_minutes
    CHECK (timer_warning_minutes > 0 AND timer_warning_minutes <= 60);

ALTER TABLE app_user ADD CONSTRAINT chk_default_times
    CHECK (default_start_time < default_end_time);
