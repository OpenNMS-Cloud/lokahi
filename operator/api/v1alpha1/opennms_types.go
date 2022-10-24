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

package v1alpha1

import (
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

// +kubebuilder:object:generate=true

// OpenNMSSpec defines the desired state of OpenNMS
type OpenNMSSpec struct {
	// Domain name used in ingress rule
	Host string `json:"host,omitempty"`

	//HTTP port number the instance will be exposed on
	HttpPort int `json:"httpPort"`

	//HTTPS port number the instance will be exposed on
	HttpsPort int `json:"httpsPort"`

	// K8s namespace to use
	Namespace string `json:"namespace"`

	// Deploy an instance in a nonoperative testing mode
	TestDeploy bool `json:"testDeploy,omitempty"` //TODO I don't think this is needed anymore

	// Only deploy the instance, do not run recurring updates on it
	DeployOnly bool `json:"deployOnly,omitempty"`

	// Set the default credentials for the instance
	Credentials Credentials `json:"credentials,omitempty"`

	// Defines service values for Core service
	Core BaseServiceResources `json:"core,omitempty"`

	// Defines service values for API service
	API BaseServiceResources `json:"api,omitempty"`

	// Defines service values for UI service
	UI BaseServiceResources `json:"ui,omitempty"`

	// Defines service values for the Minion
	Minion BaseServiceResources `json:"minion,omitempty"`

	// Defines service values for the Minion Gateway
	MinionGateway BaseServiceResources `json:"minionGateway,omitempty"`

	// Defines service values for Inventory service
	Inventory BaseServiceResources `json:"inventory,omitempty"`

	// Defines service values for Notification service
	Notification BaseServiceResources `json:"notification,omitempty"`

	// Defines service values for MetricsProcessor service
	MetricsProcessor BaseServiceResources `json:"metricsProcessor,omitempty"`

	// Defines service values for Postgres
	Postgres BaseServiceResources `json:"postgres,omitempty"`

	// Defines service values for Postgres
	Keycloak BaseServiceResources `json:"keycloak,omitempty"`

	// Defines service values for Grafana
	Grafana BaseServiceResources `json:"grafana,omitempty"`

	// Defines the logic of ONMS image update
	ImageUpdateConfig ImageUpdateConfig `json:"imageUpdate,omitempty"`
}

// Timeseries - defines the timeseries DB backend to use
type Timeseries struct {
	Mode   string `json:"mode,omitempty"`
	Host   string `json:"host,omitempty"`
	Port   string `json:"port,omitempty"`
	ApiKey string `json:"apiKey,omitempty"`
}

// BaseServiceResources - defines basic resource needs of a service
type BaseServiceResources struct {
	// Image tag version of OpenNMS.
	Image string `json:"image,omitempty"`
	MEM   string `json:"mem,omitempty"`
	Disk  string `json:"disk,omitempty"`
	CPU   string `json:"cpu,omitempty"`
}

type Credentials struct {
	AdminPassword string `json:"adminPassword"`
	UserPassword  string `json:"userPassword"`
}

// +kubebuilder:object:generate=true

// OpenNMSStatus - defines the observed state of OpenNMS
type OpenNMSStatus struct {
	Image     ImageStatus     `json:"image,omitempty"`
	Readiness ReadinessStatus `json:"readiness,omitempty"`
	Nodes     []string        `json:"nodes,omitempty"`
}

// ImageUpdateConfig - defines current status of used image for OpenNMS container
type ImageUpdateConfig struct {
	// can have values of now/none
	Update string `json:"update,omitempty"`
}

// +kubebuilder:object:generate=true

// ImageStatus - defines current status of used image for OpenNMS container
type ImageStatus struct {
	// true if latest image used, false otherwise
	IsLatest bool `json:"isLatest"`
	// timestamp of a last image check in DockerHub
	CheckedAt string `json:"checkedAt,omitempty"`
	// list of services that have updates available
	ServicesToUpdate string `json:"servicesToUpdate,omitempty"`
}

// +kubebuilder:object:generate=true

// ReadinessStatus - the ready status of the ONMS instance
type ReadinessStatus struct {
	// if the ONMS instance is ready
	Ready bool `json:"ready,omitempty"`
	// reason an ONMS instance isn't ready
	Reason string `json:"reason,omitempty"`
	// the time the `ready` flag was last updated
	Timestamp string `json:"timestamp,omitempty"`
	// list of readinesses of the constituent services
	Services []ServiceStatus `json:"services,omitempty"`
}

type ServiceStatus struct {
	// if the service is ready
	Ready bool `json:"ready"`
	// reason a service isn't ready
	Reason string `json:"reason"`
	// the time the `ready` flag was last updated
	Timestamp string `json:"timestamp,omitempty"`
}

// +kubebuilder:object:root=true
// +kubebuilder:subresource:status

// OpenNMS - is the Schema for the opennms API
type OpenNMS struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`

	Spec   OpenNMSSpec   `json:"spec,omitempty"`
	Status OpenNMSStatus `json:"status,omitempty"`
}

// +kubebuilder:object:root=true

// OpenNMSList - contains a list of OpenNMS
type OpenNMSList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`
	Items           []OpenNMS `json:"items"`
}

func init() {
	SchemeBuilder.Register(&OpenNMS{}, &OpenNMSList{})
}
