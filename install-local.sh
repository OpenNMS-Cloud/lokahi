#!/usr/bin/env bash
#use this script to install a basic version of OpenNMS Horizon Stream locally

cluster_ready_check () {
  #bash scripts/add-local-ssl-cert.sh
  kubectl config set-context --current --namespace=hs-instance

  # This is the last pod to run, if ready, then give back the terminal session.
  sleep 60 # Need to wait until the pod is created or else nothing comes back. Messes with the conditional.
  while [[ $(kubectl get pods -n hs-instance -l=app.kubernetes.io/component='controller-hs-instance' -o jsonpath='{.items[*].status.containerStatuses[0].ready}') == 'false' ]]; do 
    echo "not-ready"
    sleep 30
  done

  kubectl config set-context --current --namespace=hs-instance
}

# For local, we can setup localhost as the default on port 8080 or something.
# Like Skaffold and Tilt. 
# This determines whether or not to import custom images or not.
HELP='Need to pass "local" parameter to script'
if [[ -z "$2" || -z "$1" ]]; then
  echo "Need to add custom DNS for the second parameter, domain to use."
  echo "$HELP"
  exit 1
else
  mkdir -p tmp
  cat install-local-onms-instance.yaml | sed "s/onmshs/$2/g" > tmp/install-local-onms-instance.yaml
  cat install-local-onms-instance-custom-images.yaml | sed "s/onmshs/$2/g" > tmp/install-local-onms-instance-custom-images.yaml
  cat charts/opennms/values.yaml | sed "s/onmshs/$2/g" > tmp/values.yaml
  cat install-local-opennms-horizon-stream-values.yaml | sed "s/onmshs/$2/g" > tmp/install-local-opennms-horizon-stream-values.yaml
  cat install-local-opennms-horizon-stream-custom-images-values.yaml | sed "s/onmshs/$2/g" > tmp/install-local-opennms-horizon-stream-custom-images-values.yaml
fi

if [ $1 == "local" ]; then

  cd operator/
  bash scripts/create-kind-cluster.sh

  echo
  echo ________________Installing Horizon Stream________________
  echo
  helm upgrade -i horizon-stream ../charts/opennms -f ../tmp/install-local-opennms-horizon-stream-values.yaml --namespace hs-instance --create-namespace
  if [ $? -ne 0 ]; then exit; fi

  cluster_ready_check

elif [ "$1" == "custom-images" ]; then

  cd operator/
  bash scripts/create-kind-cluster.sh

  # Will add a kind-registry here at some point, see .github/ for sample script.
  kind load docker-image opennms/horizon-stream-alarm:local&
  kind load docker-image opennms/horizon-stream-core:local&
  kind load docker-image opennms/horizon-stream-minion:local&
  kind load docker-image opennms/horizon-stream-minion-gateway:local&
  kind load docker-image opennms/horizon-stream-minion-gateway-grpc-proxy:local&
  kind load docker-image opennms/horizon-stream-keycloak:local&
  kind load docker-image opennms/horizon-stream-grafana:local&
  kind load docker-image opennms/horizon-stream-ui:local&
  kind load docker-image opennms/horizon-stream-notification:local&
  kind load docker-image opennms/horizon-stream-rest-server:local&
  kind load docker-image opennms/horizon-stream-inventory:local&
  kind load docker-image opennms/horizon-stream-metrics-processor:local&
  kind load docker-image opennms/horizon-stream-events:local&
  kind load docker-image opennms/horizon-stream-datachoices:local&

  # Need to wait for the images to be loaded.
  echo === SLEEP
  sleep 120
  echo === PS
  ps axu | grep kind
  echo === IMAGES
  docker exec -it kind-control-plane crictl images ls
  echo === SLEEP

  sleep 120
  echo === PS
  ps axu | grep kind
  echo === IMAGES
  docker exec -it kind-control-plane crictl images ls

  echo
  echo ________________Installing Horizon Stream________________
  echo
  helm upgrade -i horizon-stream ../charts/opennms -f ../tmp/install-local-opennms-horizon-stream-custom-images-values.yaml --namespace hs-instance --create-namespace
  if [ $? -ne 0 ]; then exit; fi

  cluster_ready_check

elif [ $1 == "existing-k8s" ]; then

  cd operator/

  echo
  echo ________________Installing Horizon Stream________________
  echo
  helm upgrade -i horizon-stream ../charts/opennms -f ../tmp/install-local-opennms-horizon-stream-values.yaml --namespace hs-instance --create-namespace
  if [ $? -ne 0 ]; then exit; fi

  cluster_ready_check

elif [ $1 == "existing-k8s-no-op" ]; then

  echo
  echo ____________Installing HS Instance______________
  echo
  helm upgrade -i horizon-stream charts/opennms -f ./tmp/values.yaml --namespace hs-instance --create-namespace

else
  echo "$HELP"
fi

#bash scripts/add-local-ssl-cert.sh

# This is the last pod to run, if ready, then give back the terminal session.
JOB_FAIL=1
while [ -v $(kubectl get pods -n hs-instance -l=app.kubernetes.io/component='controller-hs-instance' -o jsonpath='{.items[*].status.containerStatuses[0].ready}')  ]; do 
  echo "not-ready"
  if [ $JOB_FAIL == 8 ]; then
    exit 1;
  else
    JOB_FAIL=$((JOB_FAIL+1))
  fi
  sleep 60
done
