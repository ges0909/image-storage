#!/bin/bash

# Wait for Keycloak to be ready
echo "Waiting for Keycloak to start..."
until curl -f http://localhost:8081/realms/master; do
  echo "Keycloak is not ready yet. Waiting..."
  sleep 5
done

echo "Keycloak is ready! Setting up realm and client..."

# Get admin access token
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8081/realms/master/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=admin" \
  -d "password=admin" \
  -d "grant_type=password" \
  -d "client_id=admin-cli" | sed -n 's/.*"access_token":"\([^"]*\)".*/\1/p')

if [ -z "$ADMIN_TOKEN" ]; then
  echo "Failed to get admin token. Check Keycloak logs."
  exit 1
fi

echo "Got admin token: ${ADMIN_TOKEN:0:20}..."

# Create realm
curl -X POST http://localhost:8081/admin/realms \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "realm": "image-storage",
    "enabled": true,
    "displayName": "Image Storage Realm"
  }'

# Create client
curl -X POST http://localhost:8081/admin/realms/image-storage/clients \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "image-storage-app",
    "name": "Image Storage Application",
    "enabled": true,
    "clientAuthenticatorType": "client-secret",
    "secret": "your-client-secret",
    "redirectUris": ["http://localhost:8080/login/oauth2/code/keycloak"],
    "webOrigins": ["http://localhost:8080"],
    "standardFlowEnabled": true,
    "directAccessGrantsEnabled": true,
    "serviceAccountsEnabled": true
  }'

# Create roles
curl -X POST http://localhost:8081/admin/realms/image-storage/roles \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name": "USER", "description": "Standard user role"}'

curl -X POST http://localhost:8081/admin/realms/image-storage/roles \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name": "ADMIN", "description": "Administrator role"}'

curl -X POST http://localhost:8081/admin/realms/image-storage/roles \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name": "UPLOADER", "description": "Can upload images"}'

# Create test user
curl -X POST http://localhost:8081/admin/realms/image-storage/users \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "firstName": "Test",
    "lastName": "User",
    "enabled": true,
    "credentials": [{
      "type": "password",
      "value": "password",
      "temporary": false
    }]
  }'

echo "Keycloak setup completed!"
echo "Access Keycloak Admin Console: http://localhost:8081 (admin/admin)"
echo "Test user: testuser/password"