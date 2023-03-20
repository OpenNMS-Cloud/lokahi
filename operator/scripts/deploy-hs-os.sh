#!/usr/bin/env bash

bash scripts/install-operator-os.sh

bash scripts/create-os-instance.sh

kubectl config set-context --current --namespace=opennms

kubectl delete pods -n opennms --all

