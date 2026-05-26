-- V13__create_refresh_token.sql
--
-- Why this table exists
-- =====================
-- Before this migration, refresh tokens were self-contained JWTs: anything that
-- could present a non-expired, validly-signed token was accepted as proof of
-- session, and the server had no way to revoke an individual token short of
-- rotating the JWT signing secret (which logs everyone out).
--
-- Issue #75 introduces:
--   1. ROTATION — every successful /auth/refresh issues a NEW token and
--      invalidates the OLD one. The client only ever holds the most recent token.
--   2. REUSE DETECTION — if an already-revoked token is presented again, that
--      strongly suggests theft (an attacker captured the cookie before the legit
--      client rotated it). We respond by revoking the entire chain of tokens for
--      that user-session, forcing a fresh login.
--   3. SERVER-SIDE REVOCATION — logout now actually invalidates the token
--      server-side, not just clears the cookie.
--
-- To do any of that the server needs a row per outstanding refresh token.
--
-- Column reference
-- ----------------
--   id           UUID, primary key. The internal identifier we use for parent/child links.
--   user_id      FK to app_user. Owns the token. Indexed for fast "revoke all this user's
--                tokens" queries (used today by reuse-detection; the future "sign out
--                everywhere" feature will use the same index).
--   token_hash   SHA-256 of the raw token (hex-encoded, 64 chars). We never store the
--                raw token — same principle as password hashing. Unique because token
--                lookups happen by hash, and there must be no collisions.
--   jti          UUID, unique. A stable, log-safe identifier for this token. The raw
--                token is a secret we won't log; the hash is opaque and hard to grep
--                for; jti gives us a human-readable handle in audit logs.
--   parent_id    Self-FK to the token this one replaced (NULL for the first token in a
--                chain, i.e. the one issued at login). Lets us trace and revoke an
--                entire rotation chain when we detect reuse.
--   expires_at   When the token stops being valid (currently login + 7 days).
--   revoked_at   When the token was invalidated (by rotation, logout, or reuse
--                detection). NULL = still active. We never DELETE rows here — keeping
--                revoked rows is what allows reuse detection to fire.
--   created_at   Audit field.
--
-- A nightly cleanup of rows where expires_at < now() - interval '30 days' is a sensible
-- follow-up, but isn't required for correctness; nothing reads expired/revoked rows in
-- a way that affects security decisions.

CREATE TABLE refresh_token (
    id          UUID                        NOT NULL DEFAULT gen_random_uuid(),
    user_id     UUID                        NOT NULL,
    token_hash  CHAR(64)                    NOT NULL,
    jti         UUID                        NOT NULL,
    parent_id   UUID,
    expires_at  TIMESTAMP WITH TIME ZONE    NOT NULL,
    revoked_at  TIMESTAMP WITH TIME ZONE,
    created_at  TIMESTAMP WITH TIME ZONE    NOT NULL DEFAULT now(),

    CONSTRAINT pk_refresh_token PRIMARY KEY (id),
    CONSTRAINT uq_refresh_token_hash UNIQUE (token_hash),
    CONSTRAINT uq_refresh_token_jti UNIQUE (jti),
    CONSTRAINT fk_refresh_token_user
        FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_refresh_token_parent
        FOREIGN KEY (parent_id) REFERENCES refresh_token(id) ON DELETE SET NULL
);

-- Lookups during /auth/refresh hit token_hash (already indexed via UNIQUE above,
-- which PostgreSQL implements with a btree index).

-- Lookups for "revoke all of a user's tokens" (reuse detection today, sign-out-
-- everywhere later) hit user_id. Without this index those would table-scan.
CREATE INDEX idx_refresh_token_user ON refresh_token (user_id);
