-- Flyway Migration: V2__create_urls_table.sql
-- Description: Create urls table for the Url entity (required by UrlService implementations)

-- Create urls table (expected by the Url entity)
CREATE TABLE urls (
    id BIGSERIAL PRIMARY KEY,
    short_code VARCHAR(10) NOT NULL UNIQUE,
    long_url VARCHAR(2048) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    click_count BIGINT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT true
);

-- Create unique index on short_code for fast lookup
CREATE UNIQUE INDEX idx_urls_short_code ON urls(short_code);

-- Create index on created_at for time-based queries
CREATE INDEX idx_urls_created_at ON urls(created_at);

-- Create index on is_active for filtering active URLs
CREATE INDEX idx_urls_is_active ON urls(is_active);

-- Create composite index for active URLs lookup (PostgreSQL specific)
CREATE INDEX idx_urls_short_code_active ON urls(short_code, is_active) WHERE is_active = true;