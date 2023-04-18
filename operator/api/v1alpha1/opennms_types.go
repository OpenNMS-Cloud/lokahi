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

	// Only deploy the instance, do not run recurring updates on it
	DeployOnly bool `json:"deployOnly,omitempty"`

	// Whether TLS is enabled for this instance
	TLSEnabled bool `json:"tlsEnabled,omitempty"`

	// Set the default credentials for the instance
	Credentials Credentials `json:"credentials,omitempty"`

	// Defines service values for API service
	API BaseServiceResources `json:"api,omitempty"`

	// Defines service values for UI service
	UI BaseServiceResources `json:"ui,omitempty"`

	// Defines service values for the Minion
	Minion BaseServiceResources `json:"minion,omitempty"`

	// Defines service values for the Minion Gateway
	MinionGateway BaseServiceResources `json:"minionGateway,omitempty"`

	// Defines service values for the Minion SSL Gateway
	MinionSSLGateway BaseServiceResources `json:"minionSslGateway,omitempty"`

	// Defines service values for Inventory service
	Inventory BaseServiceResources `json:"inventory,omitempty"`

	// Defines service values for Alert service
	Alert BaseServiceResources `json:"alert,omitempty"`

	// Defines service values for Notification service
	Notification BaseServiceResources `json:"notification,omitempty"`

	// Defines service values for MetricsProcessor service
	MetricsProcessor BaseServiceResources `json:"metricsProcessor,omitempty"`

	// Defines service values for Postgres
	Postgres BaseServiceResources `json:"postgres,omitempty"`

	// Defines service values for Events service
	Events BaseServiceResources `json:"events,omitempty"`

	// Defines service values for Keycloak
	Keycloak BaseServiceResources `json:"keycloak,omitempty"`

	// Defines service values for DataChoices service
	DataChoices BaseServiceResources `json:"dataChoices,omitempty"`

	// Defines service values for Grafana
	Grafana BaseServiceResources `json:"grafana,omitempty"`

	// Defines the config for ONMS updates
	UpdateConfig UpdateConfig `json:"updateConfig,omitempty"`
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
	Update    UpdateStatus    `json:"update,omitempty"`
	Readiness ReadinessStatus `json:"readiness,omitempty"`
	Nodes     []string        `json:"nodes,omitempty"`
}

// UpdateConfig - Defines the config for ONMS updates
type UpdateConfig struct {
	// update mode, either `automatic` updates or `manual`
	Mode string `json:"mode" default:"manual"`
	// toggle to force update, can be now/none
	Update string `json:"update" default:"none"`
}

// +kubebuilder:object:generate=true

// UpdateStatus - defines current status of available updates to ONMS
type UpdateStatus struct {
	// true if there's an update available for ONMS
	UpdateAvailable bool `json:"updateAvailable"`
	// timestamp of the last update check
	CheckedAt string `json:"checkedAt,omitempty"`
	// message describing the update status
	Message string `json:"message,omitempty"`
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
