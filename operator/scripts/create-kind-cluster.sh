#!/usr/bin/env bash

echo
echo ______________Creating Kind Cluster________________
echo
kind create cluster --config=scripts/kind-config.yaml
kubectl config use-context kind-kind
kubectl config get-contexts
