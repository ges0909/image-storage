#!/bin/bash
# LocalStack S3 Buckets erstellen
aws --endpoint-url=http://localhost:4566 s3 mb s3://images-bucket-dev
aws --endpoint-url=http://localhost:4566 s3 mb s3://thumbnails-bucket-dev
echo "S3 buckets created successfully"