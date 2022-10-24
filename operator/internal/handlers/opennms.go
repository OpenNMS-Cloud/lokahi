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
	rbacv1 "k8s.io/api/rbac/v1"
	"sigs.k8s.io/controller-runtime/pkg/client"
)

type OpenNMSHandler struct {
	ServiceHandlerObject
}

func (h *OpenNMSHandler) UpdateConfig(values values.TemplateValues) {
	//core
	var configMap corev1.ConfigMap
	var coreSA corev1.ServiceAccount
	var coreRB rbacv1.RoleBinding
	var coreService corev1.Service
	var coreIgniteService corev1.Service
	var coreDeployment appsv1.Deployment

	yaml.LoadYaml(filepath("opennms/core/core-configmap.yaml"), values, &configMap)
	yaml.LoadYaml(filepath("opennms/core/core-serviceaccount.yaml"), values, &coreSA)
	yaml.LoadYaml(filepath("opennms/core/core-rolebinding.yaml"), values, &coreRB)
	yaml.LoadYaml(filepath("opennms/core/core-service.yaml"), values, &coreService)
	yaml.LoadYaml(filepath("opennms/core/core-ignite-service.yaml"), values, &coreIgniteService)
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
	var minionSA corev1.ServiceAccount
	var minionRB rbacv1.RoleBinding
	var minionSVC corev1.Service
	var minionGatewaySVC corev1.Service
	var minionGatewayIgniteSVC corev1.Service
	var minionDeploy appsv1.Deployment
	var minionGatewayDeploy appsv1.Deployment

	yaml.LoadYaml(filepath("opennms/minion/minion-configmap.yaml"), values, &minionCM)
	yaml.LoadYaml(filepath("opennms/minion/minion-serviceaccount.yaml"), values, &minionSA)
	yaml.LoadYaml(filepath("opennms/minion/minion-rolebinding.yaml"), values, &minionRB)
	yaml.LoadYaml(filepath("opennms/minion/minion-service.yaml"), values, &minionSVC)
	yaml.LoadYaml(filepath("opennms/minion/minion-gateway-service.yaml"), values, &minionGatewaySVC)
	yaml.LoadYaml(filepath("opennms/minion/minion-gateway-ignite-service.yaml"), values, &minionGatewayIgniteSVC)
	yaml.LoadYaml(filepath("opennms/minion/minion-deployment.yaml"), values, &minionDeploy)
	yaml.LoadYaml(filepath("opennms/minion/minion-gateway-deployment.yaml"), values, &minionGatewayDeploy)

	//inventory
	var inventoryDeployment appsv1.Deployment
	var inventoryService corev1.Service

	yaml.LoadYaml(filepath("opennms/inventory/inventory-deployment.yaml"), values, &inventoryDeployment)
	yaml.LoadYaml(filepath("opennms/inventory/inventory-service.yaml"), values, &inventoryService)

	//notification
	var noteDeployment appsv1.Deployment
	var noteService corev1.Service

	yaml.LoadYaml(filepath("opennms/notification/notification-deployment.yaml"), values, &noteDeployment)
	yaml.LoadYaml(filepath("opennms/notification/notification-service.yaml"), values, &noteService)

	//metrics processor
	var mpDeployment appsv1.Deployment

	yaml.LoadYaml(filepath("opennms/metricsprocessor/metricsprocessor-deployment.yaml"), values, &mpDeployment)

	h.Config = []client.Object{
		&configMap,
		&coreSA,
		&coreRB,
		&coreService,
		&coreIgniteService,
		&coreDeployment,
		&apiService,
		&apiDeployment,
		&uiDeployment,
		&uiService,
		&minionCM,
		&minionSA,
		&minionRB,
		&minionSVC,
		&minionGatewaySVC,
		&minionGatewayIgniteSVC,
		&minionDeploy,
		&minionGatewayDeploy,
		&inventoryDeployment,
		&inventoryService,
		&noteDeployment,
		&noteService,
		&mpDeployment,
	}
}
