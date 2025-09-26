# 🖼️ AWS S3 Bildspeicher Architektur

## 🏗️ System-Architektur

```mermaid
graph TB
    subgraph "Client Layer"
        WebApp[🌐 Web Application]
        MobileApp[📱 Mobile App]
        API[🔌 REST API Client]
    end
    
    subgraph "AWS Cloud"
        subgraph "Application Layer"
            ALB[⚖️ Application Load Balancer]
            EKS[☸️ EKS Cluster]
            
            subgraph "Image Service Pod"
                ImageAPI[🍃 Spring Boot Image Service]
                Actuator[📊 Health Checks]
            end
        end
        
        subgraph "Storage Layer"
            S3Primary[🪣 S3 Images Bucket<br/>SSE-KMS Encrypted]
            S3Thumbnails[🖼️ S3 Thumbnails Bucket<br/>SSE-S3 Encrypted]
            S3Versions[📚 S3 Versioning<br/>Lifecycle Policy]
        end
        
        subgraph "Search & Metadata"
            OpenSearch[🔍 OpenSearch<br/>Image Metadata]
            RDS[🗄️ RDS PostgreSQL<br/>Image Catalog]
        end
        
        subgraph "Security & Access"
            KMS[🔑 KMS Customer Key]
            IAM[👤 IAM Roles]
            CloudFront[🌍 CloudFront CDN<br/>Signed URLs]
        end
        
        subgraph "Monitoring"
            CloudWatch[📈 CloudWatch Logs]
            XRay[🔍 X-Ray Tracing]
        end
    end
    
    WebApp --> ALB
    MobileApp --> ALB
    API --> ALB
    
    ALB --> ImageAPI
    ImageAPI --> S3Primary
    ImageAPI --> S3Thumbnails
    ImageAPI --> OpenSearch
    ImageAPI --> RDS
    
    S3Primary --> KMS
    S3Thumbnails --> KMS
    ImageAPI --> IAM
    
    CloudFront --> S3Primary
    CloudFront --> S3Thumbnails
    
    ImageAPI --> CloudWatch
    ImageAPI --> XRay
```

## 🔒 Security Architecture

```mermaid
graph TB
    subgraph "Security Perimeter"
        Internet[🌍 Internet] --> WAF[🛡️ AWS WAF]
        WAF --> CloudFront[🌍 CloudFront]
        CloudFront --> ALB[⚖️ ALB]
        
        subgraph "Private Subnet"
            ALB --> ImageService[🍃 Image Service]
            ImageService --> S3[🪣 Private S3 Buckets]
            ImageService --> RDS[🗄️ Private RDS]
        end
        
        subgraph "Encryption Layer"
            S3 --> KMS[🔑 Customer Managed KMS]
            RDS --> KMSRds[🔑 RDS KMS Key]
        end
        
        subgraph "Access Control"
            ImageService --> IAMRole[👤 EKS Service Account]
            IAMRole --> S3Policy[📋 S3 Bucket Policy]
            S3Policy --> IPWhitelist[🌐 IP Whitelist]
        end
    end
```

## 📊 Datenfluss-Diagramm

```mermaid
sequenceDiagram
    participant Client
    participant ImageAPI
    participant S3Primary
    participant S3Thumbnails
    participant OpenSearch
    participant RDS
    
    Note over Client,RDS: Bild Upload Flow
    Client->>ImageAPI: POST /images (multipart)
    ImageAPI->>ImageAPI: Validate & Resize
    ImageAPI->>S3Primary: Upload Original (KMS)
    ImageAPI->>S3Thumbnails: Upload Thumbnail (SSE-S3)
    ImageAPI->>RDS: Store Metadata
    ImageAPI->>OpenSearch: Index for Search
    ImageAPI-->>Client: 201 Created + Image ID
    
    Note over Client,RDS: Bild Search Flow
    Client->>ImageAPI: GET /images/search?q=landscape
    ImageAPI->>OpenSearch: Search Query
    OpenSearch-->>ImageAPI: Matching Image IDs
    ImageAPI->>RDS: Get Metadata
    ImageAPI-->>Client: Search Results + Signed URLs
    
    Note over Client,RDS: Bild Delete Flow
    Client->>ImageAPI: DELETE /images/{id}
    ImageAPI->>S3Primary: Delete Object (Version Marker)
    ImageAPI->>S3Thumbnails: Delete Thumbnail
    ImageAPI->>RDS: Mark as Deleted
    ImageAPI->>OpenSearch: Remove from Index
    ImageAPI-->>Client: 204 No Content
```

## 🏗️ Kubernetes Deployment

```mermaid
graph TB
    subgraph "EKS Cluster"
        subgraph "Namespace: image-service"
            subgraph "Image Service Pod"
                Container[🍃 Spring Boot Container]
                SidecarLog[📊 Fluent Bit Sidecar]
            end
            
            ConfigMap[⚙️ ConfigMap<br/>S3 Bucket Names]
            Secret[🔐 Secret<br/>Database Credentials]
            ServiceAccount[👤 Service Account<br/>S3 + RDS Permissions]
        end
        
        Service[🌐 ClusterIP Service]
        Ingress[🚪 ALB Ingress]
    end
    
    Container --> ConfigMap
    Container --> Secret
    Container --> ServiceAccount
    Service --> Container
    Ingress --> Service
```

## 🔧 Technische Komponenten

### Storage Strategy

- **S3 Primary Bucket**: Original-Bilder mit KMS-Verschlüsselung
- **S3 Thumbnails Bucket**: Optimierte Vorschaubilder mit SSE-S3
- **Versioning**: Aktiviert für Wiederherstellung gelöschter Bilder
- **Lifecycle Policy**: Automatische Archivierung nach 90 Tagen

### Search & Metadata

- **OpenSearch**: Volltext-Suche in Bild-Metadaten und Tags
- **RDS PostgreSQL**: Strukturierte Metadaten und Beziehungen
- **Indexing Strategy**: Asynchrone Indizierung via SQS/Lambda

### Security Features

- **KMS Customer Managed Keys** für sensible Bilder
- **IAM Roles** mit Least-Privilege-Prinzip
- **Bucket Policies** mit IP-Whitelist und HTTPS-Only
- **CloudFront Signed URLs** für zeitbegrenzten Zugriff

### Performance Optimierung

- **CloudFront CDN** für globale Bildauslieferung
- **Multi-Size Thumbnails** (150px, 300px, 600px)
- **Lazy Loading** mit Progressive JPEG
- **Connection Pooling** für S3 und RDS
