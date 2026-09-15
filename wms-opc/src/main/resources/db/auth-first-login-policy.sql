-- New accounts may use their initial password without a mandatory change.
ALTER TABLE sys_user ALTER COLUMN must_change_password SET DEFAULT FALSE;

-- Upgrade existing initial accounts while preserving explicit password-reset requirements.
UPDATE sys_user u
SET must_change_password = FALSE, updated_at = CURRENT_TIMESTAMP
WHERE u.must_change_password = TRUE
  AND NOT EXISTS (
    SELECT 1 FROM sys_auth_audit a
    WHERE a.target_username = u.username
      AND a.action IN ('RESET_PASSWORD', 'RECOVER_ADMIN')
  );
