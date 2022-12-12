#!/bin/bash
########################################################################################################################
##
## DESCRIPTION:
##	Stop all of the application pods that are under development.  Leave all of the pods that are not under
##	development, such as Postgres and Grafana.
##
##	This is helpful in reducing startup costs of the entire application while minimizing the risk of missing code
##	updates.
##
########################################################################################################################

kubectl delete deployment opennms-notifications
kubectl delete deployment opennms-rest-server
kubectl delete deployment opennms-core
kubectl delete deployment opennms-minion
kubectl delete deployment opennms-ui
kubectl delete deployment opennms-minion-gateway
kubectl delete deployment opennms-minion-gateway-grpc-proxy
kubectl delete deployment opennms-metrics-processor
kubectl delete deployment opennms-events
kubectl delete deployment opennms-datachoices

