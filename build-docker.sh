#!/bin/bash
# 🐳 Docker Build Script

set -e

APP_NAME="image-storage"
VERSION=${1:-latest}
REGISTRY=${REGISTRY:-""}

echo "🏗️  Building Docker image: ${APP_NAME}:${VERSION}"

# Build the image
docker build -t ${APP_NAME}:${VERSION} .

# Tag for registry if specified
if [ ! -z "$REGISTRY" ]; then
    docker tag ${APP_NAME}:${VERSION} ${REGISTRY}/${APP_NAME}:${VERSION}
    echo "🏷️  Tagged: ${REGISTRY}/${APP_NAME}:${VERSION}"
fi

echo "✅ Build completed successfully!"
echo "🚀 Run with: docker run -p 8080:8080 ${APP_NAME}:${VERSION}"
echo "🐳 Or use: docker-compose up"