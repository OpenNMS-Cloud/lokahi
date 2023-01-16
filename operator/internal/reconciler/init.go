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
	opennmsv1alpha1 "github.com/OpenNMS/opennms-operator/api/v1alpha1"
	"github.com/OpenNMS/opennms-operator/internal/handlers"
	appsv1 "k8s.io/api/apps/v1"
	ctrl "sigs.k8s.io/controller-runtime"
)

func (r *OpenNMSReconciler) SetupWithManager(mgr ctrl.Manager) error {
	r.InitServiceHandlers()
	return ctrl.NewControllerManagedBy(mgr).
		For(&opennmsv1alpha1.OpenNMS{}).
		Owns(&appsv1.Deployment{}).
		Owns(&appsv1.StatefulSet{}).
		Complete(r)
}

func (r *OpenNMSReconciler) InitServiceHandlers() {
	r.StandardHandlers = []handlers.ServiceHandler{
		&handlers.BaseHandler{}, // MUST BE FIRST
		&handlers.PostgresHandler{},
		&handlers.KeycloakHandler{},
		&handlers.KafkaHandler{},
		&handlers.OpenNMSHandler{},
		&handlers.CortexHandler{},
		&handlers.GrafanaHandler{},
		&handlers.MailServerHandler{},
		&handlers.IngressHandler{}, // MUST BE LAST
	}
}
