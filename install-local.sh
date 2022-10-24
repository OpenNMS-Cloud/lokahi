#!/usr/bin/env bash
#use this script to install a basic version of OpenNMS Horizon Stream locally

cd operator/

# This determines whether or not to import custom images or not.
HELP='Need to pass "local" parameter to script'
if [ -z "$1" ]; then
  echo "$HELP"
else
  if [ $1 == "local" ]; then

    bash scripts/create-kind-cluster.sh

    echo
    echo ________________Installing Operator________________
    echo
    helm upgrade -i operator-local ../charts/opennms-operator -f ../install-local-operator-values.yaml --namespace opennms --create-namespace
    if [ $? -ne 0 ]; then exit; fi

    bash scripts/create-instance.sh
  
  elif [ "$1" == "custom-images" ]; then

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
    kind load docker-image opennms/horizon-stream-metric-processor:local&

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
    kubectl apply -f ../install-local-onms-instance.yaml
    if [ $? -ne 0 ]; then exit; fi

  elif [ $1 == "existing-k8s" ]; then

    echo
    echo ________________Installing Operator________________
    echo
    helm upgrade -i operator-local ../charts/opennms-operator -f ../install-local-operator-values.yaml --namespace opennms --create-namespace
    if [ $? -ne 0 ]; then exit; fi

    bash scripts/create-instance.sh
  
  else
    echo "$HELP"
  fi
fi

bash scripts/add-local-ssl-cert.sh

kubectl config set-context --current --namespace=local-instance

# This is the last pod to run, if ready, then give back the terminal session.
while [ -v $(kubectl get pods -n local-instance -l=app.kubernetes.io/component='controller-local-instance' -o jsonpath='{.items[*].status.containerStatuses[0].ready}')  ]; do 
  echo "not-ready"
  sleep 60
done

