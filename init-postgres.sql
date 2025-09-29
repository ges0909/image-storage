-- Create Keycloak database and user
CREATE DATABASE keycloak;
DO
$do$
BEGIN
   IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'keycloak') THEN
      CREATE USER keycloak WITH PASSWORD 'keycloak';
   END IF;
END
$do$;
GRANT ALL PRIVILEGES ON DATABASE keycloak TO keycloak;

-- Fix schema ownership for both databases
\c s3playground;
ALTER SCHEMA public OWNER TO postgres;
GRANT ALL ON SCHEMA public TO postgres;

\c keycloak;
ALTER SCHEMA public OWNER TO keycloak;
GRANT ALL ON SCHEMA public TO keycloak;