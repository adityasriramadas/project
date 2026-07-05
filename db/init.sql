-- Initialize databases and user for local dev
-- This script will run on first start of Postgres container
CREATE DATABASE inventorydb;
CREATE DATABASE orderdb;
DO $$
BEGIN
    -- create user if not exists (Postgres 9.5+)
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'appuser') THEN
        CREATE USER appuser WITH PASSWORD 'app_pass';
    END IF;
END
$$;
GRANT ALL PRIVILEGES ON DATABASE inventorydb TO appuser;
GRANT ALL PRIVILEGES ON DATABASE orderdb TO appuser;

-- Connect to orderdb and grant schema privileges
\c orderdb
GRANT ALL ON SCHEMA public TO appuser;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO appuser;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO appuser;

-- Connect to inventorydb and grant schema privileges
\c inventorydb
GRANT ALL ON SCHEMA public TO appuser;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO appuser;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO appuser;
