# 🔐 Keycloak Integration mit Docker Compose

## 🚀 **Quick Start**

### **1. Alle Services starten**

```bash
docker-compose up -d
```

### **2. Services prüfen**

```bash
docker-compose ps
```

### **3. Keycloak Setup ausführen**

```bash
# Linux/Mac
chmod +x init-keycloak.sh
./init-keycloak.sh

# Windows
bash init-keycloak.sh
```

## 🌐 **Service URLs**

| Service            | URL                                   | Credentials         |
|--------------------|---------------------------------------|---------------------|
| **Keycloak Admin** | http://localhost:8081                 | `admin/admin`       |
| **Application**    | http://localhost:8080                 | OAuth2 Login        |
| **Swagger UI**     | http://localhost:8080/swagger-ui.html | -                   |
| **PostgreSQL**     | localhost:5432                        | `s3user/s3password` |
| **Redis**          | localhost:6379                        | -                   |
| **LocalStack S3**  | localhost:4566                        | -                   |

## 🔧 **Keycloak Konfiguration**

### **Automatisch erstellt:**

- **Realm**: `image-storage`
- **Client**: `image-storage-app`
- **Rollen**: `USER`, `ADMIN`, `UPLOADER`
- **Test User**: `testuser/password`

### **Manuelle Konfiguration:**

1. **Keycloak Admin Console**: http://localhost:8081
2. **Login**: `admin/admin`
3. **Realm wechseln**: `image-storage`
4. **Client Settings prüfen**: `image-storage-app`

## 🧪 **Testing**

### **1. OAuth2 Login Flow**

```bash
# Browser öffnen
open http://localhost:8080/oauth2/authorization/keycloak

# Login mit: testuser/password
# Redirect zu Swagger UI
```

### **2. API Token Test**

```bash
# Access Token holen
TOKEN=$(curl -s -X POST http://localhost:8081/realms/image-storage/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=image-storage-app" \
  -d "client_secret=your-client-secret" | jq -r '.access_token')

# API Call mit Token
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/images/stats
```

## 🐳 **Docker Services**

### **Keycloak**

- **Image**: `quay.io/keycloak/keycloak:23.0`
- **Port**: `8081:8080`
- **Database**: PostgreSQL
- **Mode**: Development

### **PostgreSQL**

- **Databases**: `s3playground`, `keycloak`
- **Users**: `s3user`, `keycloak`

### **Application**

- **Profiles**: `dev,keycloak`
- **Dependencies**: Keycloak, PostgreSQL, Redis, LocalStack

## 🔄 **Development Workflow**

### **Services neu starten**

```bash
docker-compose down
docker-compose up -d
```

### **Nur Keycloak neu starten**

```bash
docker-compose restart keycloak
```

### **Logs anzeigen**

```bash
# Alle Services
docker-compose logs -f

# Nur Keycloak
docker-compose logs -f keycloak

# Nur Application
docker-compose logs -f app
```

### **Keycloak Reset**

```bash
docker-compose down -v
docker-compose up -d
./init-keycloak.sh
```

## 🛠️ **Troubleshooting**

### **Keycloak startet nicht**

```bash
# Logs prüfen
docker-compose logs keycloak

# Container neu starten
docker-compose restart keycloak
```

### **Application kann nicht zu Keycloak verbinden**

```bash
# Network prüfen
docker network ls
docker network inspect image-storage_default

# DNS Resolution testen
docker-compose exec app nslookup keycloak
```

### **Database Connection Fehler**

```bash
# PostgreSQL Status
docker-compose exec postgres pg_isready -U s3user

# Keycloak Database prüfen
docker-compose exec postgres psql -U keycloak -d keycloak -c "\dt"
```

## 📋 **Environment Variables**

Die wichtigsten Keycloak-Variablen sind bereits in `docker-compose.yml` konfiguriert:

```yaml
- KEYCLOAK_CLIENT_ID=image-storage-app
- KEYCLOAK_CLIENT_SECRET=your-client-secret
- KEYCLOAK_ISSUER_URI=http://keycloak:8080/realms/image-storage
```

Für Production sollten diese über externe `.env` Datei gesetzt werden.
