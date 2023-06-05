docker buildx build \
  --tag opennms/lokahi-minion-gateway:latest \
  ../minion/docker-assembly/target/docker/opennms/lokahi-minion/local/build/ \
  -f docker-assembly/src/main/docker/app/Dockerfile \
  --platform linux/amd64,linux/arm64,linux/arm/v7
