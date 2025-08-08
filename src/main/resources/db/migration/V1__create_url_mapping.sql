-- Flyway Migration: V1__create_url_mapping.sql
-- Description: Create url_mapping table with UUID primary key (H2 and PostgreSQL compatible)

-- Create url_mapping table
CREATE TABLE url_mapping (
    id UUID PRIMARY KEY,
    short_code VARCHAR(10) NOT NULL UNIQUE,
    long_url TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    click_count BIGINT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_by VARCHAR(50)
);

-- Create unique index on short_code for fast lookup
CREATE UNIQUE INDEX idx_short_code ON url_mapping(short_code);

-- Create index on created_at for time-based queries
CREATE INDEX idx_created_at ON url_mapping(created_at);

-- Create index on is_active for filtering active URLs
CREATE INDEX idx_is_active ON url_mapping(is_active);

-- Create composite index for active URLs lookup (PostgreSQL specific, commented for H2)
-- CREATE INDEX idx_short_code_active ON url_mapping(short_code, is_active) WHERE is_active = true;