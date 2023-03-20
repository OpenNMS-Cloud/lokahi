package thirdparty

import (
	"github.com/OpenNMS-Cloud/opennms-operator/internal/handlers"
	"github.com/OpenNMS-Cloud/opennms-operator/internal/model/values"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	rbacv1 "k8s.io/api/rbac/v1"
)

type PrometheusHandler struct {
	handlers.ServiceHandlerObject
}

func (h *PrometheusHandler) UpdateConfig(values values.TemplateValues) error {
	if values.Values.Prometheus.Enabled == false {
		return nil
	}
	var prometheusServiceAccount corev1.ServiceAccount
	var prometheusClusterRole rbacv1.ClusterRole
	var prometheusClusterRoleBinding rbacv1.ClusterRoleBinding
	var prometheusConfigMap corev1.ConfigMap
	var prometheusService corev1.Service
	var prometheusDeployment appsv1.Deployment

	h.AddToTemplates(handlers.Filepath("prometheus/prometheus-service-account.yaml"), values, &prometheusServiceAccount)
	h.AddToTemplates(handlers.Filepath("prometheus/prometheus-cluster-role.yaml"), values, &prometheusClusterRole)
	h.AddToTemplates(handlers.Filepath("prometheus/prometheus-cluster-role-binding.yaml"), values, &prometheusClusterRoleBinding)
	h.AddToTemplates(handlers.Filepath("prometheus/prometheus-configmap.yaml"), values, &prometheusConfigMap)
	h.AddToTemplates(handlers.Filepath("prometheus/prometheus-service.yaml"), values, &prometheusService)
	h.AddToTemplates(handlers.Filepath("prometheus/prometheus-deployment.yaml"), values, &prometheusDeployment)

	return h.LoadTemplates()
}
