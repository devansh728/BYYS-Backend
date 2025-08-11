-- V1__init.sql
CREATE TABLE IF NOT EXISTS app_user (
    id BIGSERIAL PRIMARY KEY,
    phone VARCHAR(32) NOT NULL UNIQUE,
    referral_code VARCHAR(16) NOT NULL UNIQUE,
    referred_by_code VARCHAR(16),
    full_name VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    last_login_at TIMESTAMPTZ,
    avatar_url TEXT
);

CREATE TABLE IF NOT EXISTS referral_events (
    id BIGSERIAL PRIMARY KEY,
    referrer_user_id BIGINT NOT NULL,
    referred_user_id BIGINT,
    event_type VARCHAR(32) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    tracking_id UUID NOT NULL UNIQUE,
    user_agent TEXT,
    ip_address VARCHAR(45),
    referral_source VARCHAR(32),
    
    CONSTRAINT fk_referrer_user FOREIGN KEY (referrer_user_id) REFERENCES app_user(id),
    CONSTRAINT fk_referred_user FOREIGN KEY (referred_user_id) REFERENCES app_user(id)
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_user_phone ON app_user(phone);
CREATE INDEX IF NOT EXISTS idx_user_referral_code ON app_user(referral_code);

CREATE INDEX IF NOT EXISTS idx_referrer_user ON referral_events(referrer_user_id);
CREATE INDEX IF NOT EXISTS idx_referred_user ON referral_events(referred_user_id);
CREATE INDEX IF NOT EXISTS idx_event_type ON referral_events(event_type);
CREATE INDEX IF NOT EXISTS idx_occurred_at ON referral_events(occurred_at);