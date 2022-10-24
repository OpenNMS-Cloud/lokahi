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

package yaml

import (
	"github.com/OpenNMS/opennms-operator/internal/model/values"
	"github.com/OpenNMS/opennms-operator/internal/util/template"
	"k8s.io/apimachinery/pkg/util/yaml"
	"log"
	"os"
	"strings"
)

// LoadYaml - loads a yaml from the given filename and templates the given values into it
func LoadYaml(filename string, values values.TemplateValues, decodeInto interface{}) {
	cache := Cache()
	file, found := cache.Get(filename)
	if !found {
		loadedFile, err := loadFromFile(filename)
		if err != nil {
			log.Fatalf("%s: failed to load config: %v", filename, err)
		}
		cache.Set(filename, loadedFile)
		file = loadedFile
	}
	templatedConfig, err := template.TemplateConfig(file, values)
	if err != nil {
		log.Fatalf("%s: failed to template config: %v", filename, err)
	}
	err = decodeIntoObject(templatedConfig, decodeInto)
	if err != nil {
		log.Fatalf("%s: failed to unmarshal config: %v", filename, err)
	}
}

// decodeIntoObject - decode a given yaml string into a give interface
func decodeIntoObject(yamlStr string, decodeInto interface{}) error {
	reader := strings.NewReader(yamlStr)
	return yaml.NewYAMLOrJSONDecoder(reader, 4096).Decode(decodeInto)
}

// loadFromFile - loads a given filename
func loadFromFile(filename string) (string, error) {
	file, err := os.ReadFile(filename)
	if err != nil {
		return "", err
	}
	return string(file), nil
}
