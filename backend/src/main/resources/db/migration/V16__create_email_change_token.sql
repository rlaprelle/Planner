-- V16__create_email_change_token.sql
--
-- Why this table exists
-- =====================
-- Issue #58 adds self-service email change. Changing the email a user logs in
-- with is sensitive: we must prove the user actually controls the NEW address
-- before switching, otherwise a logged-in session (or a stolen one) could
-- redirect the account to an attacker's mailbox.
--
-- The flow:
--   1. User submits a new email + their current password (re-auth).
--   2. We mint a single-use token, store only its SHA-256 hash here, and email
--      a verification link to the NEW address. The email column is NOT changed
--      yet.
--   3. User clicks the link; we look the token up by hash, switch the email,
--      and consume the token. All of the user's refresh tokens are revoked so
--      every session re-authenticates under the new address.
--
-- Column reference
-- ----------------
--   id           UUID, primary key.
--   user_id      FK to app_user. The account whose email will change. Indexed
--                for "invalidate this user's outstanding requests" lookups.
--   new_email    The address being verified / switched to. Stored so confirm
--                doesn't have to trust anything from the request beyond the token.
--   token_hash   SHA-256 of the raw token (hex, 64 chars). The raw token only
--                ever travels in the verification link; we store the hash, same
--                principle as refresh_token and password hashing.
--   expires_at   When the link stops working (login flow uses a short TTL).
--   consumed_at  When the token was redeemed. NULL = still usable. We keep
--                consumed rows rather than deleting, mirroring refresh_token.
--   created_at   Audit field.

CREATE TABLE email_change_token (
    id          UUID                        NOT NULL DEFAULT gen_random_uuid(),
    user_id     UUID                        NOT NULL,
    new_email   VARCHAR(255)                NOT NULL,
    token_hash  VARCHAR(64)                 NOT NULL,
    expires_at  TIMESTAMP WITH TIME ZONE    NOT NULL,
    consumed_at TIMESTAMP WITH TIME ZONE,
    created_at  TIMESTAMP WITH TIME ZONE    NOT NULL DEFAULT now(),

    CONSTRAINT pk_email_change_token PRIMARY KEY (id),
    CONSTRAINT uq_email_change_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_email_change_token_user
        FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE
);

-- "Invalidate all outstanding requests for this user" (issued when a new request
-- supersedes older ones) hits user_id; without this index those would table-scan.
CREATE INDEX idx_email_change_token_user ON email_change_token (user_id);
