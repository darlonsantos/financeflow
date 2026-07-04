CREATE TABLE notification_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    budget_exceeded_enabled BOOLEAN NOT NULL DEFAULT true,
    low_balance_enabled BOOLEAN NOT NULL DEFAULT true,
    bills_due_enabled BOOLEAN NOT NULL DEFAULT true,
    goal_due_enabled BOOLEAN NOT NULL DEFAULT true,
    low_balance_threshold DECIMAL(15,2) NOT NULL DEFAULT 100.00,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notification_preferences_user_id ON notification_preferences(user_id);
