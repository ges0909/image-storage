#!/bin/bash
# 🚀 Kubernetes Deployment Script

set -e

NAMESPACE="image-storage"
ENVIRONMENT=${1:-production}

echo "🚀 Deploying Image Storage to Kubernetes (${ENVIRONMENT})"

# Apply manifests in correct order
echo "📦 Creating namespace..."
kubectl apply -f namespace.yaml

echo "🔐 Creating RBAC..."
kubectl apply -f rbac.yaml

echo "⚙️ Creating ConfigMap..."
kubectl apply -f configmap.yaml

echo "🔑 Creating Secrets..."
kubectl apply -f secret.yaml

echo "🗄️ Creating PostgreSQL..."
kubectl apply -f postgres.yaml

echo "🌐 Creating NetworkPolicy..."
kubectl apply -f networkpolicy.yaml

echo "🚀 Creating Deployment..."
kubectl apply -f deployment.yaml

echo "🔗 Creating Service..."
kubectl apply -f service.yaml

echo "🌍 Creating Ingress..."
kubectl apply -f ingress.yaml

echo "📊 Creating HPA..."
kubectl apply -f hpa.yaml

echo "🛡️ Creating PodDisruptionBudget..."
kubectl apply -f pdb.yaml

echo "⏳ Waiting for PostgreSQL to be ready..."
kubectl rollout status deployment/postgres -n ${NAMESPACE} --timeout=120s

echo "⏳ Waiting for deployment to be ready..."
kubectl rollout status deployment/image-storage-app -n ${NAMESPACE} --timeout=300s

echo "✅ Deployment completed successfully!"
echo "🔍 Check status with: kubectl get all -n ${NAMESPACE}"
echo "📊 View logs with: kubectl logs -f deployment/image-storage-app -n ${NAMESPACE}"