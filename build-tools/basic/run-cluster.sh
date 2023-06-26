#!/bin/sh

helm upgrade -i lokahi ./charts/lokahi -f build-tools/basic/helm-values.yaml \
  --set Grafana.Image=opennms/lokahi-grafana:local-basic \
  --set Keycloak.Image=opennms/lokahi-keycloak:local-basic \
  --set OpenNMS.API.Image=opennms/lokahi-rest-server:local-basic \
  --set OpenNMS.Alert.Image=opennms/lokahi-alert:local-basic \
  --set OpenNMS.DataChoices.Image=opennms/lokahi-datachoices:local-basic \
  --set OpenNMS.Events.Image=opennms/lokahi-events:local-basic \
  --set OpenNMS.Inventory.Image=opennms/lokahi-inventory:local-basic \
  --set OpenNMS.MetricsProcessor.Image=opennms/lokahi-metrics-processor:local-basic \
  --set OpenNMS.Minion.Image=opennms/lokahi-minion:local-basic \
  --set OpenNMS.MinionGateway.Image=opennms/lokahi-minion-gateway:local-basic \
  --set OpenNMS.Notification.Image=opennms/lokahi-notification:local-basic \
  --set OpenNMS.MinionCertificateManager.Image=opennms/lokahi-minion-certificate-manager:local-basic \
  --set OpenNMS.MinionCertificateVerifier.Image=opennms/lokahi-minion-certificate-verifier:local-basic \
  --set OpenNMS.UI.Image=opennms/lokahi-ui:local-basic \
