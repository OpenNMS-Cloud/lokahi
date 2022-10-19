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
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	"sigs.k8s.io/controller-runtime/pkg/client"
)

type KeycloakHandler struct {
	ServiceHandlerObject
}

func (h *KeycloakHandler) UpdateConfig(values values.TemplateValues) {
	var initialAdminSecret corev1.Secret
	var realmConfigmap corev1.ConfigMap
	var deployment appsv1.Deployment
	var service corev1.Service

	yaml.LoadYaml(filepath("keycloak/keycloak-initial-cred-secret.yaml"), values, &initialAdminSecret)
	yaml.LoadYaml(filepath("keycloak/keycloak-realm-configmap.yaml"), values, &realmConfigmap)
	yaml.LoadYaml(filepath("keycloak/keycloak-deployment.yaml"), values, &deployment)
	yaml.LoadYaml(filepath("keycloak/keycloak-service.yaml"), values, &service)

	h.Config = []client.Object{
		&initialAdminSecret,
		&realmConfigmap,
		&deployment,
		&service,
	}
}
