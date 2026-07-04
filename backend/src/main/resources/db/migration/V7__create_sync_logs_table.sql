CREATE TYPE sync_action AS ENUM ('CREATE', 'UPDATE', 'DELETE');
CREATE TYPE sync_status AS ENUM ('PENDING', 'SYNCED', 'CONFLICT', 'ERROR');

CREATE TABLE sync_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    action sync_action NOT NULL,
    sync_status sync_status NOT NULL,
    client_timestamp TIMESTAMP NOT NULL,
    server_timestamp TIMESTAMP,
    conflict_resolution TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sync_logs_user_id ON sync_logs(user_id);
CREATE INDEX idx_sync_logs_entity_type ON sync_logs(entity_type);
CREATE INDEX idx_sync_logs_sync_status ON sync_logs(sync_status);
CREATE INDEX idx_sync_logs_created_at ON sync_logs(created_at);
