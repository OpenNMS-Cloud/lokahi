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
	"context"
	"fmt"
	"github.com/OpenNMS/opennms-operator/api/v1alpha1"
	"github.com/OpenNMS/opennms-operator/internal/util/subsets"
	v1 "k8s.io/api/apps/v1"
	batchv1 "k8s.io/api/batch/v1"
	corev1 "k8s.io/api/core/v1"
	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
	"time"
)

func (r *OpenNMSReconciler) updateDeployment(ctx context.Context, instance *v1alpha1.OpenNMS, resource client.Object, deployedResource client.Object) (*reconcile.Result, error) {
	rD := resource.(*v1.Deployment)
	drD := deployedResource.(*v1.Deployment)
	if !subsets.SubsetEqual(rD.Spec, drD.Spec) || !subsets.SubsetEqual(rD.Annotations, drD.Annotations) || !subsets.SubsetEqual(rD.Labels, drD.Labels) {
		r.updateStatus(ctx, instance, false, fmt.Sprintf("updating deployment: %s", resource.GetName()))
		r.Log.Info("updating deployment for some reason", "name", resource.GetName())

		if err := r.Update(ctx, resource); err != nil {
			return &reconcile.Result{}, err
		}
		return &reconcile.Result{RequeueAfter: 10 * time.Second}, nil
	} else {
		// Determine if the resources are fully created, otherwise wait longer
		deployment := deployedResource.(*v1.Deployment)
		if deployment.Status.ReadyReplicas != deployment.Status.Replicas {
			r.Log.Info("waiting for deployment for some reason", "name", resource.GetName(), "ready", deployment.Status.ReadyReplicas, "wanted", deployment.Status.Replicas)
			return &reconcile.Result{RequeueAfter: 10 * time.Second}, nil
		}
	}
	return nil, nil
}

func (r *OpenNMSReconciler) updateStatefulSet(ctx context.Context, instance *v1alpha1.OpenNMS, resource client.Object, deployedResource client.Object) (*reconcile.Result, error) {
	rSS := resource.(*v1.StatefulSet)
	drSS := deployedResource.(*v1.StatefulSet)
	if !subsets.SubsetEqual(rSS.Spec, drSS.Spec) || !subsets.SubsetEqual(rSS.Annotations, drSS.Annotations) || !subsets.SubsetEqual(rSS.Labels, drSS.Labels) {
		r.updateStatus(ctx, instance, false, fmt.Sprintf("updating statefulset: %s", resource.GetName()))
		if err := r.Update(ctx, resource); err != nil {
			return &reconcile.Result{}, err
		}
		return &reconcile.Result{RequeueAfter: 10 * time.Second}, nil
	} else {
		// Determine if the resources are fully created, otherwise wait longer
		statefulset := deployedResource.(*v1.StatefulSet)
		if statefulset.Status.ReadyReplicas != statefulset.Status.Replicas {
			return &reconcile.Result{RequeueAfter: 10 * time.Second}, nil
		}
	}

	return nil, nil
}

func (r *OpenNMSReconciler) updateJob(deployedResource client.Object) (*reconcile.Result, error) {
	job := deployedResource.(*batchv1.Job)
	if job.Status.Succeeded < 1 {
		return &reconcile.Result{RequeueAfter: 10 * time.Second}, nil
	}
	return nil, nil
}

func (r *OpenNMSReconciler) updateSecret(ctx context.Context, resource client.Object, deployedResource client.Object) (*reconcile.Result, error) {
	//TODO HS-495
	return nil, nil
}

func (r *OpenNMSReconciler) updateConfigMap(ctx context.Context, instance *v1alpha1.OpenNMS, resource client.Object, deployedResource client.Object) (*reconcile.Result, error) {
	rCM := resource.(*corev1.ConfigMap)
	drCM := deployedResource.(*corev1.ConfigMap)
	if !subsets.SubsetEqual(rCM.Data, drCM.Data) || !subsets.SubsetEqual(rCM.Annotations, drCM.Annotations) || !subsets.SubsetEqual(rCM.Labels, drCM.Labels) {
		r.updateStatus(ctx, instance, false, fmt.Sprintf("updating statefulset: %s", resource.GetName()))
		if err := r.Update(ctx, resource); err != nil {
			return &reconcile.Result{}, err
		}
		return &reconcile.Result{RequeueAfter: 10 * time.Second}, nil
	}
	return nil, nil
}
