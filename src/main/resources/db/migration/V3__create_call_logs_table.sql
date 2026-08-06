CREATE TABLE call_logs (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL,
    caller_id VARCHAR(255) NOT NULL,
    callee_id VARCHAR(255) NOT NULL,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    duration_seconds INT,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_call_log_session FOREIGN KEY (session_id) REFERENCES chat_sessions(id) ON DELETE CASCADE
);

CREATE INDEX idx_call_logs_session_id ON call_logs(session_id);
