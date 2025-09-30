-- Create application user
CREATE USER "isUser" WITH PASSWORD 'isPassword';
GRANT ALL PRIVILEGES ON DATABASE "ImageStorage" TO "isUser";

-- Create Keycloak database and user
CREATE DATABASE keycloak;
CREATE USER keycloak WITH PASSWORD 'keycloak';
GRANT ALL PRIVILEGES ON DATABASE keycloak TO keycloak;

-- Fix schema ownership for both databases
\c "ImageStorage";
ALTER SCHEMA public OWNER TO "isUser";
GRANT ALL ON SCHEMA public TO "isUser";

\c keycloak;
ALTER SCHEMA public OWNER TO keycloak;
GRANT ALL ON SCHEMA public TO keycloak;