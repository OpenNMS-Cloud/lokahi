/*
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package crd

import (
	"github.com/OpenNMS/opennms-operator/api/v1alpha1"
	"github.com/OpenNMS/opennms-operator/internal/model/values"
)

// ConvertCRDToValues - convert an ONMS crd into a set of template values
func ConvertCRDToValues(crd v1alpha1.OpenNMS, defaultValues values.TemplateValues) values.TemplateValues {
	templateValues := defaultValues

	v := templateValues.Values
	r := templateValues.Release
	spec := crd.Spec

	r.Namespace = spec.Namespace
	v.Host = spec.Host
	v.TLS.Enabled = spec.TLSEnabled

	v.Ingress = setPorts(spec, v.Ingress)

	//ONMS API
	v.OpenNMS = getAPIValues(spec, v.OpenNMS)

	//ONMS UI
	v.OpenNMS = getUIValues(spec, v.OpenNMS)

	//ONMS Minion
	v.OpenNMS = getMinionValues(spec, v.OpenNMS)

	//ONMS Inventory
	v.OpenNMS = getInventoryValues(spec, v.OpenNMS)

	//ONMS Alarm
	v.OpenNMS = getAlarmValues(spec, v.OpenNMS)

	//ONMS Notification
	v.OpenNMS = getNotificationValues(spec, v.OpenNMS)

	//ONMS Metrics Processor
	v.OpenNMS = getMetricsProcessorValues(spec, v.OpenNMS)

	//ONMS Events Processor
	v.OpenNMS = getEventsValues(spec, v.OpenNMS)

	//ONMS Datachoices Processor
	v.OpenNMS = getDatachoicesValues(spec, v.OpenNMS)

	//Keycloak
	v.Keycloak = getKeycloakValues(spec, v.Keycloak)

	//Grafana
	v.Grafana = getGrafanaValues(spec, v.Grafana)

	//Postgres
	v.Postgres = getPostgresValues(spec, v.Postgres)

	templateValues.Values = v
	templateValues.Release = r

	return templateValues
}

// getAPIValues - get ONMS API values from the crd
func getAPIValues(spec v1alpha1.OpenNMSSpec, v values.OpenNMSValues) values.OpenNMSValues {
	if spec.API.Image != "" {
		v.API.Image = spec.API.Image
	}
	if spec.API.CPU != "" {
		v.API.Resources.Requests.Cpu = spec.API.CPU
		v.API.Resources.Limits.Cpu = spec.API.CPU
	}
	if spec.API.MEM != "" {
		v.API.Resources.Requests.Memory = spec.API.MEM
		v.API.Resources.Limits.Memory = spec.API.MEM
	}
	if spec.API.Disk != "" {
		v.API.VolumeSize = spec.API.Disk
	}
	return v
}

// getUIValues - get ONMS UI values from the crd
func getUIValues(spec v1alpha1.OpenNMSSpec, v values.OpenNMSValues) values.OpenNMSValues {
	if spec.UI.Image != "" {
		v.UI.Image = spec.UI.Image
	}
	if spec.UI.CPU != "" {
		v.UI.Resources.Requests.Cpu = spec.UI.CPU
		v.UI.Resources.Limits.Cpu = spec.UI.CPU
	}
	if spec.UI.MEM != "" {
		v.UI.Resources.Requests.Memory = spec.UI.MEM
		v.UI.Resources.Limits.Memory = spec.UI.MEM
	}
	if spec.UI.Disk != "" {
		v.UI.VolumeSize = spec.UI.Disk
	}
	return v
}

// getMinionValues - get ONMS Minion values from the crd
func getMinionValues(spec v1alpha1.OpenNMSSpec, v values.OpenNMSValues) values.OpenNMSValues {
	if spec.Minion.Image != "" {
		v.Minion.Image = spec.Minion.Image
	}
	if spec.Minion.CPU != "" {
		v.Minion.Resources.Requests.Cpu = spec.Minion.CPU
		v.Minion.Resources.Limits.Cpu = spec.Minion.CPU
	}
	if spec.Minion.MEM != "" {
		v.Minion.Resources.Requests.Memory = spec.Minion.MEM
		v.Minion.Resources.Limits.Memory = spec.Minion.MEM
	}
	if spec.Minion.Disk != "" {
		v.Minion.VolumeSize = spec.Minion.Disk
	}

	if spec.MinionGateway.Image != "" {
		v.MinionGateway.Image = spec.MinionGateway.Image
	}
	if spec.MinionGateway.CPU != "" {
		v.MinionGateway.Resources.Requests.Cpu = spec.MinionGateway.CPU
		v.MinionGateway.Resources.Limits.Cpu = spec.MinionGateway.CPU
	}
	if spec.MinionGateway.MEM != "" {
		v.MinionGateway.Resources.Requests.Memory = spec.MinionGateway.MEM
		v.MinionGateway.Resources.Limits.Memory = spec.MinionGateway.MEM
	}
	if spec.MinionGateway.Disk != "" {
		v.MinionGateway.VolumeSize = spec.MinionGateway.Disk
	}
	return v
}

// getInventoryValues - get ONMS Inventory values from the crd
func getInventoryValues(spec v1alpha1.OpenNMSSpec, v values.OpenNMSValues) values.OpenNMSValues {
	if spec.Inventory.Image != "" {
		v.Inventory.Image = spec.Inventory.Image
	}
	if spec.Inventory.CPU != "" {
		v.Inventory.Resources.Requests.Cpu = spec.Inventory.CPU
		v.Inventory.Resources.Limits.Cpu = spec.Inventory.CPU
	}
	if spec.Inventory.MEM != "" {
		v.Inventory.Resources.Requests.Memory = spec.Inventory.MEM
		v.Inventory.Resources.Limits.Memory = spec.Inventory.MEM
	}
	if spec.Inventory.Disk != "" {
		v.Inventory.VolumeSize = spec.Inventory.Disk
	}
	return v
}

// getAlarmValues - get ONMS Alarm values from the crd
func getAlarmValues(spec v1alpha1.OpenNMSSpec, v values.OpenNMSValues) values.OpenNMSValues {
	if spec.Alarm.Image != "" {
		v.Alarm.Image = spec.Alarm.Image
	}
	if spec.Alarm.CPU != "" {
		v.Alarm.Resources.Requests.Cpu = spec.Alarm.CPU
		v.Alarm.Resources.Limits.Cpu = spec.Alarm.CPU
	}
	if spec.Alarm.MEM != "" {
		v.Alarm.Resources.Requests.Memory = spec.Alarm.MEM
		v.Alarm.Resources.Limits.Memory = spec.Alarm.MEM
	}
	if spec.Alarm.Disk != "" {
		v.Alarm.VolumeSize = spec.Alarm.Disk
	}
	return v
}

// getNotificationValues - get ONMS Notification values from the crd
func getNotificationValues(spec v1alpha1.OpenNMSSpec, v values.OpenNMSValues) values.OpenNMSValues {
	if spec.Notification.Image != "" {
		v.Notification.Image = spec.Notification.Image
	}
	if spec.Notification.CPU != "" {
		v.Notification.Resources.Requests.Cpu = spec.Notification.CPU
		v.Notification.Resources.Limits.Cpu = spec.Notification.CPU
	}
	if spec.Notification.MEM != "" {
		v.Notification.Resources.Requests.Memory = spec.Notification.MEM
		v.Notification.Resources.Limits.Memory = spec.Notification.MEM
	}
	if spec.Notification.Disk != "" {
		v.Notification.VolumeSize = spec.Notification.Disk
	}
	return v
}

// getMetricsProcessorValues - get ONMS MetricsProcessor values from the crd
func getMetricsProcessorValues(spec v1alpha1.OpenNMSSpec, v values.OpenNMSValues) values.OpenNMSValues {
	if spec.MetricsProcessor.Image != "" {
		v.MetricsProcessor.Image = spec.MetricsProcessor.Image
	}
	if spec.MetricsProcessor.CPU != "" {
		v.MetricsProcessor.Resources.Requests.Cpu = spec.MetricsProcessor.CPU
		v.MetricsProcessor.Resources.Limits.Cpu = spec.MetricsProcessor.CPU
	}
	if spec.MetricsProcessor.MEM != "" {
		v.MetricsProcessor.Resources.Requests.Memory = spec.MetricsProcessor.MEM
		v.MetricsProcessor.Resources.Limits.Memory = spec.MetricsProcessor.MEM
	}
	if spec.MetricsProcessor.Disk != "" {
		v.MetricsProcessor.VolumeSize = spec.MetricsProcessor.Disk
	}
	return v
}

// getEventsValues - get ONMS Events values from the crd
func getEventsValues(spec v1alpha1.OpenNMSSpec, v values.OpenNMSValues) values.OpenNMSValues {
	if spec.Events.Image != "" {
		v.Events.Image = spec.Events.Image
	}
	if spec.Events.CPU != "" {
		v.Events.Resources.Requests.Cpu = spec.Events.CPU
		v.Events.Resources.Limits.Cpu = spec.Events.CPU
	}
	if spec.Events.MEM != "" {
		v.Events.Resources.Requests.Memory = spec.Events.MEM
		v.Events.Resources.Limits.Memory = spec.Events.MEM
	}
	if spec.Events.Disk != "" {
		v.Events.VolumeSize = spec.Events.Disk
	}
	return v
}

// getDatachoicesValues - get ONMS DataChoices values from the crd
func getDatachoicesValues(spec v1alpha1.OpenNMSSpec, v values.OpenNMSValues) values.OpenNMSValues {
	if spec.DataChoices.Image != "" {
		v.DataChoices.Image = spec.DataChoices.Image
	}
	if spec.DataChoices.CPU != "" {
		v.DataChoices.Resources.Requests.Cpu = spec.DataChoices.CPU
		v.DataChoices.Resources.Limits.Cpu = spec.DataChoices.CPU
	}
	if spec.DataChoices.MEM != "" {
		v.DataChoices.Resources.Requests.Memory = spec.DataChoices.MEM
		v.DataChoices.Resources.Limits.Memory = spec.DataChoices.MEM
	}
	if spec.DataChoices.Disk != "" {
		v.DataChoices.VolumeSize = spec.DataChoices.Disk
	}
	return v
}

// getKeycloakValues - get Keycloak values from the crd
func getKeycloakValues(spec v1alpha1.OpenNMSSpec, v values.KeycloakValues) values.KeycloakValues {
	if spec.Keycloak.Image != "" {
		v.Image = spec.Keycloak.Image
	}
	return v
}

// getGrafanaValues - get ONMS Grafana values from the crd
func getGrafanaValues(spec v1alpha1.OpenNMSSpec, v values.GrafanaValues) values.GrafanaValues {
	if spec.Grafana.Image != "" {
		v.Image = spec.Grafana.Image
	}
	return v
}

// getPostgresValues - get Postgres DB values from the CRD
func getPostgresValues(spec v1alpha1.OpenNMSSpec, v values.PostgresValues) values.PostgresValues {
	if spec.Postgres.Image != "" {
		v.Image = spec.Postgres.Image
	}
	if spec.Postgres.Disk != "" {
		v.VolumeSize = spec.Postgres.Disk
	}
	return v
}

func setPorts(spec v1alpha1.OpenNMSSpec, v values.IngressValues) values.IngressValues {
	if spec.HttpPort != 0 {
		v.HttpPort = spec.HttpPort
	}
	if spec.HttpsPort != 0 {
		v.HttpsPort = spec.HttpsPort
	}
	return v
}
