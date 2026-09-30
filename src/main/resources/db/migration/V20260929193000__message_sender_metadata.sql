-- Keep sender identity immutable on the message. Session memberships are reconciled from the
-- authoritative order roster and may change after a message is written.
ALTER TABLE messages
    ADD COLUMN IF NOT EXISTS sender_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS sender_type VARCHAR(50);
