-- PostgreSQL initialization script for URL Shortener
-- This script will be executed when the PostgreSQL container starts for the first time

-- Create the urlshortener database (if it doesn't exist)
-- Note: The database is already created by the POSTGRES_DB environment variable

-- Create extensions if needed
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Set timezone
SET timezone = 'UTC';

-- Basic setup is complete
-- Flyway will handle the actual table creation via migrations