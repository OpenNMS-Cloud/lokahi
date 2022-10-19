#!/bin/sh

helm upgrade -i opennms ./charts/opennms -f build-tools/basic/run/helm-values.yaml \
  --set OpenNMS.Core.Image=opennms/horizon-stream-core:local-basic \
  --set OpenNMS.API.Image=opennms/horizon-stream-rest-server:local-basic \
  --set OpenNMS.UI.Image=opennms/horizon-stream-ui:local-basic \
  --set OpenNMS.Notification.Image=opennms/horizon-stream-notification:local-basic \
  --set OpenNMS.Minion.Image=opennms/horizon-stream-minion:local-basic \
  --set OpenNMS.MinionGateway.Image=opennms/horizon-stream-minion-gateway:local-basic \
  --set Keycloak.Image=opennms/horizon-stream-keycloak-dev:local-basic \
  --set Grafana.Image=opennms/horizon-stream-grafana-dev:local-basic
