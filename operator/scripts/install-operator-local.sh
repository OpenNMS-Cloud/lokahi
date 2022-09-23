#!/usr/bin/env bash
#use this script to install a basic version of the opennms operator locally

echo __________________Installing OLM___________________
echo
operator-sdk olm install --version v0.21.2

echo
echo _______________Building Docker Image_______________
echo
make local-docker
if [ $? -ne 0 ]; then exit; fi

echo
echo ______Pushing Docker Images into Kind Cluster______
echo
kind load docker-image opennms/operator:local-build

echo
echo ___________Installing Helm Dependencies____________
echo
bash scripts/install-helm-deps-local.sh
if [ $? -ne 0 ]; then exit; fi

echo
echo _______________Wait For Dependencies_______________
echo
until kubectl wait -n kafka --for=condition=Ready=true pod -l name=strimzi-cluster-operator --timeout=90s 2> /dev/null
do
    sleep 5
    echo Waiting for dependencies to start....
done
if [ $? -ne 0 ]; then exit; fi
echo Dependencies started.

echo
echo ________________Installing Operator________________
echo
helm upgrade -i operator-local ../charts/opennms-operator -f scripts/local-operator-values.yaml --namespace opennms --create-namespace
if [ $? -ne 0 ]; then exit; fi
