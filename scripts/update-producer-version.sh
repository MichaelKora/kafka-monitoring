#!/bin/bash

if [ -z "$1" ]; then
  echo "Usage: $0 <VERSION>"
  exit 1
fi

VERSION="$1"

# Build producer app and push to registry
docker build -t avarange/pj-ds-producer:"$VERSION" -f ./producerapp/Dockerfile ./producerapp
docker push avarange/pj-ds-producer:"$VERSION"

# Apply new version to cluster
cd deployment
helmfile apply
