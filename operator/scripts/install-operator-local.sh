#!/usr/bin/env bash
#use this script to install a basic version of the opennms operator locally

echo
echo _______________Building Docker Image_______________
echo
make local-docker
if [ $? -ne 0 ]; then exit; fi

docker build -t opennms/lokahi-keycloak:latest -f ../keycloak-ui/Dockerfile ../keycloak-ui

echo
echo ______Pushing Docker Images into Kind Cluster______
echo
kind load docker-image opennms/operator:local-build
kind load docker-image opennms/lokahi-keycloak:latest

echo
echo ________________Installing Operator________________
echo
helm upgrade -i operator-local ../charts/opennms-operator -f scripts/local-operator-values.yaml --namespace opennms --create-namespace
if [ $? -ne 0 ]; then exit; fi
