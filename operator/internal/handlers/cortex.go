package handlers

import (
	"github.com/OpenNMS/opennms-operator/internal/model/values"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
)

type CortexHandler struct {
	ServiceHandlerObject
}

func (h *CortexHandler) UpdateConfig(values values.TemplateValues) error {
	if values.Values.Cortex.Enabled == false {
		return nil
	}
	var cortexConfigMap corev1.ConfigMap
	var cortexService corev1.Service
	var cortexDeployment appsv1.Deployment

	h.AddToTemplates(filepath("cortex/cortex-configmap.yaml"), values, &cortexConfigMap)
	h.AddToTemplates(filepath("cortex/cortex-service.yaml"), values, &cortexService)
	h.AddToTemplates(filepath("cortex/cortex-deployment.yaml"), values, &cortexDeployment)

	return h.LoadTemplates()
}
