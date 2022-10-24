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
    Core             CoreValues             `yaml:"Core"`
    API              ServiceValues          `yaml:"API"`
    UI               ServiceValues          `yaml:"UI"`
    Minion           MinionValues           `yaml:"Minion"`
    MinionGateway    MinionGatewayValues    `yaml:"MinionGateway"`
    Inventory        ServiceValues          `yaml:"Inventory"`
    Notification     ServiceValues          `yaml:"Notification"`
    MetricsProcessor MetricsProcessorValues `yaml:"MetricsProcessor"`
}

type CoreValues struct {
    ServiceValues       `yaml:",inline"`
    HttpPort            int `yaml:"HttpPort"`
    SshPort             int `yaml:"SshPort"`
    GrpcPort            int `yaml:"GrpcPort"`
    IgniteClusterPort   int `yaml:"IgniteClusterPort"`
    IgniteDiscoveryPort int `yaml:"IgniteDiscoveryPort"`
}

type MinionValues struct {
    ServiceValues `yaml:",inline"`
    SshPort       int `yaml:"SshPort"`
}

type MinionGatewayValues struct {
    ServiceValues    `yaml:",inline"`
    GrpcPort         int `yaml:"GrpcPort"`
    IgniteClientPort int `yaml:"IgniteClientPort"`
}

type MetricsProcessorValues struct {
    ServiceValues `yaml:",inline"`
}

type TimeseriesValues struct {
    Mode   string `yaml:"Mode"`
    Host   string `yaml:"Host"`
    Port   string `yaml:"Port"`
    ApiKey string `yaml:"ApiKey"`
}
