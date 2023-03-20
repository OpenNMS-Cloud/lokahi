#!/bin/bash

trap stop EXIT INT

stop ()
{
	echo "Shutting down"
	kill $(jobs -p)
}

kubectl --context kind-kind port-forward --pod-running-timeout 1s --namespace default deployment/ingress-nginx-controller 8123:80 &
kubectl --context kind-kind port-forward --pod-running-timeout 1s --namespace default deployment/opennms-inventory 29080:8080 &
kubectl --context kind-kind port-forward --pod-running-timeout 1s --namespace default deployment/opennms-inventory 29050:5005 &
kubectl --context kind-kind port-forward --pod-running-timeout 1s --namespace default deployment/opennms-alert 32080:8080 &
kubectl --context kind-kind port-forward --pod-running-timeout 1s --namespace default deployment/opennms-alert 32050:5005 &
kubectl --context kind-kind port-forward --pod-running-timeout 1s --namespace default deployment/opennms-notifications 15050:5005 &
kubectl --context kind-kind port-forward --pod-running-timeout 1s --namespace default deployment/opennms-minion 12080:8181 &
kubectl --context kind-kind port-forward --pod-running-timeout 1s --namespace default deployment/opennms-minion 12022:8102 &
kubectl --context kind-kind port-forward --pod-running-timeout 1s --namespace default deployment/opennms-minion 12050:5005 &
kubectl --context kind-kind port-forward --pod-running-timeout 1s --namespace default deployment/onms-keycloak 26080:8080 &
kubectl --context kind-kind port-forward --pod-running-timeout 1s --namespace default deployment/mail-server 22080:8025 &
kubectl --context kind-kind port-forward --pod-running-timeout 1s --namespace default deployment/mail-server 22025:1025 &
kubectl --context kind-kind port-forward --pod-running-timeout 1s --namespace default deployment/postgres 25054:5432 &
kubectl --context kind-kind port-forward --pod-running-timeout 1s --namespace default deployment/grafana 18080:3000 &
kubectl --context kind-kind port-forward --pod-running-timeout 1s --namespace default deployment/onms-kafka 59092:59092 &
kubectl --context kind-kind port-forward --pod-running-timeout 1s --namespace default deployment/ingress-nginx-controller 8123:80 &
kubectl --context kind-kind port-forward --pod-running-timeout 1s --namespace default deployment/opennms-metrics-processor 28050:5005 &
kubectl --context kind-kind port-forward --pod-running-timeout 1s --namespace default deployment/opennms-events 30050:5005 &
kubectl --context kind-kind port-forward --pod-running-timeout 1s --namespace default deployment/opennms-datachoices 33080:8080 &
kubectl --context kind-kind port-forward --pod-running-timeout 1s --namespace default deployment/opennms-datachoices 33050:5005 &

while wait
do
	:
done
