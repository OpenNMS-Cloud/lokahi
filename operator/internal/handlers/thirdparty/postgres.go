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

package thirdparty

import (
	"github.com/OpenNMS-Cloud/opennms-operator/internal/handlers"
	"github.com/OpenNMS-Cloud/opennms-operator/internal/model/values"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
)

type PostgresHandler struct {
	handlers.ServiceHandlerObject
}

func (h *PostgresHandler) UpdateConfig(values values.TemplateValues) error {
	var initSecret corev1.Secret
	var credSecret corev1.Secret
	var service corev1.Service
	var pvc corev1.PersistentVolumeClaim
	var deployment appsv1.Deployment

	h.AddToTemplates(handlers.Filepath("postgres/postgres-init-secret.yaml"), values, &initSecret)
	h.AddToTemplates(handlers.Filepath("postgres/postgres-cred-secret.yaml"), values, &credSecret)
	h.AddToTemplates(handlers.Filepath("postgres/postgres-service.yaml"), values, &service)
	h.AddToTemplates(handlers.Filepath("postgres/postgres-pvc.yaml"), values, &pvc)
	h.AddToTemplates(handlers.Filepath("postgres/postgres-deployment.yaml"), values, &deployment)

	return h.LoadTemplates()
}
