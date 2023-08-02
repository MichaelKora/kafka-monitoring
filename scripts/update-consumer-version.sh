#!/bin/bash

if [ -z "$1" ]; then
  echo "Usage: $0 <VERSION>"
  exit 1
fi

VERSION="$1"

# Build consumer app and push to registry
docker build -t avarange/pj-ds-consumer:"$VERSION" -f ./consumerapp/Dockerfile ./consumerapp
docker push avarange/pj-ds-consumer:"$VERSION"

# Apply new version to cluster
cd deployment
helmfile apply
