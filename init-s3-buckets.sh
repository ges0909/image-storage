#!/bin/bash
# LocalStack S3 Buckets automatisch erstellen
echo "Creating S3 buckets..."

awslocal s3 mb s3://images-bucket-dev
awslocal s3 mb s3://thumbnails-bucket-dev

echo "S3 buckets created successfully!"
echo "Available buckets:"
awslocal s3 ls