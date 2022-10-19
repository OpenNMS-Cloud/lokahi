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
	adminv1 "k8s.io/api/admissionregistration/v1"
	appsv1 "k8s.io/api/apps/v1"
	batchv1 "k8s.io/api/batch/v1"
	corev1 "k8s.io/api/core/v1"
	netv1 "k8s.io/api/networking/v1"
	rbacv1 "k8s.io/api/rbac/v1"
	"sigs.k8s.io/controller-runtime/pkg/client"
)

type IngressHandler struct {
	ServiceHandlerObject
}

func (h *IngressHandler) UpdateConfig(values values.TemplateValues) {

	//INGRESS CONTROLLER CONFIGS
	var controllerServiceAccount corev1.ServiceAccount
	var controllerClusterRole rbacv1.ClusterRole
	var controllerClusterRoleBinding rbacv1.ClusterRoleBinding
	var controllerRole rbacv1.Role
	var controllerRoleBinding rbacv1.RoleBinding
	var controllerConfigMap corev1.ConfigMap
	var controllerService corev1.Service
	var controllerServiceAdmission corev1.Service
	var controllerIngressClass netv1.IngressClass
	var controllerDeployment appsv1.Deployment

	yaml.LoadYaml(filepath("ingress/nginx-controller/controller-serviceaccount.yaml"), values, &controllerServiceAccount)
	yaml.LoadYaml(filepath("ingress/nginx-controller/controller-clusterrole.yaml"), values, &controllerClusterRole)
	yaml.LoadYaml(filepath("ingress/nginx-controller/controller-clusterrolebinding.yaml"), values, &controllerClusterRoleBinding)
	yaml.LoadYaml(filepath("ingress/nginx-controller/controller-role.yaml"), values, &controllerRole)
	yaml.LoadYaml(filepath("ingress/nginx-controller/controller-rolebinding.yaml"), values, &controllerRoleBinding)
	yaml.LoadYaml(filepath("ingress/nginx-controller/controller-configmap.yaml"), values, &controllerConfigMap)
	yaml.LoadYaml(filepath("ingress/nginx-controller/controller-service.yaml"), values, &controllerService)
	yaml.LoadYaml(filepath("ingress/nginx-controller/controller-service-admission.yaml"), values, &controllerServiceAdmission)
	yaml.LoadYaml(filepath("ingress/nginx-controller/controller-ingress-class.yaml"), values, &controllerIngressClass)
	yaml.LoadYaml(filepath("ingress/nginx-controller/controller-deployment.yaml"), values, &controllerDeployment)

	//CUSTOM ERRORS CONFIGS
	//FIXME Is this even used?
	var customErrorsDeployment appsv1.Deployment
	var customErrorsService corev1.Service

	yaml.LoadYaml(filepath("ingress/custom-errors/nginx-errors-deployment.yaml"), values, &customErrorsDeployment)
	yaml.LoadYaml(filepath("ingress/custom-errors/nginx-errors-service.yaml"), values, &customErrorsService)

	//VALIDATING WEBHOOK CONFIGS
	var validatingWebhook adminv1.ValidatingWebhookConfiguration
	var webhookServiceAccount corev1.ServiceAccount
	var webhookRole rbacv1.Role
	var webhookRoleBinding rbacv1.RoleBinding
	var webhookClusterRole rbacv1.ClusterRole
	var webhookClusterRoleBinding rbacv1.ClusterRoleBinding

	yaml.LoadYaml(filepath("ingress/validating-webhook/validating-webhook.yaml"), values, &validatingWebhook)
	yaml.LoadYaml(filepath("ingress/validating-webhook/webhook-serviceaccount.yaml"), values, &webhookServiceAccount)
	yaml.LoadYaml(filepath("ingress/validating-webhook/webhook-role.yaml"), values, &webhookRole)
	yaml.LoadYaml(filepath("ingress/validating-webhook/webhook-rolebinding.yaml"), values, &webhookRoleBinding)
	yaml.LoadYaml(filepath("ingress/validating-webhook/webhook-clusterrole.yaml"), values, &webhookClusterRole)
	yaml.LoadYaml(filepath("ingress/validating-webhook/webhook-clusterrolebinding.yaml"), values, &webhookClusterRoleBinding)

	//JOBS CONFIGS
	var createSecret batchv1.Job
	var patchWebhook batchv1.Job

	yaml.LoadYaml(filepath("ingress/jobs/job-createsecret.yaml"), values, &createSecret)
	yaml.LoadYaml(filepath("ingress/jobs/job-patchwebhook.yaml"), values, &patchWebhook)

	//INGRESSES
	var opennmsIngress netv1.Ingress

	yaml.LoadYaml(filepath("ingress/ingresses/opennms-ingress.yaml"), values, &opennmsIngress)

	h.Config = []client.Object{
		&controllerServiceAccount,
		&controllerClusterRole,
		&controllerClusterRoleBinding,
		&controllerRole,
		&controllerRoleBinding,
		&controllerConfigMap,
		&controllerService,
		&controllerServiceAdmission,

		&customErrorsDeployment,
		&customErrorsService,

		&webhookServiceAccount,
		&webhookRole,
		&webhookRoleBinding,
		&webhookClusterRole,
		&webhookClusterRoleBinding,

		&createSecret,
		&controllerDeployment,
		&validatingWebhook,
		&patchWebhook,

		&controllerIngressClass,
		&opennmsIngress,
	}

}
