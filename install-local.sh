#!/usr/bin/env bash
#use this script to install a basic version of OpenNMS Horizon Stream locally


operator_run () {
  bash scripts/add-local-ssl-cert.sh
  kubectl config set-context --current --namespace=local-instance

  # This is the last pod to run, if ready, then give back the terminal session.
  while [ -v $(kubectl get pods -n local-instance -l=app.kubernetes.io/component='controller-local-instance' -o jsonpath='{.items[*].status.containerStatuses[0].ready}')  ]; do 
    echo "not-ready"
    sleep 60
  done
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
  cat install-local-onms-instance.yaml| sed "s/onmshs/$2/g" > tmp/install-local-onms-instance.yaml
fi

if [ $1 == "local" ]; then

  cd operator/
  bash scripts/create-kind-cluster.sh

  echo
  echo ________________Installing Operator________________
  echo
  helm upgrade -i operator-local ../charts/opennms-operator -f ../install-local-operator-values.yaml --namespace opennms --create-namespace
  if [ $? -ne 0 ]; then exit; fi

  echo
  echo ____________Installing Local Instance______________
  echo
  kubectl apply -f ../tmp/install-local-onms-instance.yaml
  if [ $? -ne 0 ]; then exit; fi

  operator_run

elif [ "$1" == "custom-images" ]; then

  cd operator/
  bash scripts/create-kind-cluster.sh

  # Will add a kind-registry here at some point, see .github/ for sample script.
  kind load docker-image opennms/operator:local-build&
  kind load docker-image opennms/horizon-stream-core:local&
  kind load docker-image opennms/horizon-stream-minion:local&
  kind load docker-image opennms/horizon-stream-minion-gateway:local&
  kind load docker-image opennms/horizon-stream-keycloak:local&
  kind load docker-image opennms/horizon-stream-grafana:local&
  kind load docker-image opennms/horizon-stream-ui:local&
  kind load docker-image opennms/horizon-stream-notification:local&
  kind load docker-image opennms/horizon-stream-rest-server:local&
  kind load docker-image opennms/horizon-stream-inventory:local&
  kind load docker-image opennms/horizon-stream-metrics-processor:local&

  # Need to wait for the images to be loaded.
  sleep 120

  echo
  echo ________________Installing Operator________________
  echo
  helm upgrade -i operator-local ../charts/opennms-operator -f scripts/local-operator-values.yaml --namespace opennms --create-namespace
  if [ $? -ne 0 ]; then exit; fi

  echo
  echo ____________Installing Local Instance______________
  echo
  kubectl apply -f ../tmp/install-local-onms-instance-custom-images.yaml
  if [ $? -ne 0 ]; then exit; fi

  operator_run

elif [ $1 == "existing-k8s" ]; then

  cd operator/

  echo
  echo ________________Installing Operator________________
  echo
  helm upgrade -i operator-local ../charts/opennms-operator -f ../install-local-operator-values.yaml --namespace opennms --create-namespace
  if [ $? -ne 0 ]; then exit; fi

  echo
  echo ____________Installing Local Instance______________
  echo
  kubectl apply -f ../tmp/install-local-onms-instance.yaml
  if [ $? -ne 0 ]; then exit; fi

  operator_run

elif [ $1 == "existing-k8s-no-op" ]; then

  echo
  echo ____________Installing HS Instance______________
  echo
  helm upgrade -i horizon-stream charts/opennms -f ./tmp/install-local-onms-instance.yaml --namespace hs-instance --create-namespace

else
  echo "$HELP"
fi

bash scripts/add-local-ssl-cert.sh

kubectl config set-context --current --namespace=local-instance

# This is the last pod to run, if ready, then give back the terminal session.
JOB_FAIL=1
while [ -v $(kubectl get pods -n local-instance -l=app.kubernetes.io/component='controller-local-instance' -o jsonpath='{.items[*].status.containerStatuses[0].ready}')  ]; do 
  echo "not-ready"
  if [ $JOB_FAIL == 8 ]; then
    exit 1;
  else
    JOB_FAIL=$((JOB_FAIL+1))
  fi
  sleep 60
done
