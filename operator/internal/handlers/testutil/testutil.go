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

package testutil

import (
	"github.com/OpenNMS-Cloud/opennms-operator/config"
	"github.com/OpenNMS-Cloud/opennms-operator/internal/model/values"
	values2 "github.com/OpenNMS-Cloud/opennms-operator/internal/util/values"
)

var valuesSet bool

var tvals values.TemplateValues

func DefaultTestValues() values.TemplateValues {
	if !valuesSet {
		tvals, _ = values2.GetDefaultValues(config.OperatorConfig{
			DefaultOpenNMSValuesFile: "./../../../charts/lokahi/values.yaml",
		})
		valuesSet = true
	}
	return tvals
}
