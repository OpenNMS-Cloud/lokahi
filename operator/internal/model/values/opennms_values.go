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

type OpenNMSValues struct {
	API                    APIValues              `yaml:"API"`
	UI                     UIValues               `yaml:"UI"`
	Minion                 MinionValues           `yaml:"Minion"`
	MinionGateway          MinionGatewayValues    `yaml:"MinionGateway"`
	MinionSslGateway       MinionSslGatewayValues `yaml:"MinionSslGateway"`
	MinionGatewayGrpcProxy MGGPValues             `yaml:"MinionGatewayGrpcProxy"`
	Inventory              InventoryValues        `yaml:"Inventory"`
	Alert                  AlertValues            `yaml:"Alert"`
	Notification           NotificationValues     `yaml:"Notification"`
	MetricsProcessor       MetricsProcessorValues `yaml:"MetricsProcessor"`
	Events                 EventsValues           `yaml:"Events"`
	DataChoices            DataChoicesValues      `yaml:"DataChoices"`
}

type MinionValues struct {
	ServiceValues        `yaml:",inline"`
	Netflow5ListenerPort int `yaml:"Netflow5ListenerPort"`
	Netflow9ListenerPort int `yaml:"Netflow9ListenerPort"`
	SshPort              int `yaml:"SshPort"`
	TrapsListenerPort    int `yaml:"TrapsListenerPort"`
}

type MinionGatewayValues struct {
	ServiceValues      `yaml:",inline"`
	InternalGrpcPort   int         `yaml:"InternalGrpcPort"`
	IgniteClientPort   int         `yaml:"IgniteClientPort"`
	IngressAnnotations interface{} `yaml:"IngressAnnotations"`
}

type MinionSslGatewayValues struct {
	ServiceValues `yaml:",inline"`
}

type APIValues struct {
	ServiceValues `yaml:",inline"`
}

type UIValues struct {
	ServiceValues `yaml:",inline"`
}

type MGGPValues struct {
	ServiceValues `yaml:",inline"`
}
type MetricsProcessorValues struct {
	ServiceValues `yaml:",inline"`
}

type NotificationValues struct {
	ServiceValues `yaml:",inline"`
}

type InventoryValues struct {
	ServiceValues `yaml:",inline"`
	EncryptionKey string `yaml:"EncryptionKey"`
}

type AlertValues struct {
	ServiceValues `yaml:",inline"`
}

type EventsValues struct {
	ServiceValues `yaml:",inline"`
}

type DataChoicesValues struct {
	ServiceValues `yaml:",inline"`
}

type TimeseriesValues struct {
	Mode   string `yaml:"Mode"`
	Host   string `yaml:"Host"`
	Port   string `yaml:"Port"`
	ApiKey string `yaml:"ApiKey"`
}
