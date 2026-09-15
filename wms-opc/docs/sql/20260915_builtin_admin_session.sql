-- The fixed administrator identity and password are defined only in the backend.
-- This table stores revocable login token digests, never an account or password.
CREATE TABLE IF NOT EXISTS sys_builtin_session (
    token_hash VARCHAR(64) PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
