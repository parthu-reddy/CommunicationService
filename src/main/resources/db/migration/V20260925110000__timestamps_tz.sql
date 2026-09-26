-- Every remaining naive TIMESTAMP in this database becomes TIMESTAMPTZ.
--
-- Why: these columns held the JVM's wall clock, a correct instant only while the JVM ran in UTC.
-- The entities now map java.time.Instant, which Hibernate stores as TIMESTAMPTZ, and
-- `ddl-auto: validate` requires the two to agree. RandomDocuments/TimezoneCorrectness_2026-09-25, Phase 3.
--
-- USING ... AT TIME ZONE 'UTC' reads each stored wall clock as UTC, which is what the dev containers
-- wrote (they run in UTC). Every column listed here is TIMESTAMP before this migration, and
-- validate_time.py's S-USING check proves it; on a TIMESTAMPTZ column the same clause would convert
-- the wrong way.
--
-- Forward-only: the migrations that created these columns have been applied and are not edited.

ALTER TABLE chat_sessions
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';

ALTER TABLE call_logs
    ALTER COLUMN start_time TYPE TIMESTAMPTZ USING start_time AT TIME ZONE 'UTC',
    ALTER COLUMN end_time TYPE TIMESTAMPTZ USING end_time AT TIME ZONE 'UTC',
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

ALTER TABLE session_participants
    ALTER COLUMN joined_at TYPE TIMESTAMPTZ USING joined_at AT TIME ZONE 'UTC';

ALTER TABLE messages
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';
