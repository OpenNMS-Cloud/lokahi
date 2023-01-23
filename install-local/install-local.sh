#!/usr/bin/env bash
#use this script to install a basic version of OpenNMS Horizon Stream locally

set -e

#### ENV VARS
################################

# For local, we can setup localhost as the default on port 8080 or something.
# Like Skaffold and Tilt. 
# This determines whether or not to import custom images or not.
HELP='Need to pass "local" parameter to script'
if [[ -z "$2" || -z "$1" ]]; then
  echo "Need to add custom DNS for the second parameter, domain to use."
  echo "$HELP"
  exit 1
fi

CONTEXT=$1
DOMAIN=$2
IMAGE_TAG=${3:-local}
IMAGE_PREFIX=${4:-opennms}
KIND_CLUSTER_NAME=kind-test
NAMESPACE=hs-instance

#### FUNCTION DEF
################################

create_cluster() {
  echo
  echo ______________Creating Kind Cluster________________
  echo
  kind create cluster --name $KIND_CLUSTER_NAME --config=./install-local-kind-config.yaml
  kubectl config use-context "kind-$KIND_CLUSTER_NAME"
  kubectl config get-contexts
}

cluster_ready_check () {

  # This is the last pod to run, if ready, then give back the terminal session.
  sleep 60 # Need to wait until the pod is created or else nothing comes back. Messes with the conditional.
  while [[ $(kubectl get pods -n $NAMESPACE -l=app.kubernetes.io/component="controller-$NAMESPACE" -o jsonpath='{.items[*].status.containerStatuses[0].ready}') == 'false' ]]; do
    echo "not-ready"
    sleep 30
  done

}

create_ssl_cert_secret () {

  # Create SSL Certificate
  openssl genrsa -out tmp/ca.key
  openssl req -new -x509 -days 365 -key tmp/ca.key -subj "/CN=$DOMAIN/O=Test Keycloak./C=US" -out tmp/ca.crt
  openssl req -newkey rsa:2048 -nodes -keyout tmp/server.key -subj "/CN=$DOMAIN/O=Test Keycloak./C=US" -out tmp/server.csr
  openssl x509 -req -extfile <(printf "subjectAltName=DNS:$DOMAIN") -days 365 -in tmp/server.csr -CA tmp/ca.crt -CAkey tmp/ca.key -CAcreateserial -out tmp/server.crt
  if [ $? -ne 0 ]; then exit; fi
  kubectl -n $NAMESPACE delete secrets/tls-cert-wildcard
  kubectl -n $NAMESPACE create secret tls tls-cert-wildcard --cert tmp/server.crt --key tmp/server.key
  if [ $? -ne 0 ]; then exit; fi

}

load_images () {
    kind load docker-image --name $KIND_CLUSTER_NAME ${IMAGE_PREFIX}/horizon-stream-alarm:${IMAGE_TAG} &
    kind load docker-image --name $KIND_CLUSTER_NAME ${IMAGE_PREFIX}/horizon-stream-datachoices:${IMAGE_TAG} &
    kind load docker-image --name $KIND_CLUSTER_NAME ${IMAGE_PREFIX}/horizon-stream-events:${IMAGE_TAG} &
    kind load docker-image --name $KIND_CLUSTER_NAME ${IMAGE_PREFIX}/horizon-stream-grafana:${IMAGE_TAG} &
    kind load docker-image --name $KIND_CLUSTER_NAME ${IMAGE_PREFIX}/horizon-stream-inventory:${IMAGE_TAG} &
    kind load docker-image --name $KIND_CLUSTER_NAME ${IMAGE_PREFIX}/horizon-stream-keycloak:${IMAGE_TAG} &
    kind load docker-image --name $KIND_CLUSTER_NAME ${IMAGE_PREFIX}/horizon-stream-metrics-processor:${IMAGE_TAG} &
    kind load docker-image --name $KIND_CLUSTER_NAME ${IMAGE_PREFIX}/horizon-stream-minion:${IMAGE_TAG} &
    kind load docker-image --name $KIND_CLUSTER_NAME ${IMAGE_PREFIX}/horizon-stream-minion-gateway:${IMAGE_TAG} &
    kind load docker-image --name $KIND_CLUSTER_NAME ${IMAGE_PREFIX}/horizon-stream-minion-gateway-grpc-proxy:${IMAGE_TAG} &
    kind load docker-image --name $KIND_CLUSTER_NAME ${IMAGE_PREFIX}/horizon-stream-notification:${IMAGE_TAG} &
    kind load docker-image --name $KIND_CLUSTER_NAME ${IMAGE_PREFIX}/horizon-stream-rest-server:${IMAGE_TAG} &
    kind load docker-image --name $KIND_CLUSTER_NAME ${IMAGE_PREFIX}/horizon-stream-ui:${IMAGE_TAG} &
}

install_helm_chart_custom_images () {
  echo
  echo ________________Installing Horizon Stream________________
  echo

  helm upgrade -i horizon-stream ./../charts/opennms \
  -f ./tmp/install-local-opennms-horizon-stream-custom-images-values.yaml \
  --namespace $NAMESPACE --create-namespace \
  --set OpenNMS.Alarm.Image=${IMAGE_PREFIX}/horizon-stream-alarm:${IMAGE_TAG} \
  --set OpenNMS.DataChoices.Image=${IMAGE_PREFIX}/horizon-stream-datachoices:${IMAGE_TAG} \
  --set OpenNMS.Events.Image=${IMAGE_PREFIX}/horizon-stream-events:${IMAGE_TAG} \
  --set Grafana.Image=${IMAGE_PREFIX}/horizon-stream-grafana-dev:${IMAGE_TAG} \
  --set OpenNMS.Inventory.Image=${IMAGE_PREFIX}/horizon-stream-inventory:${IMAGE_TAG} \
  --set Keycloak.Image=${IMAGE_PREFIX}/horizon-stream-keycloak-dev:${IMAGE_TAG} \
  --set OpenNMS.MetricsProcessor.Image=${IMAGE_PREFIX}/horizon-stream-metrics-processor:${IMAGE_TAG} \
  --set OpenNMS.Minion.Image=${IMAGE_PREFIX}/horizon-stream-minion:${IMAGE_TAG} \
  --set OpenNMS.MinionGateway.Image=${IMAGE_PREFIX}/horizon-stream-minion-gateway:${IMAGE_TAG} \
  --set OpenNMS.MinionGatewayGrpcProxy.Image=${IMAGE_PREFIX}/horizon-stream-minion-gateway-grpc-proxy:${IMAGE_TAG} \
  --set OpenNMS.Notification.Image=${IMAGE_PREFIX}/horizon-stream-notification:${IMAGE_TAG} \
  --set OpenNMS.API.Image=${IMAGE_PREFIX}/horizon-stream-rest-server:${IMAGE_TAG} \
  --set OpenNMS.UI.Image=${IMAGE_PREFIX}/horizon-stream-ui:${IMAGE_TAG}
}

#### MAIN
################################

# Swap Domain in YAML files
mkdir -p tmp
cat install-local-onms-instance.yaml | sed "s/onmshs/$DOMAIN/g" > tmp/install-local-onms-instance.yaml
cat install-local-onms-instance-custom-images.yaml | sed "s/onmshs/$DOMAIN/g" > tmp/install-local-onms-instance-custom-images.yaml
cat ./../charts/opennms/values.yaml | sed "s/onmshs/$DOMAIN/g" > tmp/values.yaml
cat install-local-opennms-horizon-stream-values.yaml | sed "s/onmshs/$DOMAIN/g" > tmp/install-local-opennms-horizon-stream-values.yaml
cat install-local-opennms-horizon-stream-custom-images-values.yaml | sed "s/onmshs/$DOMAIN/g" > tmp/install-local-opennms-horizon-stream-custom-images-values.yaml

# Select Context, Create Cluster, and Deploy
if [ $CONTEXT == "local" ]; then

  create_cluster

  echo
  echo ________________Installing Horizon Stream________________
  echo
  helm upgrade -i horizon-stream ./../charts/opennms -f ./tmp/install-local-opennms-horizon-stream-values.yaml --namespace $NAMESPACE --create-namespace
  if [ $? -ne 0 ]; then exit; fi

  create_ssl_cert_secret
  cluster_ready_check

elif [ "$CONTEXT" == "custom-images" ]; then

  create_cluster

  load_images

  # Need to wait for the images to be loaded.
  sleep 120

  install_helm_chart_custom_images

  if [ $? -ne 0 ]; then exit; fi

  create_ssl_cert_secret
  cluster_ready_check

elif [ "$CONTEXT" == "cicd" ]; then

  create_cluster

  # assumes remote docker registry, no need to load images into cluster
  install_helm_chart_custom_images

  # output values from the release to help with debugging pipelines
  helm get values horizon-stream --namespace $NAMESPACE

  if [ $? -ne 0 ]; then exit; fi

  create_ssl_cert_secret
  cluster_ready_check

elif [ $CONTEXT == "existing-k8s" ]; then

  echo
  echo ________________Installing Horizon Stream________________
  echo
  helm upgrade -i horizon-stream ./../charts/opennms -f ./tmp/install-local-opennms-horizon-stream-values.yaml --namespace $NAMESPACE --create-namespace
  if [ $? -ne 0 ]; then exit; fi

  cluster_ready_check

else
  echo "$HELP"
fi
