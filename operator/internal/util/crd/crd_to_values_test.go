//go:build unit

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
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestConvertCRDToValues(t *testing.T) {
	crd := v1alpha1.OpenNMS{
		Spec: v1alpha1.OpenNMSSpec{
			Namespace: "testns",
			Host:      "testhost",
		},
	}

	res := ConvertCRDToValues(crd, values.TemplateValues{})

	assert.Equal(t, crd.Spec.Namespace, res.Release.Namespace, "should pull the correct value")
	assert.Equal(t, crd.Spec.Host, res.Values.Host, "should pull the correct value")
}

func TestGetAPIValues(t *testing.T) {
	spec := v1alpha1.OpenNMSSpec{
		API: v1alpha1.BaseServiceResources{
			Image: "image",
			CPU:   "testcpu",
			MEM:   "testmem",
			Disk:  "testdisk",
		},
	}

	v := values.OpenNMSValues{}

	v = getAPIValues(spec, v)

	assert.Equal(t, spec.API.Image, v.API.Image, "should set the image correctly")
	assert.Equal(t, spec.API.CPU, v.API.Resources.Requests.Cpu, "should pull the correct value")
	assert.Equal(t, spec.API.MEM, v.API.Resources.Requests.Memory, "should pull the correct value")
	assert.Equal(t, spec.API.Disk, v.API.VolumeSize, "should pull the correct value")

	v = getAPIValues(v1alpha1.OpenNMSSpec{}, v)

	assert.Equal(t, "image", v.API.Image, "value should remain unchanged when spec is unset")
	assert.Equal(t, "testcpu", v.API.Resources.Requests.Cpu, "value should remain unchanged when spec is unset")
	assert.Equal(t, "testmem", v.API.Resources.Requests.Memory, "value should remain unchanged when spec is unset")
	assert.Equal(t, "testdisk", v.API.VolumeSize, "value should remain unchanged when spec is unset")
}

func TestGetUIValues(t *testing.T) {
	spec := v1alpha1.OpenNMSSpec{
		UI: v1alpha1.BaseServiceResources{
			Image: "image",
			CPU:   "testcpu",
			MEM:   "testmem",
			Disk:  "testdisk",
		},
	}

	v := values.OpenNMSValues{}

	v = getUIValues(spec, v)

	assert.Equal(t, spec.UI.Image, v.UI.Image, "should set the image correctly")
	assert.Equal(t, spec.UI.CPU, v.UI.Resources.Requests.Cpu, "should pull the correct value")
	assert.Equal(t, spec.UI.MEM, v.UI.Resources.Requests.Memory, "should pull the correct value")
	assert.Equal(t, spec.UI.Disk, v.UI.VolumeSize, "should pull the correct value")

	v = getUIValues(v1alpha1.OpenNMSSpec{}, v)

	assert.Equal(t, "image", v.UI.Image, "value should remain unchanged when spec is unset")
	assert.Equal(t, "testcpu", v.UI.Resources.Requests.Cpu, "value should remain unchanged when spec is unset")
	assert.Equal(t, "testmem", v.UI.Resources.Requests.Memory, "value should remain unchanged when spec is unset")
	assert.Equal(t, "testdisk", v.UI.VolumeSize, "value should remain unchanged when spec is unset")
}

func TestGetMinionValues(t *testing.T) {
	spec := v1alpha1.OpenNMSSpec{
		Minion: v1alpha1.BaseServiceResources{
			Image: "image",
			CPU:   "testcpu",
			MEM:   "testmem",
			Disk:  "testdisk",
		},
		MinionGateway: v1alpha1.BaseServiceResources{
			Image: "image",
			CPU:   "testcpu",
			MEM:   "testmem",
			Disk:  "testdisk",
		},
	}

	v := values.OpenNMSValues{}

	v = getMinionValues(spec, v)

	assert.Equal(t, spec.Minion.Image, v.Minion.Image, "should set the image correctly")
	assert.Equal(t, spec.Minion.CPU, v.Minion.Resources.Requests.Cpu, "should pull the correct value")
	assert.Equal(t, spec.Minion.MEM, v.Minion.Resources.Requests.Memory, "should pull the correct value")
	assert.Equal(t, spec.Minion.Disk, v.Minion.VolumeSize, "should pull the correct value")

	assert.Equal(t, spec.MinionGateway.Image, v.MinionGateway.Image, "should set the image correctly")
	assert.Equal(t, spec.MinionGateway.CPU, v.MinionGateway.Resources.Requests.Cpu, "should pull the correct value")
	assert.Equal(t, spec.MinionGateway.MEM, v.MinionGateway.Resources.Requests.Memory, "should pull the correct value")
	assert.Equal(t, spec.MinionGateway.Disk, v.MinionGateway.VolumeSize, "should pull the correct value")

	v = getMinionValues(v1alpha1.OpenNMSSpec{}, v)

	assert.Equal(t, "image", v.Minion.Image, "value should remain unchanged when spec is unset")
	assert.Equal(t, "testcpu", v.Minion.Resources.Requests.Cpu, "value should remain unchanged when spec is unset")
	assert.Equal(t, "testmem", v.Minion.Resources.Requests.Memory, "value should remain unchanged when spec is unset")
	assert.Equal(t, "testdisk", v.Minion.VolumeSize, "value should remain unchanged when spec is unset")

	assert.Equal(t, "image", v.MinionGateway.Image, "value should remain unchanged when spec is unset")
	assert.Equal(t, "testcpu", v.MinionGateway.Resources.Requests.Cpu, "value should remain unchanged when spec is unset")
	assert.Equal(t, "testmem", v.MinionGateway.Resources.Requests.Memory, "value should remain unchanged when spec is unset")
	assert.Equal(t, "testdisk", v.MinionGateway.VolumeSize, "value should remain unchanged when spec is unset")
}

func TestGetInventoryValues(t *testing.T) {
	spec := v1alpha1.OpenNMSSpec{
		Inventory: v1alpha1.BaseServiceResources{
			Image: "image",
			CPU:   "testcpu",
			MEM:   "testmem",
			Disk:  "testdisk",
		},
	}

	v := values.OpenNMSValues{}

	v = getInventoryValues(spec, v)

	assert.Equal(t, spec.Inventory.Image, v.Inventory.Image, "should set the image correctly")
	assert.Equal(t, spec.Inventory.CPU, v.Inventory.Resources.Requests.Cpu, "should pull the correct value")
	assert.Equal(t, spec.Inventory.MEM, v.Inventory.Resources.Requests.Memory, "should pull the correct value")
	assert.Equal(t, spec.Inventory.Disk, v.Inventory.VolumeSize, "should pull the correct value")

	v = getInventoryValues(v1alpha1.OpenNMSSpec{}, v)

	assert.Equal(t, "image", v.Inventory.Image, "value should remain unchanged when spec is unset")
	assert.Equal(t, "testcpu", v.Inventory.Resources.Requests.Cpu, "value should remain unchanged when spec is unset")
	assert.Equal(t, "testmem", v.Inventory.Resources.Requests.Memory, "value should remain unchanged when spec is unset")
	assert.Equal(t, "testdisk", v.Inventory.VolumeSize, "value should remain unchanged when spec is unset")
}

func TestGetNotificationsValues(t *testing.T) {
	spec := v1alpha1.OpenNMSSpec{
		Notification: v1alpha1.BaseServiceResources{
			Image: "image",
			CPU:   "testcpu",
			MEM:   "testmem",
			Disk:  "testdisk",
		},
	}

	v := values.OpenNMSValues{}

	v = getNotificationValues(spec, v)

	assert.Equal(t, spec.Notification.Image, v.Notification.Image, "should set the image correctly")
	assert.Equal(t, spec.Notification.CPU, v.Notification.Resources.Requests.Cpu, "should pull the correct value")
	assert.Equal(t, spec.Notification.MEM, v.Notification.Resources.Requests.Memory, "should pull the correct value")
	assert.Equal(t, spec.Notification.Disk, v.Notification.VolumeSize, "should pull the correct value")

	v = getNotificationValues(v1alpha1.OpenNMSSpec{}, v)

	assert.Equal(t, "image", v.Notification.Image, "value should remain unchanged when spec is unset")
	assert.Equal(t, "testcpu", v.Notification.Resources.Requests.Cpu, "value should remain unchanged when spec is unset")
	assert.Equal(t, "testmem", v.Notification.Resources.Requests.Memory, "value should remain unchanged when spec is unset")
	assert.Equal(t, "testdisk", v.Notification.VolumeSize, "value should remain unchanged when spec is unset")
}

func TestGetMetricsProcessorValues(t *testing.T) {
	spec := v1alpha1.OpenNMSSpec{
		MetricsProcessor: v1alpha1.BaseServiceResources{
			Image: "image",
			CPU:   "testcpu",
			MEM:   "testmem",
			Disk:  "testdisk",
		},
	}

	v := values.OpenNMSValues{}

	v = getMetricsProcessorValues(spec, v)

	assert.Equal(t, spec.MetricsProcessor.Image, v.MetricsProcessor.Image, "should set the image correctly")
	assert.Equal(t, spec.MetricsProcessor.CPU, v.MetricsProcessor.Resources.Requests.Cpu, "should pull the correct value")
	assert.Equal(t, spec.MetricsProcessor.MEM, v.MetricsProcessor.Resources.Requests.Memory, "should pull the correct value")
	assert.Equal(t, spec.MetricsProcessor.Disk, v.MetricsProcessor.VolumeSize, "should pull the correct value")

	v = getMetricsProcessorValues(v1alpha1.OpenNMSSpec{}, v)

	assert.Equal(t, "image", v.MetricsProcessor.Image, "value should remain unchanged when spec is unset")
	assert.Equal(t, "testcpu", v.MetricsProcessor.Resources.Requests.Cpu, "value should remain unchanged when spec is unset")
	assert.Equal(t, "testmem", v.MetricsProcessor.Resources.Requests.Memory, "value should remain unchanged when spec is unset")
	assert.Equal(t, "testdisk", v.MetricsProcessor.VolumeSize, "value should remain unchanged when spec is unset")
}

func TestGetEventsValues(t *testing.T) {
	spec := v1alpha1.OpenNMSSpec{
		Events: v1alpha1.BaseServiceResources{
			Image: "image",
			CPU:   "testcpu",
			MEM:   "testmem",
			Disk:  "testdisk",
		},
	}

	v := values.OpenNMSValues{}

	v = getEventsValues(spec, v)

	assert.Equal(t, spec.Events.Image, v.Events.Image, "should set the image correctly")
	assert.Equal(t, spec.Events.CPU, v.Events.Resources.Requests.Cpu, "should pull the correct value")
	assert.Equal(t, spec.Events.MEM, v.Events.Resources.Requests.Memory, "should pull the correct value")
	assert.Equal(t, spec.Events.Disk, v.Events.VolumeSize, "should pull the correct value")

	v = getEventsValues(v1alpha1.OpenNMSSpec{}, v)

	assert.Equal(t, "image", v.Events.Image, "value should remain unchanged when spec is unset")
	assert.Equal(t, "testcpu", v.Events.Resources.Requests.Cpu, "value should remain unchanged when spec is unset")
	assert.Equal(t, "testmem", v.Events.Resources.Requests.Memory, "value should remain unchanged when spec is unset")
	assert.Equal(t, "testdisk", v.Events.VolumeSize, "value should remain unchanged when spec is unset")
}

func TestGetDataChoicesValues(t *testing.T) {
	spec := v1alpha1.OpenNMSSpec{
		DataChoices: v1alpha1.BaseServiceResources{
			Image: "image",
			CPU:   "testcpu",
			MEM:   "testmem",
			Disk:  "testdisk",
		},
	}

	v := values.OpenNMSValues{}

	v = getDatachoicesValues(spec, v)

	assert.Equal(t, spec.DataChoices.Image, v.DataChoices.Image, "should set the image correctly")
	assert.Equal(t, spec.DataChoices.CPU, v.DataChoices.Resources.Requests.Cpu, "should pull the correct value")
	assert.Equal(t, spec.DataChoices.MEM, v.DataChoices.Resources.Requests.Memory, "should pull the correct value")
	assert.Equal(t, spec.DataChoices.Disk, v.DataChoices.VolumeSize, "should pull the correct value")

	v = getDatachoicesValues(v1alpha1.OpenNMSSpec{}, v)

	assert.Equal(t, "image", v.DataChoices.Image, "value should remain unchanged when spec is unset")
	assert.Equal(t, "testcpu", v.DataChoices.Resources.Requests.Cpu, "value should remain unchanged when spec is unset")
	assert.Equal(t, "testmem", v.DataChoices.Resources.Requests.Memory, "value should remain unchanged when spec is unset")
	assert.Equal(t, "testdisk", v.DataChoices.VolumeSize, "value should remain unchanged when spec is unset")
}

func TestGetPostgresValues(t *testing.T) {
	spec := v1alpha1.OpenNMSSpec{
		Postgres: v1alpha1.BaseServiceResources{
			Image: "image",
			Disk:  "testing",
		},
	}

	v := values.PostgresValues{}

	v = getPostgresValues(spec, v)

	assert.Equal(t, spec.Postgres.Image, v.Image, "should set the image correctly")
	assert.Equal(t, spec.Postgres.Disk, v.VolumeSize, "should pull the correct value")

	v = getPostgresValues(v1alpha1.OpenNMSSpec{}, v)

	assert.Equal(t, "image", v.Image, "value should remain unchanged when spec is unset")
	assert.Equal(t, "testing", v.VolumeSize, "value should remain unchanged when spec is unset")
}

func TestGetKeycloakValues(t *testing.T) {
	spec := v1alpha1.OpenNMSSpec{
		Keycloak: v1alpha1.BaseServiceResources{
			Image: "image",
		},
	}

	v := values.KeycloakValues{}

	v = getKeycloakValues(spec, v)

	assert.Equal(t, spec.Keycloak.Image, v.Image, "should set the image correctly")

	v = getKeycloakValues(v1alpha1.OpenNMSSpec{}, v)

	assert.Equal(t, "image", v.Image, "value should remain unchanged when spec is unset")
}

func TestGetGrafanaValues(t *testing.T) {
	spec := v1alpha1.OpenNMSSpec{
		Grafana: v1alpha1.BaseServiceResources{
			Image: "image",
		},
	}

	v := values.GrafanaValues{}

	v = getGrafanaValues(spec, v)

	assert.Equal(t, spec.Grafana.Image, v.Image, "should set the image correctly")

	v = getGrafanaValues(v1alpha1.OpenNMSSpec{}, v)

	assert.Equal(t, "image", v.Image, "value should remain unchanged when spec is unset")
}

func TestSetPorts(t *testing.T) {
	spec := v1alpha1.OpenNMSSpec{
		HttpsPort: 123,
		HttpPort:  456,
	}
	iV := values.IngressValues{}
	iV = setPorts(spec, iV)
	assert.Equal(t, 123, iV.HttpsPort, "should set https port")
	assert.Equal(t, 456, iV.HttpPort, "should set http port")
}
