#!/bin/bash

# 🗄️ MinIO Bucket Initialization
# This script creates the required S3 buckets in MinIO

echo "🚀 Setting up MinIO buckets..."

# Wait for MinIO to be ready
echo "⏳ Waiting for MinIO to start..."
sleep 5

# Configure MinIO client
echo "🔧 Configuring MinIO client..."
mc alias set minio http://minio:9000 devuser devpassword123

# Create buckets
echo "📦 Creating buckets..."
mc mb minio/dev-images-bucket --ignore-existing
mc mb minio/dev-thumbnails-bucket --ignore-existing

# Set bucket policies (both private)
echo "🔒 Setting bucket policies..."
# mc anonymous set public minio/dev-thumbnails-bucket  # Disabled - keeping private

echo "✅ MinIO setup complete!"

echo "📊 Available buckets:"
mc ls minio