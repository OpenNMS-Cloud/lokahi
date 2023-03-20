docker buildx build \
  --tag opennms/horizon-stream-minion-gateway:latest \
  ../minion/docker-assembly/target/docker/opennms/horizon-stream-minion/local/build/ \
  -f docker-assembly/src/main/docker/app/Dockerfile \
  --platform linux/amd64,linux/arm64,linux/arm/v7
