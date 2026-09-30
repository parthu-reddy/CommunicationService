-- An order has exactly one chat. Previous schema versions indexed reference_id but did not make
-- it unique, allowing concurrent first requests to produce separate conversations.
--
-- Preserve all historical messages and call records while merging old duplicates into the oldest
-- session. Participant rows are copied through the existing (session_id, user_id) unique key.
WITH ranked AS (
    SELECT id,
           FIRST_VALUE(id) OVER (
               PARTITION BY session_type, reference_id
               ORDER BY created_at ASC, id ASC
           ) AS canonical_id,
           ROW_NUMBER() OVER (
               PARTITION BY session_type, reference_id
               ORDER BY created_at ASC, id ASC
           ) AS row_number
    FROM chat_sessions
    WHERE reference_id IS NOT NULL
), duplicates AS (
    SELECT id AS duplicate_id, canonical_id
    FROM ranked
    WHERE row_number > 1
)
INSERT INTO session_participants (id, session_id, user_id, entity_type, display_name, joined_at)
SELECT gen_random_uuid(), d.canonical_id, sp.user_id, sp.entity_type, sp.display_name, sp.joined_at
FROM session_participants sp
JOIN duplicates d ON d.duplicate_id = sp.session_id
ON CONFLICT (session_id, user_id) DO NOTHING;

WITH ranked AS (
    SELECT id,
           FIRST_VALUE(id) OVER (
               PARTITION BY session_type, reference_id
               ORDER BY created_at ASC, id ASC
           ) AS canonical_id,
           ROW_NUMBER() OVER (
               PARTITION BY session_type, reference_id
               ORDER BY created_at ASC, id ASC
           ) AS row_number
    FROM chat_sessions
    WHERE reference_id IS NOT NULL
), duplicates AS (
    SELECT id AS duplicate_id, canonical_id
    FROM ranked
    WHERE row_number > 1
)
UPDATE messages m
SET session_id = d.canonical_id
FROM duplicates d
WHERE m.session_id = d.duplicate_id;

WITH ranked AS (
    SELECT id,
           FIRST_VALUE(id) OVER (
               PARTITION BY session_type, reference_id
               ORDER BY created_at ASC, id ASC
           ) AS canonical_id,
           ROW_NUMBER() OVER (
               PARTITION BY session_type, reference_id
               ORDER BY created_at ASC, id ASC
           ) AS row_number
    FROM chat_sessions
    WHERE reference_id IS NOT NULL
), duplicates AS (
    SELECT id AS duplicate_id, canonical_id
    FROM ranked
    WHERE row_number > 1
)
UPDATE call_logs c
SET session_id = d.canonical_id
FROM duplicates d
WHERE c.session_id = d.duplicate_id;

WITH ranked AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY session_type, reference_id
               ORDER BY created_at ASC, id ASC
           ) AS row_number
    FROM chat_sessions
    WHERE reference_id IS NOT NULL
)
DELETE FROM session_participants sp
USING ranked r
WHERE sp.session_id = r.id AND r.row_number > 1;

WITH ranked AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY session_type, reference_id
               ORDER BY created_at ASC, id ASC
           ) AS row_number
    FROM chat_sessions
    WHERE reference_id IS NOT NULL
)
DELETE FROM chat_sessions cs
USING ranked r
WHERE cs.id = r.id AND r.row_number > 1;

CREATE UNIQUE INDEX IF NOT EXISTS uq_chat_sessions_session_type_reference_id
    ON chat_sessions (session_type, reference_id)
    WHERE reference_id IS NOT NULL;
