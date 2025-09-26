# 🚀 Development Setup

## Quick Start

### Option 1: Mit Dummy-Credentials (Empfohlen für lokale Entwicklung)
```bash
# Anwendung starten - verwendet Dummy-Credentials
mvn spring-boot:run
```

### Option 2: Mit LocalStack
```bash
# LocalStack starten
docker run --rm -it -p 4566:4566 -p 4510-4559:4510-4559 localstack/localstack

# Anwendung mit dev-Profil starten
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Option 3: Mit echten AWS-Credentials
```bash
# Umgebungsvariablen setzen
export AWS_ACCESS_KEY_ID=your-access-key
export AWS_SECRET_ACCESS_KEY=your-secret-key
export AWS_REGION=eu-central-1
export IMAGE_BUCKET_NAME=your-bucket-name
export THUMBNAIL_BUCKET_NAME=your-thumbnails-bucket

# Validation aktivieren
export AWS_CONFIG_VALIDATION_ENABLED=true

mvn spring-boot:run
```

## 🔧 Konfiguration

### Umgebungsvariablen
- `AWS_ACCESS_KEY_ID` - AWS Access Key (default: "test")
- `AWS_SECRET_ACCESS_KEY` - AWS Secret Key (default: "test")  
- `AWS_REGION` - AWS Region (default: "eu-central-1")
- `IMAGE_BUCKET_NAME` - S3 Bucket für Images (default: "images-bucket-dev")
- `CLOUDFRONT_DOMAIN` - CloudFront Domain (default: "https://cdn.example.com")
- `AWS_CONFIG_VALIDATION_ENABLED` - AWS Validierung (default: false)

### Profile
- **default** - Dummy-Credentials, keine AWS-Validierung
- **dev** - LocalStack-Konfiguration
- **prod** - Production-Konfiguration (erfordert echte AWS-Credentials)

## 🏥 Health Checks

Nach dem Start verfügbar:
- http://localhost:8080/actuator/health
- http://localhost:8080/actuator/health/s3
- http://localhost:8080/swagger-ui.html