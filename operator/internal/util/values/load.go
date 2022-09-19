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
	"github.com/OpenNMS/opennms-operator/config"
	"github.com/OpenNMS/opennms-operator/internal/model/values"
	uberConfig "go.uber.org/config"
	"log"
)

// GetDefaultValues - get the default Helm/Template values
func GetDefaultValues(operatorConfig config.OperatorConfig) values.TemplateValues {
	v := LoadValues(operatorConfig.DefaultOpenNMSValuesFile, operatorConfig.DefaultOperatorValuesFile)
	return values.TemplateValues{
		Values: v,
	}
}

// LoadValues - load Helm/Template values from the given files
func LoadValues(opennmsValues string, operatorValues string) values.Values {
	combinedYaml, err := uberConfig.NewYAML(
		// TODO remove after cleaning up yamls
		uberConfig.Permissive(), // this allows for values from the yaml that aren't represented in the Values struct
		uberConfig.File(opennmsValues),
		uberConfig.File(operatorValues),
	)
	if err != nil {
		log.Fatalf("error reading yaml from files: %v", err)
	}
	var defValues values.Values
	err = combinedYaml.Get(uberConfig.Root).Populate(&defValues)
	if err != nil {
		log.Fatalf("error unmarshalling default values from file: %v", err)
	}
	return defValues
}
