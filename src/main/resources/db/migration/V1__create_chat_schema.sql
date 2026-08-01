-- Chat Sessions: one per order
CREATE TABLE chat_sessions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_type    VARCHAR(50)  NOT NULL DEFAULT 'ORDER',
    reference_id    VARCHAR(100),
    is_active       BOOLEAN      NOT NULL DEFAULT true,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Session Participants: who is in each chat
CREATE TABLE session_participants (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id   UUID         NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
    user_id      VARCHAR(100) NOT NULL,
    entity_type  VARCHAR(50)  NOT NULL,
    display_name VARCHAR(150),
    joined_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE(session_id, user_id)
);

-- Messages
CREATE TABLE messages (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id   UUID         NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
    sender_id    VARCHAR(100) NOT NULL,
    message_type VARCHAR(50)  NOT NULL DEFAULT 'TEXT',
    content      TEXT         NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_messages_session_id ON messages(session_id);
CREATE INDEX idx_messages_created_at ON messages(created_at);
CREATE INDEX idx_chat_sessions_reference_id ON chat_sessions(reference_id);
CREATE INDEX idx_session_participants_user_id ON session_participants(user_id);
CREATE INDEX idx_session_participants_session_id ON session_participants(session_id);
