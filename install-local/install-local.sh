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

  # Will add a kind-registry here at some point, see .github/ for sample script.
  kind load docker-image --name kind-test opennms/horizon-stream-alarm:local&
  kind load docker-image --name kind-test opennms/horizon-stream-core:local&
  kind load docker-image --name kind-test opennms/horizon-stream-minion:local&
  kind load docker-image --name kind-test opennms/horizon-stream-minion-gateway:local&
  kind load docker-image --name kind-test opennms/horizon-stream-minion-gateway-grpc-proxy:local&
  kind load docker-image --name kind-test opennms/horizon-stream-keycloak:local&
  kind load docker-image --name kind-test opennms/horizon-stream-grafana:local&
  kind load docker-image --name kind-test opennms/horizon-stream-ui:local&
  kind load docker-image --name kind-test opennms/horizon-stream-notification:local&
  kind load docker-image --name kind-test opennms/horizon-stream-rest-server:local&
  kind load docker-image --name kind-test opennms/horizon-stream-inventory:local&
  kind load docker-image --name kind-test opennms/horizon-stream-metrics-processor:local&
  kind load docker-image --name kind-test opennms/horizon-stream-events:local&
  kind load docker-image --name kind-test opennms/horizon-stream-datachoices:local&

  # Need to wait for the images to be loaded.
  sleep 120

  echo
  echo ________________Installing Horizon Stream________________
  echo
  helm upgrade -i horizon-stream ./../charts/opennms -f ./tmp/install-local-opennms-horizon-stream-custom-images-values.yaml --namespace $NAMESPACE --create-namespace
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
