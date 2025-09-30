#!/bin/bash

# 🪣 LocalStack S3 Bucket Initialization
# This script runs automatically when LocalStack is ready

echo "🚀 Creating S3 buckets..."

# Create buckets
awslocal s3 mb s3://dev-images-bucket
awslocal s3 mb s3://dev-thumbnails-bucket

echo "✅ S3 buckets created successfully!"

echo "Available buckets:"
awslocal s3 ls
