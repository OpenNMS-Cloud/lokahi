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
	"github.com/OpenNMS/opennms-operator/internal/model/values"
	"github.com/OpenNMS/opennms-operator/internal/util/yaml"
	corev1 "k8s.io/api/core/v1"
	rbacv1 "k8s.io/api/rbac/v1"
	"sigs.k8s.io/controller-runtime/pkg/client"
)

type BaseHandler struct {
	ServiceHandlerObject
}

func (h *BaseHandler) UpdateConfig(values values.TemplateValues) {
	var namespace corev1.Namespace
	var certSecret corev1.Secret
	var endpointRole rbacv1.Role

	yaml.LoadYaml(filepath("_namespace.yaml"), values, &namespace)
	yaml.LoadYaml(filepath("cert/cert-secret.yaml"), values, &certSecret)
	yaml.LoadYaml(filepath("endpoints-role.yaml"), values, &endpointRole)

	h.Config = []client.Object{
		&namespace,
		&certSecret,
		&endpointRole,
	}
}
