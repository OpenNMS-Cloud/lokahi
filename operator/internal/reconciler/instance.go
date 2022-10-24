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

package reconciler

import (
	"context"
	"github.com/OpenNMS/opennms-operator/api/v1alpha1"
	"github.com/OpenNMS/opennms-operator/internal/handlers"
	"github.com/OpenNMS/opennms-operator/internal/model/values"
	"reflect"
	"sigs.k8s.io/controller-runtime/pkg/client"
)

// Instance - represents an ONMS instance as it is known to the operator
type Instance struct {
	Name     string
	CRD      v1alpha1.OpenNMS
	Values   values.TemplateValues
	Deployed bool
	Handlers []handlers.ServiceHandler
	Client   client.Client
}

func (i *Instance) Init(ctx context.Context, k8sClient client.Client, defaultValues values.TemplateValues, handlers []handlers.ServiceHandler, crd v1alpha1.OpenNMS) {
	i.Name = crd.Name
	i.CRD = crd
	i.Client = k8sClient
	i.Values = defaultValues
	i.Handlers = handlers
	i.Deployed = false
	i.SetValues(ctx)
	i.updateHandlers()
}

func (i *Instance) Update(ctx context.Context, crd v1alpha1.OpenNMS) {
	if reflect.DeepEqual(i.CRD.Spec, crd.Spec) { //no changes, no work needed
		return
	}
	i.CRD = crd
	i.SetValues(ctx)
	i.updateHandlers()
}

func (i *Instance) updateHandlers() {
	for _, handler := range i.Handlers {
		handler.UpdateConfig(i.Values)
	}
}
