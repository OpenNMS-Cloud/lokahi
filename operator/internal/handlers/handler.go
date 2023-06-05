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

package handlers

import (
	"github.com/OpenNMS-Cloud/opennms-operator/internal/model/values"
	"github.com/OpenNMS-Cloud/opennms-operator/internal/util/yaml"
	"sigs.k8s.io/controller-runtime/pkg/client"
)

var ConfigFilePath = "./charts/lokahi/templates/"

type ServiceHandler interface {
	//GetConfig - get the config
	GetConfig() []client.Object

	//UpdateConfig - update k8s config for the service handler
	UpdateConfig(values values.TemplateValues) error

	//GetDeployed - get the handler's deployed status
	GetDeployed() bool

	//SetDeployed - set the handler's deployed status
	SetDeployed(tf bool)
}
type ServiceHandlerObject struct {
	Templates []yaml.LoadTemplate
	Config    []client.Object
	Deployed  bool
}

func (o *ServiceHandlerObject) GetConfig() []client.Object {
	return o.Config
}

func (o *ServiceHandlerObject) GetDeployed() bool {
	return o.Deployed
}

func (o *ServiceHandlerObject) SetDeployed(tf bool) {
	o.Deployed = tf
}

// AddToTemplates - add a template to be loaded from YAML
func (o *ServiceHandlerObject) AddToTemplates(filename string, values values.TemplateValues, decodeInto interface{}) {
	tmpl := yaml.LoadTemplate{
		Filename:   filename,
		Values:     values,
		DecodeInto: decodeInto,
	}
	o.Templates = append(o.Templates, tmpl)
}

// GetTemplates - get the list of added templates
func (o *ServiceHandlerObject) GetTemplates() []yaml.LoadTemplate {
	return o.Templates
}

// LoadTemplates - load the list of added templates from YAML and template the TemplateValues into them, add to the config list
func (o *ServiceHandlerObject) LoadTemplates() error {
	for _, template := range o.GetTemplates() {
		err := yaml.LoadYaml(template)
		if err != nil {
			return err
		}
		o.Config = append(o.Config, template.DecodeInto.(client.Object))
	}
	return nil
}

func Filepath(filename string) string {
	return ConfigFilePath + filename
}
