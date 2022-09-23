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

type OpenNMSHandler struct {
	ServiceHandlerObject
}

func (h *OpenNMSHandler) ProvideConfig(values values.TemplateValues) []client.Object {
	//core
	var certSecret corev1.Secret
	var configMap corev1.ConfigMap
	var coreService corev1.Service
	var coreDeployment appsv1.Deployment

	yaml.LoadYaml(filepath("opennms/core/cert-secret.yaml"), values, &certSecret)
	yaml.LoadYaml(filepath("opennms/core/core-configmap.yaml"), values, &configMap)
	yaml.LoadYaml(filepath("opennms/core/core-service.yaml"), values, &coreService)
	yaml.LoadYaml(filepath("opennms/core/core-deployment.yaml"), values, &coreDeployment)

	//api
	var apiService corev1.Service
	var apiDeployment appsv1.Deployment

	yaml.LoadYaml(filepath("opennms/api/api-service.yaml"), values, &apiService)
	yaml.LoadYaml(filepath("opennms/api/api-deployment.yaml"), values, &apiDeployment)

	//ui
	var uiDeployment appsv1.Deployment
	var uiService corev1.Service

	yaml.LoadYaml(filepath("opennms/ui/ui-deployment.yaml"), values, &uiDeployment)
	yaml.LoadYaml(filepath("opennms/ui/ui-service.yaml"), values, &uiService)

	//minion
	var minionCM corev1.ConfigMap
	var minionSVC corev1.Service
	var minionDeploy appsv1.Deployment

	yaml.LoadYaml(filepath("opennms/minion/minion-configmap.yaml"), values, &minionCM)
	yaml.LoadYaml(filepath("opennms/minion/minion-service.yaml"), values, &minionSVC)
	yaml.LoadYaml(filepath("opennms/minion/minion-deployment.yaml"), values, &minionDeploy)

	//notification
	var noteDeployment appsv1.Deployment
	var noteService corev1.Service

	yaml.LoadYaml(filepath("opennms/notification/notification-deployment.yaml"), values, &noteDeployment)
	yaml.LoadYaml(filepath("opennms/notification/notification-service.yaml"), values, &noteService)

	h.Config = []client.Object{
		&certSecret,
		&configMap,
		&coreService,
		&coreDeployment,
		&apiService,
		&apiDeployment,
		&uiDeployment,
		&uiService,
		&minionCM,
		&minionSVC,
		&minionDeploy,
		&noteDeployment,
		&noteService,
	}

	return h.Config
}
