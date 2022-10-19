#!/usr/bin/env bash
#use this script to install a basic version of OpenNMS Horizon Stream locally

cd operator/

bash scripts/create-kind-cluster.sh

echo
echo ________________Installing Operator________________
echo
helm upgrade -i operator-local ../charts/opennms-operator -f ../install-local-operator-values.yaml --namespace opennms --create-namespace
if [ $? -ne 0 ]; then exit; fi

bash scripts/create-instance.sh

bash scripts/add-local-ssl-cert.sh

kubectl config set-context --current --namespace=local-instance

# This is the last pod to run, if ready, then give back the terminal session.
while [ -v $(kubectl get pods -n local-instance -l=app.kubernetes.io/component='controller-local-instance' -o jsonpath='{.items[*].status.containerStatuses[0].ready}')  ]; do 
  echo "not-ready"
  sleep 60
done
