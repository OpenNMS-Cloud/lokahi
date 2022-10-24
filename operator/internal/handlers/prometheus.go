package handlers

import (
	"github.com/OpenNMS/opennms-operator/internal/model/values"
	"github.com/OpenNMS/opennms-operator/internal/util/yaml"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	"sigs.k8s.io/controller-runtime/pkg/client"
)

type PrometheusHandler struct {
	ServiceHandlerObject
}

func (h *PrometheusHandler) UpdateConfig(values values.TemplateValues) {
	if values.Values.Prometheus.Enabled == false {
		return
	}
	var prometheusConfigMap corev1.ConfigMap
	var prometheusService corev1.Service
	var prometheusDeployment appsv1.Deployment
	var pushGatewayService corev1.Service
	var pushGateWayDeployment appsv1.Deployment

	yaml.LoadYaml(filepath("prometheus/prometheus-configmap.yaml"), values, &prometheusConfigMap)
	yaml.LoadYaml(filepath("prometheus/prometheus-service.yaml"), values, &prometheusService)
	yaml.LoadYaml(filepath("prometheus/prometheus-deployment.yaml"), values, &prometheusDeployment)
	yaml.LoadYaml(filepath("prometheus/pushgateway-service.yaml"), values, &pushGatewayService)
	yaml.LoadYaml(filepath("prometheus/pushgateway-deployment.yaml"), values, &pushGateWayDeployment)

	h.Config = []client.Object{
		&prometheusConfigMap,
		&prometheusService,
		&prometheusDeployment,
		&pushGatewayService,
		&pushGateWayDeployment,
	}
}
