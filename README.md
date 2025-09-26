# S3 Playground

Spring Boot Anwendung für AWS S3 Operationen mit modernen Java-Testpraktiken.

## 🚀 Technologien

- **Java 24**
- **Spring Boot 3.5.6**
- **AWS SDK 2.28.11**
- **JUnit 5 + Mockito**
- **Testcontainers + LocalStack**

## 📁 Projektstruktur

```
src/
├── main/java/
│   └── com/valantic/sti/s3/
│       ├── S3Application.java      # Main Class
│       ├── S3Config.java           # S3Client Bean
│       ├── S3Controller.java       # REST API
│       └── S3Service.java          # Business Logic
├── test/java/                      # Unit Tests
│   └── com/valantic/sti/s3/
│       └── S3ServiceTest.java      # @ExtendWith(MockitoExtension)
└── integration-test/java/          # Integration Tests
    └── com/valantic/sti/s3/
        └── S3ServiceIntegrationTest.java  # @Testcontainers
```

## 🧪 Tests ausführen

```bash
# Nur Unit Tests
mvn test

# Unit + Integration Tests
mvn verify

# Alle Tests mit Coverage
mvn clean verify
```

## 🔧 Entwicklung

### Voraussetzungen
- Java 24
- Maven 3.8+
- Docker (für Integration Tests)

### Lokale Ausführung
```bash
mvn spring-boot:run
```

### AWS Konfiguration
```bash
# Environment Variables
export AWS_ACCESS_KEY_ID=your-key
export AWS_SECRET_ACCESS_KEY=your-secret
export AWS_REGION=eu-central-1
```

## 📋 API Endpoints

- `POST /s3/upload` - Datei hochladen
- `GET /s3/download` - Datei herunterladen
- `GET /s3/list` - Objekte auflisten
- `GET /s3/list?prefix=images/` - Gefilterte Auflistung

## 🐳 Integration Tests

Nutzen **Testcontainers** mit **LocalStack** für echte S3-API Tests ohne AWS-Kosten.

## 🔒 S3 Security Features

### Bucket-Level Security
- **ACL (Access Control Lists)** - Private/Public Bucket-Berechtigungen
- **Bucket Policy** - JSON-basierte Zugriffsregeln mit IP-Whitelist
- **Block Public Access** - Verhindert versehentliche öffentliche Freigabe
- **Versioning** - Schutz vor versehentlichem Löschen/Überschreiben
- **MFA Delete** - Multi-Factor Authentication für Löschvorgänge
- **Access Logging** - Audit-Trail aller Bucket-Zugriffe

### Object-Level Security
- **Object ACL** - Individuelle Objektberechtigungen
- **Server-Side Encryption (SSE-S3)** - AES-256 Verschlüsselung
- **KMS Encryption (SSE-KMS)** - Customer Managed Keys
- **Pre-signed URLs** - Temporärer, zeitbegrenzter Zugriff

### Security Best Practices
```java
// Sicherer Bucket mit allen Schutzmaßnahmen
securityService.createBucketWithACL("secure-bucket");
securityService.blockPublicAccess("secure-bucket");
securityService.enableVersioning("secure-bucket");
securityService.uploadFileWithEncryption("secure-bucket", "secret.txt", data);
```

### Beispiel Bucket Policy
- **HTTPS-Only** - Verweigert unverschlüsselte Verbindungen
- **IP-Whitelist** - Nur bestimmte IP-Bereiche erlaubt
- **MFA für Delete** - Multi-Factor Authentication erforderlich

Siehe `bucket-policy-example.json` für vollständige Policy-Beispiele.