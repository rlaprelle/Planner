-- Convert refresh_token.token_hash from CHAR(64) to VARCHAR(64).
--
-- Why: the JPA entity (RefreshToken.java) maps token_hash to a Java String,
-- which Hibernate expects to be VARCHAR by default. The V13 migration declared
-- the column as CHAR(64), so Hibernate schema validation fails at startup
-- under the prod profile:
--
--   SchemaManagementException: Schema-validation: wrong column type encountered
--   in column [token_hash] in table [refresh_token];
--   found [bpchar (Types#CHAR)], but expecting [varchar(64) (Types#VARCHAR)]
--
-- Why VARCHAR and not adjust the entity: VARCHAR(64) is the conventional type
-- for a fixed-length hex hash anyway. CHAR pads with trailing spaces for
-- shorter values; we never write a token_hash shorter than 64 chars, so the
-- pad behavior is irrelevant, but the type choice should match the entity's
-- natural mapping rather than have us thread columnDefinition annotations.
--
-- Safe to apply: SHA-256 hex hashes are exactly 64 characters, so no CHAR
-- padding gets stripped during the conversion. The UNIQUE constraint and
-- foreign key references survive the type change (Postgres re-validates
-- but the column semantics don't change).

ALTER TABLE refresh_token
    ALTER COLUMN token_hash TYPE VARCHAR(64);
