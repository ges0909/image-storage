# 🚀 Deployment Diagramme

## 🏗️ AWS Infrastructure

```mermaid
graph TB
    subgraph "AWS Account"
        subgraph "VPC"
            subgraph "Public Subnets"
                ALB[⚖️ Application Load Balancer]
                NAT[🌐 NAT Gateway]
            end
            
            subgraph "Private Subnets"
                EKS[☸️ EKS Worker Nodes]
                RDS[🗄️ RDS PostgreSQL]
            end
        end
        
        subgraph "S3 Storage"
            S3Images[🪣 images-bucket-prod]
            S3Thumbs[🖼️ thumbnails-bucket-prod]
        end
        
        subgraph "Security Services"
            KMS[🔑 Customer Managed KMS]
            IAM[👤 IAM Roles & Policies]
        end
        
        subgraph "Monitoring"
            CloudWatch[📈 CloudWatch]
            OpenSearch[🔍 OpenSearch Service]
        end
        
        CloudFront[🌍 CloudFront Distribution]
    end
    
    Internet[🌍 Internet] --> CloudFront
    CloudFront --> ALB
    ALB --> EKS
    EKS --> RDS
    EKS --> S3Images
    EKS --> S3Thumbs
    EKS --> OpenSearch
    
    S3Images --> KMS
    S3Thumbs --> KMS
    RDS --> KMS
```

## ☸️ Kubernetes Resources

```mermaid
graph TB
    subgraph "Namespace: image-service"
        subgraph "Workloads"
            Deployment[📦 image-service-deployment]
            ReplicaSet[🔄 ReplicaSet (3 replicas)]
            
            subgraph "Pod Template"
                Container[🍃 image-service:v1.0.0]
                InitContainer[🔧 migration-init]
                SidecarLog[📊 fluent-bit]
            end
        end
        
        subgraph "Configuration"
            ConfigMap[⚙️ image-service-config]
            Secret[🔐 image-service-secrets]
            ServiceAccount[👤 image-service-sa]
        end
        
        subgraph "Networking"
            Service[🌐 image-service-svc]
            Ingress[🚪 image-service-ingress]
        end
        
        subgraph "Storage"
            PVC[💾 image-cache-pvc]
        end
    end
    
    Deployment --> ReplicaSet
    ReplicaSet --> Container
    Container --> ConfigMap
    Container --> Secret
    Container --> ServiceAccount
    Container --> PVC
    Service --> Container
    Ingress --> Service
```

## 🔄 CI/CD Pipeline

```mermaid
graph LR
    subgraph "Development"
        Dev[👨‍💻 Developer]
        Git[📚 Git Repository]
    end
    
    subgraph "CI Pipeline"
        Build[🔨 Maven Build]
        Test[🧪 Unit Tests]
        Integration[🔗 Integration Tests]
        Security[🔒 Security Scan]
        Package[📦 Docker Build]
    end
    
    subgraph "CD Pipeline"
        Registry[📋 ECR Registry]
        Deploy[🚀 Helm Deploy]
        Verify[✅ Health Check]
    end
    
    subgraph "Environments"
        Dev_Env[🧪 Development]
        Staging[🎭 Staging]
        Prod[🏭 Production]
    end
    
    Dev --> Git
    Git --> Build
    Build --> Test
    Test --> Integration
    Integration --> Security
    Security --> Package
    Package --> Registry
    Registry --> Deploy
    Deploy --> Dev_Env
    Dev_Env --> Staging
    Staging --> Prod
    Prod --> Verify
```