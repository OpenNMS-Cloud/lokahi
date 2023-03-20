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

package values

type TemplateValues struct {
	Release HelmRelease
	Values  Values
}

// Values - Helm values for a complete OpenNMS Horizon Stream instance
type Values struct {
	Host             string                 `yaml:"Host"`
	Port             int                    `yaml:"Port"`
	Protocol         string                 `yaml:"Protocol"`
	OpenShift        bool                   `yaml:"OpenShift"`
	Cortex           CortexValues           `yaml:"Cortex"`
	OpenNMS          OpenNMSValues          `yaml:"OpenNMS"`
	TLS              TLSValues              `yaml:"TLS"`
	Postgres         PostgresValues         `yaml:"Postgres"`
	Grafana          GrafanaValues          `yaml:"Grafana"`
	Ingress          IngressValues          `yaml:"Ingress"`
	Keycloak         KeycloakValues         `yaml:"Keycloak"`
	Kafka            KafkaValues            `yaml:"Kafka"`
	MailServer       MailServerValues       `yaml:"MailServer"`
	Prometheus       PrometheusValues       `yaml:"Prometheus`
	NodeRestrictions NodeRestrictionsValues `yaml:"NodeRestrictions"`
	Operator         OperatorValues         `yaml:"Operator"`
	CustomErrors     CustomErrorsValues     `yaml:"CustomErrors"`
}

// HelmRelease - Special Helm values
type HelmRelease struct {
	Namespace string `yaml:"Namespace"`
}
