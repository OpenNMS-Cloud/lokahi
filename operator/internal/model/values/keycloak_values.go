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

import "github.com/google/uuid"

type KeycloakValues struct {
	ServiceValues `yaml:",inline"`
	HttpsPort     string   `yaml:"HttpsPort"`
	AdminUsername string   `yaml:"AdminUsername"`
	AdminPassword string   `yaml:"AdminPassword"`
	UserUsername  string   `yaml:"UserUsername"`
	UserPassword  string   `yaml:"UserPassword"`
	RealmName     string   `yaml:"RealmName"`
	UUID          UUIDList `yaml:"UUID"`
}

type UUIDList struct {
	RealmId     uuid.UUID `yaml:"RealmId"`
	ClientId    uuid.UUID `yaml:"ClientId"`
	BaseUserId  uuid.UUID `yaml:"BaseUserId"`
	AdminUserId uuid.UUID `yaml:"AdminUserId"`
}
