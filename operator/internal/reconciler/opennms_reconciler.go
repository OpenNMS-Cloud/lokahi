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
	"log"
	"reflect"
	"time"

	"github.com/OpenNMS-Cloud/opennms-operator/api/v1alpha1"
	"github.com/OpenNMS-Cloud/opennms-operator/config"
	"github.com/OpenNMS-Cloud/opennms-operator/internal/handlers"
	"github.com/OpenNMS-Cloud/opennms-operator/internal/model/values"
	"github.com/OpenNMS-Cloud/opennms-operator/internal/util/crd"
	"github.com/go-logr/logr"
	"k8s.io/apimachinery/pkg/api/errors"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/runtime/serializer"
	"k8s.io/apimachinery/pkg/types"
	ctrl "sigs.k8s.io/controller-runtime"
	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
)

// OpenNMSReconciler - reconciles a OpenNMS object
type OpenNMSReconciler struct {
	client.Client
	Log              logr.Logger
	Scheme           *runtime.Scheme
	CodecFactory     serializer.CodecFactory
	Config           config.OperatorConfig
	DefaultValues    values.TemplateValues
	StandardHandlers []handlers.ServiceHandler
	Instances        map[string]*Instance
}

func (r *OpenNMSReconciler) Reconcile(ctx context.Context, req ctrl.Request) (reconcile.Result, error) {
	instance, err := r.getInstance(ctx, req)
	if err != nil { //any error here is fatal
		log.Fatal(err)
	} else if instance == nil { //instance doesn't exist or is set to deployOnly
		return reconcile.Result{RequeueAfter: 30 * time.Second}, nil
	}

	for _, handler := range instance.Handlers {
		if handler.GetDeployed() && instance.CRD.Spec.DeployOnly { //skip handler if already deployed and the instance is marked as "deploy only"
			continue
		}
		for _, resource := range handler.GetConfig() {
			kind := reflect.ValueOf(resource).Elem().Type().String()
			deployedResource, exists := r.getResourceFromCluster(ctx, resource)
			if !exists {
				r.updateStatus(ctx, &instance.CRD, false, "instance starting")
				r.Log.Info("creating resource", "namespace", resource.GetNamespace(), "name", resource.GetName(), "kind", kind)

				if err := ctrl.SetControllerReference(&instance.CRD, resource, r.Scheme); err != nil {
					r.Log.Error(err, "error setting resource controller reference", "controller", instance, "resource", resource, "kind", kind, "error", err)
					return reconcile.Result{}, err
				}
				if err := r.Create(ctx, resource); err != nil {
					r.Log.Error(err, "error creating resource", "namespace", resource.GetNamespace(), "name", resource.GetName(), "kind", kind, "error", err)
					return reconcile.Result{}, err
				}
				if kind == "v1.Deployment" || kind == "v1.Job" || kind == "v1.StatefulSet" {
					return reconcile.Result{RequeueAfter: 10 * time.Second}, nil
				}
			} else {
				r.Log.Info("checking resource for update", "namespace", resource.GetNamespace(), "name", resource.GetName(), "kind", kind)
				if resource.GetResourceVersion() != "" { //something is setting these on some of the resources
					resource.SetResourceVersion("")
					resource.SetUID("")
				}
				var res *reconcile.Result
				var err error
				switch kind {
				case "v1.Deployment":
					res, err = r.updateDeployment(ctx, &instance.CRD, resource, deployedResource)
				case "v1.StatefulSet":
					res, err = r.updateStatefulSet(ctx, &instance.CRD, resource, deployedResource)
				case "v1.Job":
					res = r.updateJob(deployedResource)
				case "v1.Secret":
					res, err = r.updateSecret(ctx, resource, deployedResource)
				case "v1.ConfigMap":
					res, err = r.updateConfigMap(ctx, &instance.CRD, resource, deployedResource)
				}
				if err != nil {
					r.Log.Info("error updating resource", "namespace", resource.GetNamespace(), "name", resource.GetName(), "kind", kind, "error", err)
					r.updateStatus(ctx, &instance.CRD, false, fmt.Sprintf("Error: failed to update resource: %s %s %s", resource.GetNamespace(), kind, resource.GetName()))
					return reconcile.Result{}, err
				}
				if res != nil {
					r.Log.Info("resource updated", "namespace", resource.GetNamespace(), "name", resource.GetName(), "kind", kind)
					return *res, nil
				}
			}
		}
		handler.SetDeployed(true)
	}

	// all clear, instance is ready
	r.updateStatus(ctx, &instance.CRD, true, "instance ready")
	instance.Deployed = true
	return reconcile.Result{RequeueAfter: 30 * time.Second}, nil
}

func (r *OpenNMSReconciler) getInstance(ctx context.Context, req ctrl.Request) (*Instance, error) {
	instanceCRD, err := crd.GetInstance(ctx, r.Client, req.NamespacedName)
	if err != nil && errors.IsNotFound(err) {
		if instance, ok := r.Instances[req.Name]; ok {
			r.Log.Info("known instance no longer not exists, forgetting it", "name", instance.Name)
			delete(r.Instances, instance.Name)
		}
		return nil, nil
	} else if err != nil {
		r.Log.Error(err, "failed to get crd for instance", "name", req.Name)
		return nil, err
	}

	instance, ok := r.Instances[instanceCRD.Name]
	if !ok {
		instance = &Instance{}
		err := instance.Init(ctx, r.Client, r.DefaultValues, r.StandardHandlers, instanceCRD)
		if err != nil {
			return nil, err
		}
		r.Instances[instanceCRD.Name] = instance
	} else if instance.Deployed && instance.CRD.Spec.DeployOnly {
		// instance is complete, noop
		return nil, nil
	} else {
		err := instance.Update(ctx, instanceCRD)
		if err != nil {
			return nil, err
		}
	}
	return instance, nil
}

func (r *OpenNMSReconciler) getResourceFromCluster(ctx context.Context, resource client.Object) (client.Object, bool) {
	deployedResource := resource.DeepCopyObject().(client.Object)
	err := r.Get(ctx, types.NamespacedName{Name: resource.GetName(), Namespace: resource.GetNamespace()}, deployedResource)
	return deployedResource, !(err != nil && errors.IsNotFound(err))
}

func (r *OpenNMSReconciler) updateStatus(ctx context.Context, instance *v1alpha1.OpenNMS, ready bool, reason string) {
	if instance.Status.Readiness.Ready != ready || instance.Status.Readiness.Reason != reason {
		instance.Status.Readiness.Ready = ready
		instance.Status.Readiness.Reason = reason
		instance.Status.Readiness.Timestamp = time.Now().Format(time.RFC3339)
		err := r.Status().Update(ctx, instance)
		if err != nil {
			r.Log.Error(err, "failed to update instance status", "instance", instance.Namespace)
		}
	}
}
