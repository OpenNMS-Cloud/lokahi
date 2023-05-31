#!/usr/bin/env bash
#use this script to install a basic version of OpenNMS Horizon Stream locally

bash scripts/create-kind-cluster.sh

bash scripts/install-operator-local.sh

bash scripts/create-instance.sh

bash scripts/add-local-ssl-cert.sh

kubectl config set-context --current --namespace=opennms-instance

# This is the last pod to run, if ready, then give back the terminal session.
while [ -v $(kubectl get pods -n opennms-instance -l=app.kubernetes.io/component='controller-local-instance' -o jsonpath='{.items[*].status.containerStatuses[0].ready}')  ]; do
  echo "not-ready"
  sleep 60
done
