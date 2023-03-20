package values

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

import (
	"github.com/OpenNMS-Cloud/opennms-operator/config"
	"github.com/OpenNMS-Cloud/opennms-operator/internal/model/values"
	uberConfig "go.uber.org/config"
)

// GetDefaultValues - get the default Helm/Template values
func GetDefaultValues(operatorConfig config.OperatorConfig) (values.TemplateValues, error) {
	v, err := LoadValues(operatorConfig.DefaultOpenNMSValuesFile)
	v.Keycloak.DynamicRealmConfig = false
	v.OpenShift = operatorConfig.OpenshiftMode
	return values.TemplateValues{
		Values:  v,
		Release: values.HelmRelease{},
	}, err
}

// LoadValues - load Helm/Template values from the given files
func LoadValues(opennmsValues string) (values.Values, error) {
	yaml, err := uberConfig.NewYAML(
		uberConfig.Permissive(), // this allows for values from the yaml that aren't represented in the Values struct
		uberConfig.File(opennmsValues),
	)
	if err != nil {
		return values.Values{}, err
	}
	var defValues values.Values
	err = yaml.Get(uberConfig.Root).Populate(&defValues)
	if err != nil {
		return values.Values{}, err
	}
	return defValues, nil
}
