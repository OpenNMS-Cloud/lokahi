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

package template

import (
	"bytes"
	"github.com/Masterminds/sprig/v3"
	"github.com/OpenNMS/opennms-operator/internal/model/values"
	"text/template"
)

var templater *template.Template

func TemplateConfig(file string, values values.TemplateValues) (string, error) {
	if templater == nil {
		initTemplater()
	}
	tmpl, err := templater.Parse(file)
	if err != nil {
		return "", err
	}
	buffer := new(bytes.Buffer)
	err = tmpl.Execute(buffer, values)
	if err != nil {
		return "", err
	}
	return buffer.String(), nil
}

func initTemplater() {
	templater = template.New("operator-templater")
	funcMap := sprig.TxtFuncMap()
	funcMap["lookup"] = emptyDict
	templater.Funcs(funcMap)
}

func emptyDict(_ ...string) map[string]interface{} {
	return map[string]interface{}{}
}
