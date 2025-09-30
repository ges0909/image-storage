-- Create keycloak database first
CREATE DATABASE keycloak;

-- Create application user 'isUser'
CREATE USER "isUser" WITH PASSWORD 'isPassword';

-- Create keycloak user 'keycloak'
CREATE USER keycloak WITH PASSWORD 'keycloak';

-- Grant database-level privileges
GRANT ALL PRIVILEGES ON DATABASE "ImageStorage" TO "isUser";
GRANT ALL PRIVILEGES ON DATABASE keycloak TO keycloak;

-- Connect to each database to set the schema owner correctly
\c "ImageStorage"
ALTER SCHEMA public OWNER TO "isUser";

\c keycloak
ALTER SCHEMA public OWNER TO keycloak;
